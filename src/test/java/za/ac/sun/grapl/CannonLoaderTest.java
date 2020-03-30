package za.ac.sun.grapl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.ac.sun.grapl.hooks.TinkerGraphHook;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CannonLoaderTest {

    private static Cannon fileCannon;
    private static File validSourceFile;
    private static File validClassFile;
    private static File validDirectory;
    private static TinkerGraphHook hook;

    @BeforeAll
    static void setUpAll() {
        final URL validSourceURL = CannonLoaderTest.class.getClassLoader().getResource("cannon_tests/Test1.java");
        final URL validClassURL = CannonLoaderTest.class.getClassLoader().getResource("cannon_tests/Test2.class");
        final URL validDirectoryURL = CannonLoaderTest.class.getClassLoader().getResource("cannon_tests/dir_test");
        String validSourceFileDir = Objects.requireNonNull(validSourceURL).getFile();
        String validClassFileDir = Objects.requireNonNull(validClassURL).getFile();
        String validDirectoryFileDir = Objects.requireNonNull(validDirectoryURL).getFile();
        validSourceFile = new File(validSourceFileDir);
        validClassFile = new File(validClassFileDir);
        validDirectory = new File(validDirectoryFileDir);
        hook = new TinkerGraphHook.TinkerGraphHookBuilder("/tmp/grapl/intraprocedural_test.xml").createNewGraph(true).build();
        fileCannon = new Cannon(hook);
    }

    @Test
    public void validSourceFileTest() throws IOException {
        fileCannon.load(validSourceFile);
        fileCannon.fire();
        hook.exportCurrentGraph();
    }

    @Test
    public void validClassFileTest() throws IOException {
        fileCannon.load(validClassFile);
        fileCannon.fire();
        hook.exportCurrentGraph();
    }

    @Test
    public void validDirectoryTest() throws IOException {
        fileCannon.load(validDirectory);
        fileCannon.fire();
        hook.exportCurrentGraph();
    }

    @Test
    public void loadNullFileTest() {
        assertThrows(NullPointerException.class, () -> fileCannon.load(null));
    }

    @Test
    public void loadFileThatDoesNotExistTest() {
        assertThrows(NullPointerException.class, () -> fileCannon.load(new File("dne.class")));
    }

}
