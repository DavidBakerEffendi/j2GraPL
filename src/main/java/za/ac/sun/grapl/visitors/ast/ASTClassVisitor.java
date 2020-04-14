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
package za.ac.sun.grapl.visitors.ast;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.controllers.ASTController;

public class ASTClassVisitor extends ClassVisitor implements Opcodes {

    private final ASTController controller;

    public ASTClassVisitor(ClassVisitor cv) {
        super(ASM5, cv);
        this.controller = ASTController.getInstance();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        String className;
        String namespace;
        if (name.lastIndexOf('/') != -1) {
            className = name.substring(name.lastIndexOf('/') + 1);
            namespace = name.substring(0, name.lastIndexOf('/'));
        } else {
            className = name;
            namespace = "";
        }
        namespace = namespace.replaceAll("/", ".");

        controller.projectFileAndNamespace(namespace, className);
        // TODO: Could create MEMBER vertex from here to declare member classes
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        controller.pushMethod(name, descriptor, access);
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new ASTMethodVisitor(mv);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

}
