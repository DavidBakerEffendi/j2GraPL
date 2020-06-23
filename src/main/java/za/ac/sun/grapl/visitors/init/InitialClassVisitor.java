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
package za.ac.sun.grapl.visitors.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.controllers.ClassMetaController;
import za.ac.sun.grapl.domain.enums.ModifierTypes;
import za.ac.sun.grapl.domain.meta.ClassInfo;
import za.ac.sun.grapl.domain.meta.MethodInfo;
import za.ac.sun.grapl.util.ASMParserUtil;

import java.util.EnumSet;
import java.util.StringJoiner;

public final class InitialClassVisitor extends ClassVisitor implements Opcodes {

    private final static Logger logger = LogManager.getLogger();

    private ClassInfo classInfo;
    private final ClassMetaController classMetaController;

    public InitialClassVisitor(final ClassMetaController classMetaController) {
        super(ASM5);
        this.classMetaController = classMetaController;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.classInfo = this.classMetaController.putClass(name, access);
        logger.debug("");
        logger.debug(this.classInfo.toString() + " extends " + superName + " {");
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        final MethodInfo methodInfo = classInfo.addMethod(name, descriptor, access, -1);
        logger.debug("");
        logger.debug("\t " + methodInfo + " {");
        return new InitialMethodVisitor(mv, methodInfo);
    }

    @Override
    public void visitEnd() {
        logger.debug("");
        logger.debug("}");
        super.visitEnd();
    }

}
