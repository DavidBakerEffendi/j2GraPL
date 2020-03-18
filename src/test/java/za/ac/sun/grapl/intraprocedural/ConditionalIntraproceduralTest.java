package za.ac.sun.grapl.intraprocedural;

import org.junit.jupiter.api.*;
import za.ac.sun.grapl.CannonLoader;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class ConditionalIntraproceduralTest {

    private static final String PATH = "intraprocedural/conditional/";
    private static final String TEST_DIR = "/tmp/grapl/intraprocedural_test.xml";
    private CannonLoader fileCannon;
    private TinkerGraphHook hook;

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
    }

    @Test
    public void conditional1Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional2Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional3Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional4Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional5Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional6Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional7Test() {
        // TODO: Compare this to an accepted graph
    }

}
