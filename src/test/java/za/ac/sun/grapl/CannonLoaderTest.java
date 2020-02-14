package za.ac.sun.grapl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CannonLoaderTest {

    private CannonLoader fileCannon;
    private File validFile;

    @BeforeEach
    public void setUpAll() {
        final URL resource = getClass().getClassLoader().getResource("intraprocedural/basic/Basic1.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        this.validFile = new File(resourceDir);
        fileCannon = new CannonLoader();
    }

    @Test
    public void validClassFireOneTest() {
        fileCannon.loadClassFile(this.validFile);
        fileCannon.fireOne();
    }

    @Test
    public void validClassFireAllTest() {
        fileCannon.loadClassFile(this.validFile);
        fileCannon.fireAll();
    }

    @Test
    public void loadNullClassFireOneTest() {
        assertThrows(NullPointerException.class, () -> fileCannon.loadClassFile(null));
    }

    @Test
    public void fireOneThatDoesNotExistTest() {
        fileCannon.loadClassFile(new File("dne.class"));
        assertDoesNotThrow(() -> fileCannon.fireOne());
    }

    @Test
    public void fireAllThatDoesNotExistTest() {
        fileCannon.loadClassFile(new File("dne1.class"));
        fileCannon.loadClassFile(new File("dne2.class"));
        assertDoesNotThrow(() -> fileCannon.fireAll());
    }

    @Test
    public void fireOutofBoundsTest() {
        fileCannon.loadClassFile(this.validFile);
        assertDoesNotThrow(() -> fileCannon.fireOne(2));
    }

    @Test
    public void emptyJarTest() {
        fileCannon.loadJarFile(null);
    }
}
