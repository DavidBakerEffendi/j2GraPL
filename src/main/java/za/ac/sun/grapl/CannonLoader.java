/**
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
import za.ac.sun.grapl.hooks.IHook;
import za.ac.sun.grapl.visitors.ast.ASTClassVisitor;
import za.ac.sun.grapl.visitors.debug.DebugClassVisitor;

import java.io.*;
import java.util.LinkedList;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class CannonLoader {

    final static Logger logger = LogManager.getLogger();

    private final LinkedList<File> loadedFiles;
    private final IHook hook;

    public CannonLoader(IHook hook) {
        this.loadedFiles = new LinkedList<>();
        this.hook = hook;
    }

    /**
     * Loads a single Java class file into the cannon.
     *
     * @param file the Java class file.
     * @throws NullPointerException if the file is null
     */
    public void loadClassFile(File file) throws NullPointerException {
        if (file == null) throw new NullPointerException("File may not be null!");
        this.loadedFiles.add(file);
    }

    /**
     * Loads a single Java JAR file into the cannon.
     *
     * @param jarFile the JAR file.
     */
    public void loadJarFile(JarFile jarFile) {
        // TODO: Load class files into the cannon
    }

    /**
     * Fires all loaded Java classes currently loaded.
     */
    public void fireAll() {
        this.loadedFiles.stream().flatMap(f -> {
            try {
                this.fire(f);
                return null;
            } catch (IOException e) {
                return Stream.of(e);
            }
        }).reduce((e1, e2) -> {
            e1.addSuppressed(e2);
            return e1;
        }).ifPresent(logger::error);
    }

    /**
     * Fires the first file in the list of loaded in the cannon.
     */
    public void fireOne() {
        this.fireOne(0);
    }

    /**
     * Fires the file at index i in the cannon.
     *
     * @param i the index of the file to fire.
     */
    public void fireOne(final int i) {
        File f;
        try {
            f = this.loadedFiles.get(i);
            this.fire(f);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) logger.error("File at " + i + " not found!", e);
            else logger.error("I/O Exception while accessing '" + i + "'.", e);
        } catch (IndexOutOfBoundsException e) {
            logger.error("Index " + i + " is out of bounds!", e);
        }
    }

    /**
     * Attempts to fire a file from the cannon.
     *
     * @param f the file to fire.
     * @throws IOException if the ClassReader or visitors encounter an IOException.
     */
    private void fire(final File f) throws IOException {
        try (InputStream fis = new FileInputStream(f)) {
            ClassReader cr = new ClassReader(fis);

            DebugClassVisitor cfgVis = new DebugClassVisitor();
            ASTClassVisitor astVis = new ASTClassVisitor(hook);
            cr.accept(cfgVis, 0);
            cr.accept(astVis,0);
        }
    }
}
