package za.ac.sun.grapl.util;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceCompilationUtil {

    /**
     * Given a path to a directory, programmatically compile any .java files found in the directory.
     *
     * @param path the path to the directory
     * @throws IOException if the path is not a directory or does not exist
     */
    public static void compileJavaFiles(String path) throws IOException {
        // Validate path
        File dir = new File(Objects.requireNonNull(
                ResourceCompilationUtil.class.getClassLoader().getResource(path)).getFile());
        if (!dir.isDirectory()) throw new IOException("The path must point to a directory!");
        if (!dir.exists()) throw new IOException("The path does not exist!");
        // Dynamically compile Java test resources
        final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);
        final LinkedList<File> fileList = new LinkedList<>();

        try (Stream<Path> walk = Files.walk(Paths.get(dir.getAbsolutePath()))) {
            List<String> result = walk
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".java"))
                    .collect(Collectors.toList());
            result.forEach((f) -> fileList.add(new File(f)));
        }

        javac.getTask(null, fileManager, null, Arrays.asList("-g"), null,
                fileManager.getJavaFileObjectsFromFiles(fileList)).call();
    }

}
