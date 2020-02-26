package za.ac.sun.grapl.visitors.ast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.ModifierTypes;
import za.ac.sun.grapl.domain.models.vertices.MethodParameterInVertex;
import za.ac.sun.grapl.domain.models.vertices.MethodVertex;
import za.ac.sun.grapl.domain.models.vertices.ModifierVertex;
import za.ac.sun.grapl.hooks.IHook;
import za.ac.sun.grapl.util.ASMParserUtil;

import java.util.EnumSet;
import java.util.List;

public class ASTMethodVisitor extends MethodVisitor implements Opcodes {

    final static Logger logger = LogManager.getLogger();
    private final IHook hook;
    private final int access;
    private final String classPath;
    private final String methodName;
    private final String methodSignature;
    private int order = 0;

    public ASTMethodVisitor(final MethodVisitor mv, IHook hook, int access, String methodName, String classPath, String methodSignature) {
        super(ASM5, mv);
        this.hook = hook;
        this.access = access;
        this.methodName = methodName;
        this.classPath = classPath;
        this.methodSignature = methodSignature;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        final String shortName = methodName.substring(methodName.lastIndexOf('.') + 1);
        hook.createVertex(new MethodVertex(shortName, classPath.concat(".").concat(methodName), methodSignature, 0, order++));
        // Method parameter in. If primitive then easy, if object then need to find namespace
        // TODO: Determine parameters in based on signature and parameter types for the method
        final List<String> params = ASMParserUtil.obtainParameters(methodSignature);
        for (String p : params) {
            logger.debug("LONG NAME (" + p + ") SHORT NAME: (" + ASMParserUtil.getShortName(p) + ")");
        }
        params.parallelStream().forEach(p -> {
            hook.createVertex(new MethodParameterInVertex(
                    methodSignature,
                    ASMParserUtil.getShortName(p),
                    ASMParserUtil.determineEvaluationStrategy(p, false),
                    p, 0, order++));
            // TODO: Connect method to this vertex with AST edge
        });
        // TODO: Determine return type based on signature
        final String returnType = ASMParserUtil.obtainMethodReturnType(methodSignature);

        // TODO: Determine evaluation strategy
        final EvaluationStrategies eval = ASMParserUtil.determineEvaluationStrategy(returnType, true);
        // TODO: Add modifier here, e.g. public, based on access
        final EnumSet<ModifierTypes> modifiers = ASMParserUtil.determineModifiers(access, methodName);
        modifiers.forEach(m -> {
            hook.putVertexIfAbsent(new ModifierVertex(m, order++), "modifierType", m.name());
        });
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
        // TODO: Refer to the variable name by its label as this can be converted back later
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
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

    public void declareType() {
        // TODO: If we encounter an undeclared type, we should create it here
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
