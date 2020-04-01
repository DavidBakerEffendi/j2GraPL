package za.ac.sun.grapl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import za.ac.sun.grapl.intraprocedural.ConditionalIntraproceduralTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CannonLoaderTest {

    final static Logger logger = LogManager.getLogger();

    private static final String TEST_DIR = "/tmp/grapl/j2grapl_test.xml";
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
        hook = new TinkerGraphHook.TinkerGraphHookBuilder(TEST_DIR).createNewGraph(true).build();
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
