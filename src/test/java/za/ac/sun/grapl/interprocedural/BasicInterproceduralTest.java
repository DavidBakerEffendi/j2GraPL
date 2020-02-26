package za.ac.sun.grapl.interprocedural;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.ac.sun.grapl.CannonLoader;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class BasicInterproceduralTest {

    private static final String PATH = "interprocedural/basic/";
    private CannonLoader fileCannon;
    private TinkerGraphHook hook;

    @BeforeEach
    public synchronized void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
        hook = new TinkerGraphHook.TinkerGraphHookBuilder("/tmp/grapl/intraprocedural_test.kryo").createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
    }

    @Test
    public void basicCall1Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic1.class");
        final String resourceDir = Objects.requireNonNull(resource).getFile();
        final File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
    }

    @Test
    public void basicCall2Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic2.class");
        final String resourceDir = Objects.requireNonNull(resource).getFile();
        final File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
    }

    @Test
    public void basicCall3Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic3.class");
        final String resourceDir = Objects.requireNonNull(resource).getFile();
        final File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
    }

    @Test
    public void basicCall4Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic4.class");
        final String resourceDir = Objects.requireNonNull(resource).getFile();
        final File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
    }

    @Test
    public void basicCall5Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic5.class");
        final String resourceDir = Objects.requireNonNull(resource).getFile();
        final File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
    }

}
