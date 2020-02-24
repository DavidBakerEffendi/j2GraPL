package za.ac.sun.grapl.intraprocedural;

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
    private CannonLoader fileCannon;
    private TinkerGraphHook hook;

    @BeforeEach
    public void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
        hook = new TinkerGraphHook.TinkerGraphHookBuilder("/tmp/intraprocedural_test.kryo").createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
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
