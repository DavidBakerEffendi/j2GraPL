package za.ac.sun.grapl.intraprocedural;

import org.junit.jupiter.api.*;
import za.ac.sun.grapl.CannonLoader;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class ArithmeticTest {

    private static final String PATH = "intraprocedural/arithmetic/";
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
        if (f.exists()) f.delete();
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
    }

    @Test
    public void arithmetic1Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic2Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic3Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic4Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic5Test() {
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic6Test() {
        // TODO: Compare this to an accepted graph
    }

}
