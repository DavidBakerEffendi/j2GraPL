package za.ac.sun.grapl.intraprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import za.ac.sun.grapl.Cannon;
import za.ac.sun.grapl.controllers.ASTController;
import za.ac.sun.grapl.domain.enums.EdgeLabels;
import za.ac.sun.grapl.domain.enums.Equality;
import za.ac.sun.grapl.domain.models.vertices.BlockVertex;
import za.ac.sun.grapl.domain.models.vertices.LocalVertex;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static za.ac.sun.grapl.util.TestQueryBuilderUtil.getVertexAlongEdge;

public class ConditionalIntraproceduralTest {

    final static Logger logger = LogManager.getLogger();

    private static final File PATH;
    private static final String TEST_DIR = "/tmp/grapl/j2grapl_test.xml";
    private GraphTraversalSource g;
    private Vertex methodRoot;

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource("intraprocedural/conditional")).getFile());
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        ResourceCompilationUtil.deleteClassFiles(PATH);
        File f = new File(TEST_DIR);
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Could not clear " + ConditionalIntraproceduralTest.class.getName() + "'s test resources.");
            }
        }
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws IOException {
        ASTController.getInstance().resetOrder();
        final TinkerGraphHook hook = new TinkerGraphHook.TinkerGraphHookBuilder(TEST_DIR).createNewGraph(true).build();
        final Cannon fileCannon = new Cannon(hook);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo
                .getDisplayName()
                .replaceAll("[^0-9]", "");
        String resourceDir = PATH.getAbsolutePath().concat("/Conditional").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.load(f);
        fileCannon.fire();
        hook.exportCurrentGraph();

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.conditional.Conditional"
                        .concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        methodRoot = methodTraversal.next();
    }

    @Test
    public void conditional1Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> ifElseTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseTraversal.hasNext());
        final Vertex elseBody = ifElseTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check condition branch
        final GraphTraversal<Vertex, Vertex> ifConditionTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name());
        assertTrue(ifConditionTraversal.hasNext());
        final Vertex ifCondition = ifConditionTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifCondition, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifCondition, LocalVertex.LABEL, "name", "2").hasNext());
    }

    @Test
    public void conditional2Test() {
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", "34");
        // This test is a modified version of Conditional 1, just test changes
        assertTrue(methodStoreRootTraversal.hasNext());
        Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        Vertex ifRoot = ifRootTraversal.next();
        // Check no else branch exists
        assertFalse(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").hasNext());
    }

    @Test
    public void conditional3Test() {
        // This test is a modified version of Conditional 1, just test changes
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check mul op under else body
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check the method operation still remains under method
        final GraphTraversal<Vertex, Vertex> methodStoreRootBody = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", "40");
        assertTrue(methodStoreRootBody.hasNext());
        final Vertex methodStoreRoot = methodStoreRootBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpBody = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpBody.hasNext());
        final Vertex storeOp = storeOpBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional4Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check if-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method-level operation
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE")
                        .has("order", "45");
        assertTrue(methodStoreRootTraversal.hasNext());
        final Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        final Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional5Test() {
        // This test is a modified version of Conditional 4
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check if-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check if-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method-level operation
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", "51");
        assertTrue(methodStoreRootTraversal.hasNext());
        final Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        final Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional6Test() {
        // This test is a modified version of Conditional 5
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check else-if branch
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex ifElseBody = elseElseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check else-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method-level operation
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", "51");
        assertTrue(methodStoreRootTraversal.hasNext());
        final Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        final Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional7Test() {
        // This test is a modified version of Conditional 6
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check else-if branch
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex ifElseBody = elseElseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check else-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
    }

    @Test
    public void conditional8Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check if-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        // Check if-else-if branch
        final GraphTraversal<Vertex, Vertex> ifElseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifElseIfRoot = ifElseIfRootTraversal.next();
        // Check if-else-if-body branch
        final GraphTraversal<Vertex, Vertex> ifElseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifElseIfBodyTraversal.hasNext());
        final Vertex ifElseIfBody = ifElseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "DIV").hasNext());
    }

    @Test
    public void conditional9Test() {
        // This is Conditional 8 with a symmetrical else body minus a SUB operation
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check if-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        // Check if-else-if branch
        final GraphTraversal<Vertex, Vertex> ifElseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifElseIfRoot = ifElseIfRootTraversal.next();
        // Check if-else-if-body branch
        final GraphTraversal<Vertex, Vertex> ifElseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifElseIfBodyTraversal.hasNext());
        final Vertex ifElseIfBody = ifElseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        // Check else-if branch
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex elseElseBody = elseElseBodyTraversal.next();
        // Check else-else-if branch
        final GraphTraversal<Vertex, Vertex> elseElseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseBody, BlockVertex.LABEL, "name", "IF");
        assertTrue(elseElseIfRootTraversal.hasNext());
        final Vertex elseElseIfRoot = elseElseIfRootTraversal.next();
        // Check if-else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseElseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseElseIfBodyTraversal.hasNext());
        final Vertex elseElseIfBody = elseElseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseElseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseElseIfBody, BlockVertex.LABEL, "name", "DIV").hasNext());
    }

    @Test
    public void conditional10Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check empty if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertFalse(ifBodyTraversal.hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
    }

}
