package za.ac.sun.grapl.intraprocedural;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.*;
import za.ac.sun.grapl.CannonLoader;
import za.ac.sun.grapl.domain.enums.Equality;
import za.ac.sun.grapl.domain.models.vertices.BlockVertex;
import za.ac.sun.grapl.domain.models.vertices.LocalVertex;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionalIntraproceduralTest {

    private static final String PATH = "intraprocedural/conditional/";
    private static final String TEST_DIR = "/tmp/grapl/intraprocedural_test.xml";
    private CannonLoader fileCannon;
    private TinkerGraphHook hook;
    private GraphTraversalSource g;
    private Vertex methodRoot;

    @BeforeAll
    static void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        ResourceCompilationUtil.deleteClassFiles(PATH);
        File f = new File(TEST_DIR);
//        if (f.exists()) f.delete();
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        hook = new TinkerGraphHook.TinkerGraphHookBuilder(TEST_DIR).createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo
                .getDisplayName()
                .replaceAll("[^0-9]", "");
        final URL resource = getClass().getClassLoader().getResource(PATH.concat("Conditional").concat(currentTestNumber).concat(".class"));
        String resourceDir = Objects.requireNonNull(resource).getFile();
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        methodRoot = g.V().has("METHOD", "fullName", "intraprocedural.conditional.Conditional"
                .concat(currentTestNumber).concat(".main")).next();
        assertNotNull(methodRoot);
    }

    @Test
    public void conditional1Test() {
        // Get conditional root
        Vertex ifRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").next();
        assertNotNull(ifRoot);
        // Check if branch
        Vertex ifBody = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").next();
        assertNotNull(ifBody);
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "SUB").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "SUB").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        // Check else branch
        Vertex elseBody = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ELSE_BODY").next();
        assertNotNull(elseBody);
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        // Check condition branch
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", Equality.GT.name()).hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
    }

    @Test
    public void conditional2Test() {
        // This test is a modified version of Conditional 1, just test changes
        assertTrue(g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        Vertex methodStoreRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").next();
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").hasNext());
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        Vertex storeOp = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").next();
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        // Get conditional root
        Vertex ifRoot = g.V(methodRoot)
                .repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").next();
        assertNotNull(ifRoot);
        // Check no else branch exists
        assertFalse(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ELSE_BODY").hasNext());

    }

    @Test
    public void conditional3Test() {
        // This test is a modified version of Conditional 1, just test changes
        // Get conditional root
        Vertex ifRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").next();
        assertNotNull(ifRoot);
        // Check mul op under else body
        Vertex elseBody = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ELSE_BODY").next();
        assertNotNull(elseBody);
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "MUL").hasNext());
        // Check the method operation still remains under method
        Vertex methodStoreRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").next();
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").hasNext());
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        Vertex storeOp = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").next();
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
    }

    @Test
    public void conditional4Test() {
        // Get conditional root
        Vertex ifRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").next();
        assertNotNull(ifRoot);
        // Check if branch
        Vertex ifBody = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").next();
        assertNotNull(ifBody);
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "SUB").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        // Check nested if branch
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").hasNext());
        Vertex nestedIf = g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").next();
        // Check nested if body branch
        assertTrue(g.V(nestedIf).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").hasNext());
        Vertex nestedIfBody = g.V(nestedIf).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").next();
        assertTrue(g.V(nestedIfBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(nestedIfBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "SUB").hasNext());
        // Check nested condition branch
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", Equality.GT.name()).hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        // Check else branch
        Vertex elseBody = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ELSE_BODY").next();
        assertNotNull(elseBody);
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "MUL").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        // Check condition branch
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", Equality.EQ.name()).hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        // Check method-level operation
        Vertex methodStoreRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").next();
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").hasNext());
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        Vertex storeOp = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").next();
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
    }

    @Test
    public void conditional5Test() {
        // This test is a modified version of Conditional 5
        // Get conditional root
        Vertex ifRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").next();
        assertNotNull(ifRoot);
        // Check if branch
        Vertex ifBody = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").next();
        assertNotNull(ifBody);
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "SUB").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        // Check nested if branch
        assertTrue(g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").hasNext());
        Vertex nestedIf = g.V(ifBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF").next();
        // Check nested if body branch
        assertTrue(g.V(nestedIf).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").hasNext());
        Vertex nestedIfBody = g.V(nestedIf).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").next();
        assertTrue(g.V(nestedIfBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(nestedIfBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "SUB").hasNext());
        // Check nested if body branch
        assertTrue(g.V(nestedIf).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ELSE_BODY").hasNext());
        Vertex nestedElseBody = g.V(nestedIf).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ELSE_BODY").next();
        assertTrue(g.V(nestedElseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(nestedElseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "DIV").hasNext());
        // Check nested condition branch
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", Equality.GT.name()).hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        // Check else branch
        Vertex elseBody = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ELSE_BODY").next();
        assertNotNull(elseBody);
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "MUL").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(elseBody).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        // Check condition branch
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", Equality.EQ.name()).hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
        assertTrue(g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        // Check method-level operation
        Vertex methodStoreRoot = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "STORE").next();
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").hasNext());
        assertTrue(g.V(methodStoreRoot).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        Vertex storeOp = g.V(methodRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "ADD").next();
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "2").hasNext());
        assertTrue(g.V(storeOp).repeat(__.out("AST")).emit()
                .has(LocalVertex.LABEL.toString(), "name", "1").hasNext());
    }

    @Test
    public void conditional6Test() {
        // TODO: Compare this to an accepted graph
    }

}
