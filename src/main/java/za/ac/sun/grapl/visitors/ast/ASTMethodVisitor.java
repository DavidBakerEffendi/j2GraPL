package za.ac.sun.grapl.visitors.ast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.domain.models.vertices.MethodVertex;
import za.ac.sun.grapl.hooks.IHook;

public class ASTMethodVisitor extends MethodVisitor implements Opcodes {

    final static Logger logger = LogManager.getLogger();
    private final IHook hook;
    private final String classPath;
    private final String methodName;
    private final String methodSignature;
    private int currentLine = -1;
    private int order = 0;

    public ASTMethodVisitor(final MethodVisitor mv, IHook hook, String methodName, String classPath, String methodSignature) {
        super(ASM5, mv);
        this.hook = hook;
        this.methodName = methodName;
        this.classPath = classPath;
        this.methodSignature = methodSignature;
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
        if (currentLine == -1) {
            String shortName = methodName.substring(methodName.lastIndexOf('.') + 1);
            hook.createVertex(new MethodVertex(shortName, classPath + "#" + methodName, methodSignature, line - 1, order++));
        }
        logger.debug("\t  " + line + " " + start + " (visitLineNumber)");
        this.currentLine = line;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
