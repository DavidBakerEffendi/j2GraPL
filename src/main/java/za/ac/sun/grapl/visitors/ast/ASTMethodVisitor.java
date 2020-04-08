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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.controllers.ASTController;
import za.ac.sun.grapl.util.ASMParserUtil;

public class ASTMethodVisitor extends MethodVisitor implements Opcodes {

    private ASTController controller;

    public ASTMethodVisitor(final MethodVisitor mv) {
        super(ASM5, mv);
        this.controller = ASTController.getInstance();
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        controller.lineNumberClone(line, start);
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        String varName = String.valueOf(var);
        String operation = ASMifier.OPCODES[opcode];

        if (ASMParserUtil.isLoad(operation)) controller.visitVarInsnLoad(operation, varName);
        else if (ASMParserUtil.isStore(operation)) controller.visitVarInsnStore(operation, varName);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        controller.visitIincInsnClone(var, increment);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        final String jumpOp = ASMifier.OPCODES[opcode];
        controller.addJumpLabelAssoc(label, jumpOp);

        if (ASMParserUtil.NULLARY_JUMPS.contains(jumpOp)) controller.visitJumpInsnNullaryJumps(jumpOp, label);
        else if (ASMParserUtil.UNARY_JUMPS.contains(jumpOp)) controller.visitJumpInsnUnaryJumps(jumpOp, label);
        else if (ASMParserUtil.BINARY_JUMPS.contains(jumpOp)) controller.visitJumpInsnBinaryJumps(jumpOp, label);
    }

    /**
     * An instantiation of a constant value to be pushed on the operand stack.
     *
     * @param opcode the opcode of the form "xCONST_y", an operator, or a type cast.
     */
    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        controller.visitInsnClone(opcode);
    }

    /**
     * An instantiation of an arbitrary int, float, long, double, String, or class constant to push onto the operand
     * stack.
     *
     * @param val the arbitrary value.
     */
    @Override
    public void visitLdcInsn(Object val) {
        super.visitLdcInsn(val);
        controller.visitLdcInsnClone(val);
    }

    /**
     * An instantiation of a byte or short value to be pushed on the operand stack.
     *
     * @param opcode  the push operation in the form of "{B, S}PUSH"
     * @param operand the value to be pushed onto the operand stack.
     */
    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        controller.visitIntInsnClone(opcode, operand);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

}
