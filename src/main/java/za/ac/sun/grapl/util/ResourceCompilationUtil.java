package za.ac.sun.grapl.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceCompilationUtil {

    final static Logger logger = LogManager.getLogger();

    /**
     * Validates the given file as a directory that exists.
     *
     * @param f the file to validate
     * @throws IOException if the file is not a valid directory or does not exist.
     */
    private static void validateFileAsDirectory(File f) throws IOException {
        // Validate path
        if (!f.isDirectory()) throw new IOException("The path must point to a valid directory!");
        if (!f.exists()) throw new IOException("The path does not exist!");
    }

    /**
     * Given a path to a Java source file, programmatically compiles the source (.java) file.
     *
     * @param file the source file to compile
     */
    public static void compileJavaFile(File file) {
        final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);
        javac.getTask(null, fileManager, null, Collections.singletonList("-g"), null,
                fileManager.getJavaFileObjects(file)).call();
    }

    /**
     * Given a path to a directory, programmatically compile any .java files found in the directory.
     *
     * @param path the path to the directory
     * @throws IOException if the path is not a directory or does not exist
     */
    public static void compileJavaFiles(File path) throws IOException {
        validateFileAsDirectory(path);
        // Dynamically compile Java test resources
        final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);
        final LinkedList<File> fileList = new LinkedList<>();

        try (Stream<Path> walk = Files.walk(Paths.get(path.getAbsolutePath()))) {
            List<String> result = walk
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".java"))
                    .collect(Collectors.toList());
            result.forEach((f) -> fileList.add(new File(f)));
        }

        javac.getTask(null, fileManager, null, Collections.singletonList("-g"), null,
                fileManager.getJavaFileObjectsFromFiles(fileList)).call();
    }

    /**
     * Given a path to a directory, programmatically delete any .class files found in the directory.
     *
     * @param path the path to the directory
     * @throws IOException if the path is not a directory or does not exist
     */
    public static void deleteClassFiles(File path) throws IOException {
        validateFileAsDirectory(path);

        try (Stream<Path> walk = Files.walk(Paths.get(path.getAbsolutePath()))) {
            List<String> result = walk
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".class"))
                    .collect(Collectors.toList());
            result.forEach((f) -> {
                if (!new File(f).delete()) logger.error("Unable to delete: " + f);
            });
        }
    }

    /**
     * Returns a list of all the class files under a given directory recursively.
     *
     * @param path the path to the directory
     * @return a list of all .class files under the given directory
     * @throws IOException if the path is not a directory or does not exist
     */
    public static List<String> fetchClassFiles(File path) throws IOException {
        validateFileAsDirectory(path);
        try (Stream<Path> walk = Files.walk(Paths.get(path.getAbsolutePath()))) {
            return walk.map(Path::toString)
                    .filter(f -> f.endsWith(".class"))
                    .collect(Collectors.toList());
        }
    }

}
