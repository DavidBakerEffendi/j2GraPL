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

public class ConditionalIntraproceduralTest {

    private static final String PATH = "intraprocedural/conditional/";
    private static final String TEST_DIR = "/tmp/grapl/intraprocedural_test.xml";
    private CannonLoader fileCannon;
    private TinkerGraphHook hook;

    @BeforeEach
    public void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
        hook = new TinkerGraphHook.TinkerGraphHookBuilder(TEST_DIR).createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
    }

    @AfterAll
    static void tearDownAll() {
        File f = new File(TEST_DIR);
        if (f.exists()) f.delete();
    }

    @Test
    public void conditional1Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Conditional1.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional2Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Conditional2.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional3Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Conditional3.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }

    @Test
    public void conditional4Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Conditional4.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
        // TODO: Compare this to an accepted graph
    }
}
