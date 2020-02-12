package za.ac.sun.grapl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

public class CannonLoaderTest {

    private CannonLoader fileCannon;

    @BeforeEach
    public void setUpAll() {
        fileCannon = new CannonLoader();
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void helloWorldTest() {
        File f = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("hello_world/HelloWorld.class")).getFile());
        fileCannon.loadClassFile(f);
    }
}
