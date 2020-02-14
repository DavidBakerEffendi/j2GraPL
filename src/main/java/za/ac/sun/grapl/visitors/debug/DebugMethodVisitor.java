package za.ac.sun.grapl.visitors.debug;

import org.apache.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;

class DebugMethodVisitor extends MethodVisitor implements Opcodes {

    final static Logger logger = Logger.getLogger(DebugClassVisitor.class);

    public DebugMethodVisitor(final MethodVisitor mv) {
        super(ASM5, mv);
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitLabel(Label label) {
        logger.debug("\t  " + label.toString() + " (label)");
        super.visitLabel(label);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + owner + " " + name + " " + descriptor + " (visitFieldInsn)");
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + var + " (visitVarInsn)");
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        logger.debug("\t  " + line + " " + start + " (visitLineNumber)");
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        logger.debug("\t  " + maxStack + " " + maxLocals + " (visitMaxs)");
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitInsn(int opcode) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " (visitInsn)");
        super.visitInsn(opcode);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + label + " (visitJumpInsn)");
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        logger.debug("\t  " + value.toString() + " (visitLdcInsn)");
        super.visitLdcInsn(value);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        logger.debug("\t " + ASMifier.OPCODES[opcode] + " " + type + " (visitTypeInsn)");
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        logger.debug("\t  " + owner + " " + name + " " + desc + " (visitMethodInsn)");
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitEnd() {
        logger.debug("\t}");
    }
}