package za.ac.sun.grapl.intraprocedural;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.ac.sun.grapl.CannonLoader;
import za.ac.sun.grapl.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class ConditionalIntraproceduralTest {

    private static final String PATH = "intraprocedural/conditional/";
    private CannonLoader fileCannon;

    @BeforeEach
    public void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
        fileCannon = new CannonLoader();
    }

    @Test
    public void conditional1Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Conditional1.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
    }
}
