package za.ac.sun.grapl.intraprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import za.ac.sun.grapl.Cannon;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoopIntraproceduralTest {

    final static Logger logger = LogManager.getLogger();

    private static final File PATH;
    private static final String TEST_DIR = "/tmp/grapl/j2grapl_test.xml";
    private GraphTraversalSource g;
    private Vertex methodRoot;

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource("intraprocedural/loop")).getFile());
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        ResourceCompilationUtil.deleteClassFiles(PATH);
        File f = new File(TEST_DIR);
        if (f.exists()) {
//            if (!f.delete()) {
//                logger.warn("Could not clear " + ConditionalIntraproceduralTest.class.getName() + "'s test resources.");
//            }
        }
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws IOException {
        final TinkerGraphHook hook = new TinkerGraphHook.TinkerGraphHookBuilder().build();
        final Cannon fileCannon = new Cannon(hook);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo
                .getDisplayName()
                .replaceAll("[^0-9]", "");
        String resourceDir = PATH.getAbsolutePath().concat("/Loop").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.load(f);
        fileCannon.fire();
        hook.exportCurrentGraph(TEST_DIR);

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.loop.Loop"
                        .concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        methodRoot = methodTraversal.next();
    }

    @Test
    public void loop1Test() {

    }

    @Test
    public void loop2Test() {

    }

}
