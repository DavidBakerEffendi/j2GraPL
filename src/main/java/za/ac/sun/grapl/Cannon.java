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
package za.ac.sun.grapl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import za.ac.sun.grapl.controllers.ASTController;
import za.ac.sun.grapl.hooks.IHook;
import za.ac.sun.grapl.util.ResourceCompilationUtil;
import za.ac.sun.grapl.visitors.ast.ASTClassVisitor;
import za.ac.sun.grapl.visitors.debug.DebugClassVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.jar.JarFile;

public final class Cannon {

    final static Logger logger = LogManager.getLogger();

    private final LinkedList<File> loadedFiles;

    public Cannon(final IHook hook) {
        this.loadedFiles = new LinkedList<>();
        // Set controller hooks
        ASTController.Companion.getInstance().hook(hook);
    }

    /**
     * Loads a single Java class file or directory of class files into the cannon.
     *
     * @param file the Java source/class file, directory of source/class files, or a JAR file.
     * @throws NullPointerException if the file is null
     * @throws IOException          In the case of a directory given, this would throw if .java files fail to compile
     */
    public void load(final File file) throws NullPointerException, IOException {
        if (file == null) throw new NullPointerException("File may not be null!");
        if (file.isDirectory()) {
            // Any .java files will automatically be compiled
            ResourceCompilationUtil.compileJavaFiles(file);
            loadedFiles.addAll(ResourceCompilationUtil.fetchClassFiles(file));
        } else if (file.isFile()) {
            if (file.getName().endsWith(".java")) {
                ResourceCompilationUtil.compileJavaFile(file);
                this.loadedFiles.add(new File(file.getAbsolutePath().replace(".java", ".class")));
            } else if (file.getName().endsWith(".jar")) {
                final JarFile jar = new JarFile(file);
                loadedFiles.addAll(ResourceCompilationUtil.fetchClassFiles(jar));
            } else if (file.getName().endsWith(".class")) {
                this.loadedFiles.add(file);
            }
        } else if (!file.exists()) {
            throw new NullPointerException("File '" + file.getName() + "' does not exist!");
        }
    }

    /**
     * Fires all loaded Java classes currently loaded.
     */
    public void fire() {
        this.loadedFiles.forEach(this::fire);
    }

    /**
     * Attempts to fire a file from the cannon.
     *
     * @param f the file to fire.
     */
    private void fire(final File f) {
        try (InputStream fis = new FileInputStream(f)) {
            final ClassReader cr = new ClassReader(fis);
            // The class visitors are declared here and wrapped by one another in a pipeline
            final DebugClassVisitor rootVisitor = new DebugClassVisitor();
            final ASTClassVisitor astVisitor = new ASTClassVisitor(rootVisitor);
            // ^ append new visitors here
            cr.accept(astVisitor, 0);
        } catch (IOException e) {
            logger.error("IOException encountered while visiting '" + f.getName() + "'.", e);
        }
    }
}
