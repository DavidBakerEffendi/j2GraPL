package za.ac.sun.grapl.intraprocedural;

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
    private CannonLoader fileCannon;
    private TinkerGraphHook hook;

    @BeforeEach
    public void setUpAll() throws IOException {
        ResourceCompilationUtil.compileJavaFiles(PATH);
        hook = new TinkerGraphHookBuilder("/tmp/intraprocedural_test.xml").createNewGraph(true).build();
        fileCannon = new CannonLoader(hook);
    }

    @Test
    public void basic1Test() {
        final URL resource = getClass().getClassLoader().getResource(PATH + "Basic1.class");
        String resourceDir = Objects.requireNonNull(resource).getFile();
        File f = new File(resourceDir);
        fileCannon.loadClassFile(f);
        fileCannon.fireOne();
        hook.exportCurrentGraph();
    }
}
