package za.ac.sun.grapl.intraprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.*;
import za.ac.sun.grapl.CannonLoader;
import za.ac.sun.grapl.domain.enums.EdgeLabels;
import za.ac.sun.grapl.domain.models.vertices.BlockVertex;
import za.ac.sun.grapl.domain.models.vertices.LiteralVertex;
import za.ac.sun.grapl.domain.models.vertices.LocalVertex;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static za.ac.sun.grapl.util.TestQueryBuilderUtil.buildStoreTraversal;
import static za.ac.sun.grapl.util.TestQueryBuilderUtil.getVertexAlongEdge;

public class ArithmeticTest {

    final static Logger logger = LogManager.getLogger();

    private static final String PATH = "intraprocedural/arithmetic/";
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
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Could not clear " + ArithmeticTest.class.getName() + "'s test resources.");
            }
        }
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        hook = new TinkerGraphHook.TinkerGraphHookBuilder(TEST_DIR).createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo
                .getDisplayName()
                .replaceAll("[^0-9]", "");
        final URL resource = getClass().getClassLoader().getResource(PATH.concat("Arithmetic").concat(currentTestNumber).concat(".class"));
        String resourceDir = Objects.requireNonNull(resource).getFile();
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.arithmetic.Arithmetic"
                        .concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        methodRoot = methodTraversal.next();
    }

    @Test
    public void arithmetic1Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(6, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> subTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", "INTEGER");
        assertTrue(subTraversal.hasNext());
        final Vertex subVertex = subTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "6").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> divTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "DIV").has("typeFullName", "INTEGER");
        assertTrue(divTraversal.hasNext());
        final Vertex divVertex = divTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, divVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, divVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> mulTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "MUL").has("typeFullName", "INTEGER");
        assertTrue(mulTraversal.hasNext());
        final Vertex mulVertex = mulTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic2Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(4, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> mulTraversal = getVertexAlongEdge(g, EdgeLabels.AST, addVertex, BlockVertex.LABEL, "name", "MUL").has("typeFullName", "INTEGER");
        assertTrue(mulTraversal.hasNext());
        final Vertex mulVertex = mulTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic3Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(4, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> subTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", "INTEGER").has("typeFullName", "INTEGER");
        assertTrue(subTraversal.hasNext());
        final Vertex subVertex = subTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, subVertex, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> mulTraversal = getVertexAlongEdge(g, EdgeLabels.AST, addVertex, BlockVertex.LABEL, "name", "MUL").has("typeFullName", "INTEGER");
        assertTrue(mulTraversal.hasNext());
        final Vertex mulVertex = mulTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic4Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(4, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "-1").has("typeFullName", "INTEGER").hasNext());

        // TODO: This needs to be fixed for ISSUE #19
    }

    @Test
    public void arithmetic5Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(7, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "13682").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "27371").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "5").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> shlTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SHL").has("typeFullName", "INTEGER");
        assertTrue(shlTraversal.hasNext());
        final Vertex shlVertex = shlTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shlVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shlVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> andTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "AND").has("typeFullName", "INTEGER");
        assertTrue(andTraversal.hasNext());
        final Vertex andVertex = andTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, andVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, andVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "6").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> shrTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SHR").has("typeFullName", "INTEGER");
        assertTrue(shrTraversal.hasNext());
        final Vertex shrVertex = shrTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shrVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shrVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> orTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "OR").has("typeFullName", "INTEGER");
        assertTrue(orTraversal.hasNext());
        final Vertex orVertex = orTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, orVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, orVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "7").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> remTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "REM").has("typeFullName", "INTEGER");
        assertTrue(remTraversal.hasNext());
        final Vertex remVertex = remTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, remVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, remVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic6Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(4, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "0").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> ushrTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "USHR").has("typeFullName", "INTEGER");
        assertTrue(ushrTraversal.hasNext());
        final Vertex ushrVertex = ushrTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ushrVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ushrVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> xorTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "XOR").has("typeFullName", "INTEGER");
        assertTrue(xorTraversal.hasNext());
        final Vertex xorVertex = xorTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, xorVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, xorVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

}
