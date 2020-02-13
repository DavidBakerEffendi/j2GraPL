package za.ac.sun.grapl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Objects;

public class CannonLoaderTest {

    private CannonLoader fileCannon;

    @BeforeEach
    public void setUpAll() {
        fileCannon = new CannonLoader();
    }

    @Test
    public void helloWorldTest() {
        final URL resource = getClass().getClassLoader().getResource("hello_world/HelloWorld.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
    }

    @Test
    public void emptyJarTest() {
        fileCannon.loadJarFile(null);
    }
}
