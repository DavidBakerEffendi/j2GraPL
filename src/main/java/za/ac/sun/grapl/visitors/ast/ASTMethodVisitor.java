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

import java.util.*;

public class ASTMethodVisitor extends MethodVisitor implements Opcodes {

    private final static Logger logger = LogManager.getLogger();

    private final IHook hook;
    private final int access;
    private final String classPath;
    private final String methodName;
    private final String methodSignature;
    private final HashMap<Label, Integer> labelToLineNo = new HashMap<>();
    private final HashMap<String, String> localVars = new HashMap<>();
    private final HashMap<String, String> varTypes = new HashMap<>();
    private final Stack<Integer> blockHistory = new Stack<>();
    private final Stack<String> operandStack = new Stack<>();
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
        if (ASMParserUtil.isLoad(operation)) {
            final String variableType = ASMParserUtil.getStackOperationType(operation);
            logger.debug(new StringJoiner(" ")
                    .add("Recognized load instruction, pushing var (")
                    .add(varName)
                    .add(") to operand stack with type").add(variableType));
            operandStack.push(varName);
            varTypes.put(varName, variableType);
        } else if (ASMParserUtil.isStore(operation)) {
            logger.debug(new StringJoiner(" ")
                    .add("Recognized store instruction, popping result of").add(operandStack.peek())
                    .add("from operand stack and assigning result to").add(varName).add("\b."));
            final String result = operandStack.pop();
            final String operationType = ASMParserUtil.getStackOperationType(operation);

            if (localVars.containsKey(varName)) localVars.replace(varName, result);
            else localVars.put(varName, result);
            if (varTypes.containsKey(varName)) varTypes.replace(varName, operationType);
            else varTypes.put(varName, operationType);

            logger.debug(new StringJoiner(" ")
                    .add("Creating base block to method with order").add(String.valueOf(order)));
            final BlockVertex baseBlock = new BlockVertex(operation.substring(1), order++, 1, operationType, currentLineNo);
            this.hook.assignToBlock(methodVertex, baseBlock, 0);

            LocalVertex leftChild = new LocalVertex(varName, varName, varTypes.get(varName), currentLineNo, order++);
            this.hook.assignToBlock(methodVertex, leftChild, baseBlock.order);
            logger.debug(new StringJoiner(" ")
                    .add("Linked a block to left child").add(String.valueOf(baseBlock.order)).add("->")
                    .add(String.valueOf(order)));

            logger.debug(new StringJoiner(" ")
                    .add("Linked a block to right child").add(String.valueOf(baseBlock.order)).add("->")
                    .add(String.valueOf(order + 1)));
            if (ASMParserUtil.isOperator(result)) {
                handleOperator(baseBlock, result, varTypes.get(varName));
            } else {
                // TODO: Assumes RHS is literal - this will be addressed in a later feature
                this.hook.assignToBlock(
                        methodVertex,
                        new LiteralVertex(result, order++, 1, operationType, currentLineNo),
                        baseBlock.order);
            }
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
        BlockVertex currBlock = new BlockVertex(operator.substring(1), order++, 1, operatorType, currentLineNo);
        logger.debug(new StringJoiner(" ")
                .add("Linked a block to block").add(String.valueOf(prevBlock.order)).add("->")
                .add(String.valueOf(currBlock.order)));
        hook.assignToBlock(methodVertex, currBlock, prevBlock.order);

        List<String> operands = new LinkedList<>();
        // TODO: if binary, add 2 operands, if unary, add 1. Right now we assume binary
        operands.add(operandStack.pop());
        operands.add(operandStack.pop());
        // TODO: Right now assuming lhs and rhs are variables or literals
        operands.forEach(operand -> {
            if (ASMParserUtil.isPrimitive(operand.charAt(0))) {
                LiteralVertex literalVertex = new LiteralVertex(operand, order++, 1, operatorType, currentLineNo);
                hook.assignToBlock(methodVertex, literalVertex, currBlock.order);
            } else {
                // TODO: default assume this is variable
                LocalVertex localVertex = new LocalVertex(operand, operand, varTypes.get(operand), currentLineNo, order++);
                hook.assignToBlock(methodVertex, localVertex, currBlock.order);
            }
        });
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        if (this.currentLineNo == -1) {
            generateMethodHeaderVertices(line);
        }
        this.currentLineNo = line;
        this.labelToLineNo.put(start, line);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
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
            operandStack.push(line.substring(line.indexOf('_') + 1));
        } else if (ASMParserUtil.isOperator(line)) {
            logger.debug(new StringJoiner(" ").add("Recognized operator").add(line));
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
        operandStack.push(line);
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
        operandStack.push(line.substring(line.indexOf(' ') + 1));
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
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
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        // TODO: Identifiers are part of the CFG - LOCAL should just keep index
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
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
