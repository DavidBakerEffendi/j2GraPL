package za.ac.sun.grapl.visitors.ast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.ModifierTypes;
import za.ac.sun.grapl.domain.models.vertices.*;
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
    private MethodVertex methodVertex;
    private FileVertex fv;
    private int order = 0;

    public ASTMethodVisitor(final MethodVisitor mv, IHook hook, int access, String methodName, String classPath, String methodSignature, FileVertex fv) {
        super(ASM5, mv);
        this.hook = hook;
        this.access = access;
        this.methodName = methodName;
        this.classPath = classPath;
        this.methodSignature = methodSignature;
        this.fv = fv;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        // Create METHOD
        final String shortName = methodName.substring(methodName.lastIndexOf('.') + 1);
        this.methodVertex = new MethodVertex(shortName, classPath.concat(".").concat(methodName), methodSignature, 0, order++);
        hook.createVertex(this.methodVertex);
        // Join FILE and METHOD
        hook.joinFileVertexTo(fv, methodVertex);
        // Create METHOD_PARAM_IN
        final List<String> params = ASMParserUtil.obtainParameters(methodSignature);
        params.forEach(p -> hook.createAndAddToMethod(this.methodVertex,
                new MethodParameterInVertex(
                        methodSignature,
                        ASMParserUtil.getShortName(p),
                        ASMParserUtil.determineEvaluationStrategy(p, false),
                        p, 0, order++)));
        // Create METHOD_RETURN
        final String returnType = ASMParserUtil.obtainMethodReturnType(methodSignature);
        final EvaluationStrategies eval = ASMParserUtil.determineEvaluationStrategy(returnType, true);
        hook.createAndAddToMethod(this.methodVertex,
                new MethodReturnVertex(ASMParserUtil.getShortName(returnType), returnType, eval, 0, order++));
        // Create MODIFIER
        final EnumSet<ModifierTypes> modifiers = ASMParserUtil.determineModifiers(access, methodName);
        modifiers.forEach(m -> {
            hook.createAndAddToMethod(this.methodVertex, new ModifierVertex(m, order++));
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
