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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.controllers.ASTController;
import za.ac.sun.grapl.domain.meta.ClassInfo;
import za.ac.sun.grapl.domain.meta.LocalVarInfo;
import za.ac.sun.grapl.domain.meta.MethodInfo;
import za.ac.sun.grapl.util.ASMParserUtil;

public final class ASTMethodVisitor extends MethodVisitor implements Opcodes {

    final static Logger logger = LogManager.getLogger();

    private final ASTController astController;

    public ASTMethodVisitor(
            final MethodVisitor mv,
            final ASTController astController
    ) {
        super(ASM5, mv);
        this.astController = astController;
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        astController.pushConstInsnOperation(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        astController.pushConstInsnOperation(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        final String operation = ASMifier.OPCODES[opcode];

        if (ASMParserUtil.isLoad(operation)) astController.pushVarInsnLoad(var, operation);
        else if (ASMParserUtil.isStore(operation)) astController.pushVarInsnStore(var, operation);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        final String jumpOp = ASMifier.OPCODES[opcode];

        if (ASMParserUtil.NULLARY_JUMPS.contains(jumpOp)) astController.pushNullaryJumps(label);
        else if (ASMParserUtil.UNARY_JUMPS.contains(jumpOp)) astController.pushUnaryJump(jumpOp, label);
        else if (ASMParserUtil.BINARY_JUMPS.contains(jumpOp)) astController.pushBinaryJump(jumpOp, label);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
    }

    @Override
    public void visitLdcInsn(Object val) {
        super.visitLdcInsn(val);
        astController.pushConstInsnOperation(val);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        astController.pushVarInc(var, increment);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        astController.associateLineNumberWithLabel(line, start);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        logger.debug(astController.toString());
    }

}
