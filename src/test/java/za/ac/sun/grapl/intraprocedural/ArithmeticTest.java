package za.ac.sun.grapl.intraprocedural;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @AfterAll
    static void tearDownAll() {
        File f = new File(TEST_DIR);
//        if (f.exists()) f.delete();
    }

    @BeforeEach
    public void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
        hook = new TinkerGraphHook.TinkerGraphHookBuilder(TEST_DIR).createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
    }

    @Test
    public void arithmetic1Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Arithmetic1.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic2Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Arithmetic2.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic3Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Arithmetic3.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic4Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Arithmetic4.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void arithmetic5Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Arithmetic5.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }
}
