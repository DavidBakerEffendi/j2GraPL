/*
 * Copyright 2020 David Baker Effendi
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package za.ac.sun.grapl.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ResourceCompilationUtil {

    final static Logger logger = LogManager.getLogger();

    /**
     * Validates the given file as a directory that exists.
     *
     * @param f the file to validate
     * @throws IOException if the file is not a valid directory or does not exist.
     */
    private static void validateFileAsDirectory(final File f) throws IOException {
        // Validate path
        if (!f.isDirectory()) throw new IOException("The path must point to a valid directory!");
        if (!f.exists()) throw new IOException("The path does not exist!");
    }

    /**
     * Given a path to a Java source file, programmatically compiles the source (.java) file.
     *
     * @param file the source file to compile
     */
    public static void compileJavaFile(final File file) {
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
    public static void compileJavaFiles(final File path) throws IOException {
        validateFileAsDirectory(path);
        // Dynamically compile Java test resources
        final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);
        final LinkedList<File> fileList = new LinkedList<>();

        try (final Stream<Path> walk = Files.walk(Paths.get(path.getAbsolutePath()))) {
            walk.map(Path::toString)
                    .filter(f -> f.endsWith(".java"))
                    .collect(Collectors.toList())
                    .forEach((f) -> fileList.add(new File(f)));
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
    public static void deleteClassFiles(final File path) throws IOException {
        validateFileAsDirectory(path);

        try (final Stream<Path> walk = Files.walk(Paths.get(path.getAbsolutePath()))) {
            walk.map(Path::toString)
                    .filter(f -> f.endsWith(".class"))
                    .collect(Collectors.toList())
                    .forEach((f) -> {
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
    public static List<File> fetchClassFiles(final File path) throws IOException {
        validateFileAsDirectory(path);
        try (final Stream<Path> walk = Files.walk(Paths.get(path.getAbsolutePath()))) {
            return walk.map(Path::toString)
                    .filter(f -> f.endsWith(".class"))
                    .map(File::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Returns a list of all the class files inside of a JAR file.
     *
     * @param jar the JarFile
     * @return a list of all <code>.class</code> files under the given JAR file.
     */
    public static List<File> fetchClassFiles(final JarFile jar) {
        return jar.stream()
                .map(JarEntry::toString)
                .filter(f -> f.endsWith(".class"))
                .map(JarEntry::new)
                .map(je -> extractJarEntry(jar, je))
                .filter(f -> !Objects.isNull(f))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the {@link JarFile} from a given {@link JarEntry} and writes it to a temporary file, which is returned
     * as a {@link File}.
     *
     * @param jarFile the JAR to extract from
     * @param entry   the entry to extract
     * @return the temporary file if the extraction process was successful, <code>null</code> if otherwise.
     */
    public static File extractJarEntry(final JarFile jarFile, final JarEntry entry) {
        File tmp = null;
        try {
            tmp = File.createTempFile(entry.toString().substring(entry.toString().lastIndexOf('/') + 1), null);
            try (InputStream in = jarFile.getInputStream(entry);
                 BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))
            ) {
                final byte[] buffer = new byte[2048];
                int nBytes = in.read(buffer);
                while (nBytes > 0) {
                    out.write(buffer, 0, nBytes);
                    nBytes = in.read(buffer);
                }
            }
        } catch (IOException e) {
            logger.warn("Error while extracting '" + entry.getName() + "' from JAR.", e);
        }
        return tmp;
    }

}
