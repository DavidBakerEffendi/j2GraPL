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
import za.ac.sun.grapl.controllers.ClassMetaController;
import za.ac.sun.grapl.domain.meta.ClassInfo;
import za.ac.sun.grapl.domain.meta.MethodInfo;

public final class ASTClassVisitor extends ClassVisitor implements Opcodes {

    private final ClassMetaController classMetaController;
    private final ASTController astController;
    private ClassInfo classInfo;

    public ASTClassVisitor(final ClassMetaController classMetaController, final ASTController astController) {
        super(ASM5);
        this.classMetaController = classMetaController;
        this.astController = astController;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.classInfo = this.classMetaController.getClass(name);
        assert classInfo != null;
        this.astController.projectFileAndNamespace(classInfo.getNamespace(), classInfo.getClassName());
        // TODO: Could create MEMBER vertex from here to declare member classes
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        final MethodInfo methodInfo = classInfo.getMethod(name, descriptor, access);
        assert methodInfo != null;
        astController.pushNewMethod(methodInfo);
        return new ASTMethodVisitor(mv, astController);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

}
