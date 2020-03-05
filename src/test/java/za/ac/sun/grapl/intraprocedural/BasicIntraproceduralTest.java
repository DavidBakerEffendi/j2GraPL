package za.ac.sun.grapl.intraprocedural;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.ac.sun.grapl.CannonLoader;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.hooks.TinkerGraphHook.TinkerGraphHookBuilder;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class BasicIntraproceduralTest {

    private static final String PATH = "intraprocedural/basic/";
    private static final String TEST_DIR = "/tmp/grapl/intraprocedural_test.xml";
    private CannonLoader fileCannon;
    private TinkerGraphHook hook;

    @BeforeEach
    public void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
        hook = new TinkerGraphHookBuilder("/tmp/grapl/intraprocedural_test.xml").createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
    }

    @AfterAll
    static void tearDownAll() {
        File f = new File(TEST_DIR);
        if (f.exists()) f.delete();
    }

    @Test
    public void basic1Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic1.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void basic2Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic2.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void basic3Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic3.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void basic4Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic4.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }
}
