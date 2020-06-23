package za.ac.sun.grapl.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.controllers.OpStackController;
import za.ac.sun.grapl.util.ASMParserUtil;

public class OpStackMethodVisitor extends MethodVisitor implements Opcodes {

    private final OpStackController controller;

    public OpStackMethodVisitor(
            final MethodVisitor mv,
            final OpStackController abstractController
    ) {
        super(ASM5, mv);
        this.controller = abstractController;
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        controller.pushConstInsnOperation(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        controller.pushConstInsnOperation(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        final String operation = ASMifier.OPCODES[opcode];

        if (ASMParserUtil.isLoad(operation)) controller.pushVarInsnLoad(var, operation);
        else if (ASMParserUtil.isStore(operation)) controller.pushVarInsnStore(var, operation);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        final String jumpOp = ASMifier.OPCODES[opcode];

        if (ASMParserUtil.NULLARY_JUMPS.contains(jumpOp)) controller.pushNullaryJumps(label);
        else if (ASMParserUtil.UNARY_JUMPS.contains(jumpOp)) controller.pushUnaryJump(jumpOp, label);
        else if (ASMParserUtil.BINARY_JUMPS.contains(jumpOp)) controller.pushBinaryJump(jumpOp, label);
    }

    @Override
    public void visitLdcInsn(Object val) {
        super.visitLdcInsn(val);
        controller.pushConstInsnOperation(val);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        controller.pushVarInc(var, increment);
    }

}
