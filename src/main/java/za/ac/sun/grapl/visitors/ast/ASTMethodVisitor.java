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
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.models.vertices.*;
import za.ac.sun.grapl.hooks.IHook;
import za.ac.sun.grapl.util.ASMParserUtil;

import java.util.HashMap;
import java.util.Stack;
import java.util.StringJoiner;

public class ASTMethodVisitor extends MethodVisitor implements Opcodes {
    private final static Logger logger = LogManager.getLogger();

    private final IHook hook;
    private final int access;
    private final String classPath;
    private final String methodName;
    private final String methodSignature;
    private final HashMap<Label, Integer> labelBlockNo = new HashMap<>();
    private final HashMap<String, String> localVars = new HashMap<>();
    private final HashMap<String, String> varTypes = new HashMap<>();
    private final HashMap<Label, String> labelJumpMap = new HashMap<>();
    private final Stack<String> operandStack = new Stack<>();
    private final Stack<Integer> blockHistory = new Stack<>();
    private final Stack<Label> jumpHistory = new Stack<>();
    private boolean enteringJumpBody = false;
    private int order = 0;
    private int currentLineNo = -1;
    private MethodVertex methodVertex;
    private FileVertex fv;


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

        if (ASMParserUtil.isLoad(operation)) visitVarInsnLoad(operation, varName);
        else if (ASMParserUtil.isStore(operation)) visitVarInsnStore(operation, varName);
    }

    /**
     * Handles visitVarInsn if the opcode is a load operation.
     *
     * @param operation the load operation.
     * @param varName   the variable name.
     */
    private void visitVarInsnLoad(String operation, String varName) {
        final String variableType = ASMParserUtil.getStackOperationType(operation);
        logger.debug(new StringJoiner(" ")
                .add("Recognized load instruction, pushing var (")
                .add(varName)
                .add(") to operand stack with type").add(variableType));
        operandStack.push("V".concat(varName));
        varTypes.put(varName, variableType);
    }

    /**
     * Handles visitVarInsn if the opcode is a store operation.
     *
     * @param operation the store operation.
     * @param varName   the variable name.
     */
    private void visitVarInsnStore(String operation, String varName) {
        final String result = operandStack.pop();
        final String sfx = result.substring(1);
        final char pfx = result.charAt(0);
        final String operationType = ASMParserUtil.getStackOperationType(operation);
        logger.debug(new StringJoiner(" ")
                .add("Recognized store instruction, popping result of").add(result)
                .add("from operand stack and assigning result to").add(varName).add("\b."));

        if (localVars.containsKey(varName)) localVars.replace(varName, result);
        else localVars.put(varName, result);
        if (varTypes.containsKey(varName)) varTypes.replace(varName, operationType);
        else varTypes.put(varName, operationType);

        logger.debug(new StringJoiner(" ")
                .add("Creating base block to method with order").add(String.valueOf(order)));
        if (enteringJumpBody) {
            BlockVertex jumpBodyBlock = new BlockVertex("BODY", order++, 1, "VOID", currentLineNo);
            this.hook.assignToBlock(methodVertex, jumpBodyBlock, blockHistory.peek());
            blockHistory.push(jumpBodyBlock.order);
            enteringJumpBody = false;
        }
        final BlockVertex baseBlock = new BlockVertex(operation.substring(1), order++, 1, operationType, currentLineNo);
        if (!blockHistory.empty()) this.hook.assignToBlock(methodVertex, baseBlock, blockHistory.peek());
        else this.hook.assignToBlock(methodVertex, baseBlock, 0);

        logger.debug(new StringJoiner(" ")
                .add("Linking a block to left child").add(String.valueOf(baseBlock.order)).add("->")
                .add(String.valueOf(order)));
        LocalVertex leftChild = new LocalVertex(varName, varName, varTypes.get(varName), currentLineNo, order++);
        this.hook.assignToBlock(methodVertex, leftChild, baseBlock.order);

        logger.debug(new StringJoiner(" ")
                .add("Linking a block to right child").add(String.valueOf(baseBlock.order)).add("->")
                .add(String.valueOf(order + 1)));
        if (ASMParserUtil.isOperator(result)) {
            handleOperator(baseBlock, result, ASMParserUtil.getOperatorType(result));
        } else if (pfx == 'C') {
            this.hook.assignToBlock(
                    methodVertex,
                    new LiteralVertex(sfx, order++, 1, operationType, currentLineNo),
                    baseBlock.order);
        }
    }

    /**
     * Will handle the case of an operator being nested under a parent block.
     *
     * @param prevBlock    the parent block of this operation.
     * @param operator     the operator of the statement.
     * @param operatorType the type of the operator.
     */
    private void handleOperator(BlockVertex prevBlock, String operator, String operatorType) {
        logger.debug(new StringBuilder().append("Next operator: ").append(operator));

        BlockVertex currBlock = new BlockVertex(operator.substring(1), order++, 1, operatorType, currentLineNo);
        logger.debug(new StringBuilder()
                .append("Joining block (").append(prevBlock.name).append(", ").append(prevBlock.order)
                .append(") -> (").append(currBlock.name).append(", ").append(currBlock.order).append(")"));
        hook.assignToBlock(methodVertex, currBlock, prevBlock.order);

        // TODO: Assume all operations that aren't automatically evaluated by compiler are binary
        int noOperands = 2;
        for (int i = 0; i < noOperands; i++) {
            final String operand = operandStack.pop();
            final String sfx = operand.substring(1);
            final char pfx = operand.charAt(0);
            logger.debug("Next operand: ".concat(operand));

            if (ASMParserUtil.isOperator(operand)) {
                handleOperator(currBlock, operand, ASMParserUtil.getOperatorType(operand));
            } else if (pfx == 'C') {
                final LiteralVertex literalVertex = new LiteralVertex(sfx, order++, 1, operatorType, currentLineNo);
                hook.assignToBlock(methodVertex, literalVertex, currBlock.order);
            } else if (pfx == 'V') {
                final LocalVertex localVertex = new LocalVertex(sfx, sfx, varTypes.get(sfx), currentLineNo, order++);
                hook.assignToBlock(methodVertex, localVertex, currBlock.order);
            }
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        if (this.currentLineNo == -1) generateMethodHeaderVertices(line);
        this.currentLineNo = line;
        this.labelBlockNo.put(start, line);

        if (!jumpHistory.isEmpty()) {
            if (jumpHistory.peek() == start) {
                logger.debug("Jump location identified: ".concat(start.toString().concat(" for ").concat(labelJumpMap.get(start))));
                logger.debug("Jump history ".concat(jumpHistory.toString()));
                blockHistory.pop();
                blockHistory.pop();
                jumpHistory.pop();
            }
        }
    }

    /**
     * An instantiation of a constant value to be pushed on the operand stack.
     *
     * @param opcode the opcode of the form "xCONST_y", an operator, or a type cast.
     */
    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        final String line = ASMifier.OPCODES[opcode];
        if (ASMParserUtil.isConstant(line)) {
            logger.debug(new StringJoiner(" ")
                    .add("Recognized constant, pushing").add(line)
                    .add("to the operand stack."));
            operandStack.push("C".concat(line.substring(line.indexOf('_') + 1).replace("M", "-")));
        } else if (ASMParserUtil.isOperator(line)) {
            logger.debug("Recognized operator ".concat(line));
            operandStack.push(line);
        }
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
        String line = val.toString();
        logger.debug(new StringJoiner(" ")
                .add("Recognized constant, pushing").add(line)
                .add("to the operand stack."));
        operandStack.push("C".concat(line));
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
        String line = ASMifier.OPCODES[opcode].concat(" ").concat(String.valueOf(operand));
        logger.debug(new StringJoiner(" ")
                .add("Recognized constant, pushing").add(line)
                .add("to the operand stack."));
        operandStack.push("C".concat(line.substring(line.indexOf(' ') + 1)));
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        final String jumpOp = ASMifier.OPCODES[opcode];
        jumpHistory.push(label);
        labelJumpMap.put(label, ASMifier.OPCODES[opcode]);

        if (ASMParserUtil.NULLARY_JUMPS.contains(jumpOp)) visitJumpInsnNullaryJumps(jumpOp, label);
        else if (ASMParserUtil.UNARY_JUMPS.contains(jumpOp)) visitJumpInsnUnaryJumps(jumpOp, label);
        else if (ASMParserUtil.BINARY_JUMPS.contains(jumpOp)) visitJumpInsnBinaryJumps(jumpOp, label);

        enteringJumpBody = true;
    }

    /**
     * Handles visitJumpInsn if the opcode is a nullary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to.
     */
    private void visitJumpInsnNullaryJumps(String jumpOp, Label label) {
        logger.debug("Recognized nullary jump ".concat(jumpOp).concat(" with label ".concat(label.toString())));
        blockHistory.pop();
    }

    /**
     * Handles visitJumpInsn if the opcode is a unary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    private void visitJumpInsnUnaryJumps(String jumpOp, Label label) {
        logger.debug("Recognized unary jump ".concat(jumpOp).concat(" with label ".concat(label.toString())));
        final String arg1 = operandStack.pop();
        logger.debug("Jump arguments = [".concat(arg1).concat("]"));
    }

    /**
     * Handles visitJumpInsn if the opcode is a binary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    private void visitJumpInsnBinaryJumps(String jumpOp, Label label) {
        logger.debug("Recognized binary jump ".concat(jumpOp).concat(" with label ".concat(label.toString())));
        final String arg2 = operandStack.pop();
        final String arg1 = operandStack.pop();
        logger.debug("Jump arguments = [".concat(arg1).concat(", ").concat(arg2).concat("]"));
        final char[] pfx = new char[]{arg1.charAt(0), arg2.charAt(0)};
        final String[] sfx = new String[]{arg1.substring(1), arg2.substring(1)};
        final String jumpType = ASMParserUtil.getBinaryJumpType(jumpOp);

        BlockVertex condRoot = new BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo);
        BlockVertex condBlock = new BlockVertex(ASMParserUtil.parseEquality(jumpOp).toString(), order++, 2, jumpType, currentLineNo);
        hook.assignToBlock(methodVertex, condRoot, 0);
        hook.assignToBlock(methodVertex, condBlock, condRoot.order);

        for (int i = 0; i < pfx.length; i++) {
            if (pfx[i] == 'C')
                hook.assignToBlock(methodVertex, new LiteralVertex(sfx[i], order++, 1, jumpType, currentLineNo), condBlock.order);
            else if (pfx[i] == 'V')
                hook.assignToBlock(methodVertex, new LocalVertex(sfx[i], sfx[i], varTypes.get(sfx[i]), currentLineNo, order++), condBlock.order);
        }

        blockHistory.push(condRoot.order);
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
        logger.debug("--- AST DEBUG INFO ---");
        logger.debug("VAR TABLE: ".concat(localVars.toString()));
        logger.debug("VAR TYPES: ".concat(varTypes.toString()));
    }

    public void generateMethodHeaderVertices(int lineNumber) {
        // Create METHOD
        final String shortName = methodName.substring(methodName.lastIndexOf('.') + 1);
        this.methodVertex = new MethodVertex(shortName, classPath.concat(".").concat(methodName), methodSignature, lineNumber, order++);
        hook.createVertex(this.methodVertex);
        // Join FILE and METHOD
        hook.joinFileVertexTo(fv, methodVertex);
        // Create METHOD_PARAM_IN
        ASMParserUtil.obtainParameters(methodSignature)
                .forEach(p -> hook.createAndAddToMethod(
                        this.methodVertex,
                        new MethodParameterInVertex(
                                methodSignature,
                                ASMParserUtil.getReadableType(p),
                                ASMParserUtil.determineEvaluationStrategy(p, false),
                                p, lineNumber, order++)));
        // Create METHOD_RETURN
        final String returnType = ASMParserUtil.obtainMethodReturnType(methodSignature);
        final EvaluationStrategies eval = ASMParserUtil.determineEvaluationStrategy(returnType, true);
        hook.createAndAddToMethod(
                this.methodVertex,
                new MethodReturnVertex(ASMParserUtil.getReadableType(returnType), returnType, eval, lineNumber, order++)
        );
        // Create MODIFIER
        ASMParserUtil.determineModifiers(access, methodName)
                .forEach(m -> hook.createAndAddToMethod(this.methodVertex, new ModifierVertex(m, order++)));
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
