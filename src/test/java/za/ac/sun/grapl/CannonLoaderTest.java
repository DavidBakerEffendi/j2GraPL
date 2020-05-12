package za.ac.sun.grapl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.ac.sun.grapl.domain.enums.EdgeLabels;
import za.ac.sun.grapl.domain.enums.VertexLabels;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.intraprocedural.BasicIntraproceduralTest;
import za.ac.sun.grapl.intraprocedural.ConditionalIntraproceduralTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static za.ac.sun.grapl.util.TestQueryBuilderUtil.buildStoreTraversal;
import static za.ac.sun.grapl.util.TestQueryBuilderUtil.getVertexAlongEdge;

public class CannonLoaderTest {

    final static Logger logger = LogManager.getLogger();

    private static final String TEST_DIR = "/tmp/grapl/j2grapl_test.xml";
    private static Cannon fileCannon;
    private static File validSourceFile;
    private static File validClassFile;
    private static File validDirectory;
    private static File validJarFile;
    private static TinkerGraphHook hook;

    private static File getTestResource(String dir) {
        final URL resourceURL = CannonLoaderTest.class.getClassLoader().getResource(dir);
        String fullURL = Objects.requireNonNull(resourceURL).getFile();
        return new File(fullURL);
    }

    @BeforeAll
    static void setUpAll() {
        validSourceFile = getTestResource("cannon_tests/Test1.java");
        validClassFile = getTestResource("cannon_tests/Test2.class");
        validJarFile = getTestResource("cannon_tests/Test3.jar");
        validDirectory = getTestResource("cannon_tests/dir_test");
        hook = new TinkerGraphHook.TinkerGraphHookBuilder().build();
        fileCannon = new Cannon(hook);
    }

    @AfterAll
    static void tearDown() {
        File f = new File(TEST_DIR);
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Could not clear " + ConditionalIntraproceduralTest.class.getName() + "'s test resources.");
            }
        }
    }

    @Test
    public void validSourceFileTest() throws IOException {
        fileCannon.load(validSourceFile);
        fileCannon.fire();
        hook.exportCurrentGraph(TEST_DIR);
    }

    @Test
    public void validClassFileTest() throws IOException {
        fileCannon.load(validClassFile);
        fileCannon.fire();
        hook.exportCurrentGraph(TEST_DIR);
    }

    @Test
    public void validDirectoryTest() throws IOException {
        fileCannon.load(validDirectory);
        fileCannon.fire();
        hook.exportCurrentGraph(TEST_DIR);
    }

    @Test
    public void validJarTest() throws IOException {
        GraphTraversalSource g = TinkerGraph.open().traversal();
        fileCannon.load(validJarFile);
        fileCannon.fire();
        hook.exportCurrentGraph(TEST_DIR);
        g.io(TEST_DIR).read().iterate();

        // This is za.ac.sun.grapl.intraprocedural.Basic6's test in a JAR
        final GraphTraversal<Vertex, Vertex> intraNamespaceTraversal = g.V().has(VertexLabels.NAMESPACE_BLOCK.toString(), "fullName", "intraprocedural");
        assertTrue(intraNamespaceTraversal.hasNext());
        final Vertex intraNamespaceVertex = intraNamespaceTraversal.next();
        final GraphTraversal<Vertex, Vertex> basicNamespaceTraversal = getVertexAlongEdge(g, EdgeLabels.AST, intraNamespaceVertex, VertexLabels.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic");
        assertTrue(basicNamespaceTraversal.hasNext());
        final Vertex basicNamespaceVertex = basicNamespaceTraversal.next();
        final GraphTraversal<Vertex, Vertex> basic6NamespaceTraversal = getVertexAlongEdge(g, EdgeLabels.AST, basicNamespaceVertex, VertexLabels.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic.basic6");
        assertTrue(basic6NamespaceTraversal.hasNext());
        final Vertex basic6NamespaceVertex = basic6NamespaceTraversal.next();

        final GraphTraversal<Vertex, Vertex> basicMethodTraversal = getVertexAlongEdge(g, EdgeLabels.AST, basicNamespaceVertex, VertexLabels.METHOD, "name", "main");
        assertTrue(basicMethodTraversal.hasNext());
        final GraphTraversal<Vertex, Vertex> basic6MethodTraversal = getVertexAlongEdge(g, EdgeLabels.AST, basic6NamespaceVertex, VertexLabels.METHOD, "name", "main");
        assertTrue(basic6MethodTraversal.hasNext());

        assertEquals(6, buildStoreTraversal(g, EdgeLabels.AST, intraNamespaceVertex).count().next());

        BasicIntraproceduralTest.testBasic1Structure(g, basicNamespaceVertex);
        BasicIntraproceduralTest.testBasic1Structure(g, basic6NamespaceVertex);
    }

    @Test
    public void loadNullFileTest() {
        assertThrows(IllegalArgumentException.class, () -> fileCannon.load(null));
    }

    @Test
    public void loadFileThatDoesNotExistTest() {
        assertThrows(NullPointerException.class, () -> fileCannon.load(new File("dne.class")));
    }

}
