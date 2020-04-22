package za.ac.sun.grapl.controllers;

import org.objectweb.asm.Label;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.JumpState;
import za.ac.sun.grapl.domain.models.vertices.*;
import za.ac.sun.grapl.domain.stack.BlockItem;
import za.ac.sun.grapl.domain.stack.OperandItem;
import za.ac.sun.grapl.domain.stack.block.*;
import za.ac.sun.grapl.domain.stack.operand.ConstantItem;
import za.ac.sun.grapl.domain.stack.operand.OperatorItem;
import za.ac.sun.grapl.domain.stack.operand.VariableItem;
import za.ac.sun.grapl.util.ASMParserUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ASTController extends AbstractController {

    private final Stack<OperandItem> operandStack = new Stack<>();
    private final HashSet<VariableItem> variables = new HashSet<>();

    private final Stack<BlockItem> bHistory = new Stack<>();
    private final HashSet<JumpBlock> allJumps = new HashSet<>();
    private final Map<IfCmpBlock, GotoBlock> pairedBlocks = new HashMap<>();

    private int order;
    private Label currentLabel;
    private String classPath;
    private FileVertex currentClass;
    private MethodVertex currentMethod;
    private int currentLineNo = -1;

    private ASTController() {
        order = 0;
    }

    public static ASTController getInstance() {
        return Singleton.INSTANCE;
    }

    public void resetOrder() {
        order = super.hook().maxOrder();
    }

    public void projectFileAndNamespace(final String namespace, final String className) {
        super.checkHook();
        this.classPath = namespace.isEmpty() ? className : namespace + "." + className;

        // Build NAMESPACE_BLOCK if packages are present
        NamespaceBlockVertex nbv = null;
        if (!namespace.isEmpty()) {
            // Populate namespace block chain
            String[] namespaceList = namespace.split("\\.");
            if (namespaceList.length > 0) nbv = this.populateNamespaceChain(namespaceList);
        }

        currentClass = new FileVertex(className, order++);
        // Join FILE and NAMESPACE_BLOCK if namespace is present
        if (!Objects.isNull(nbv)) {
            super.hook().joinFileVertexTo(currentClass, nbv);
        }
    }

    /**
     * Creates a change of namespace block vertices and returns the final one in the chain.
     *
     * @param namespaceList a list of package names
     * @return the final namespace block vertex in the chain (the one associated with the file)
     */
    private NamespaceBlockVertex populateNamespaceChain(final String[] namespaceList) {
        NamespaceBlockVertex prevNamespaceBlock = new NamespaceBlockVertex(namespaceList[0], namespaceList[0], order++);
        if (namespaceList.length == 1) return prevNamespaceBlock;

        NamespaceBlockVertex currNamespaceBlock = null;
        StringBuilder namespaceBuilder = new StringBuilder(namespaceList[0]);
        for (int i = 1; i < namespaceList.length; i++) {
            namespaceBuilder.append(".".concat(namespaceList[i]));
            currNamespaceBlock = new NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++);
            super.hook().joinNamespaceBlocks(prevNamespaceBlock, currNamespaceBlock);
            prevNamespaceBlock = currNamespaceBlock;
        }
        return currNamespaceBlock;
    }

    /**
     * Generates the method meta-data vertices describing the method being visited.
     */
    public void pushMethod(final String methodName, final String methodSignature, final int access) {
        this.clear();
        // Create METHOD
        final String shortName = methodName.substring(methodName.lastIndexOf('.') + 1);
        currentMethod = new MethodVertex(shortName, classPath.concat(".").concat(methodName), methodSignature, currentLineNo, order++);
        // Join FILE and METHOD
        hook().joinFileVertexTo(currentClass, currentMethod);
        // Create METHOD_PARAM_IN
        ASMParserUtil.obtainParameters(methodSignature)
                .forEach(p -> hook().createAndAddToMethod(
                        this.currentMethod,
                        new MethodParameterInVertex(
                                methodSignature,
                                ASMParserUtil.getReadableType(p),
                                ASMParserUtil.determineEvaluationStrategy(p, false),
                                p, currentLineNo, order++)));
        // Create METHOD_RETURN
        final String returnType = ASMParserUtil.obtainMethodReturnType(methodSignature);
        final EvaluationStrategies eval = ASMParserUtil.determineEvaluationStrategy(returnType, true);
        hook().createAndAddToMethod(
                this.currentMethod,
                new MethodReturnVertex(ASMParserUtil.getReadableType(returnType), returnType, eval, currentLineNo, order++)
        );
        // Create MODIFIER
        ASMParserUtil.determineModifiers(access, methodName)
                .forEach(m -> hook().createAndAddToMethod(currentMethod, new ModifierVertex(m, order++)));
    }

    public void pushConstInsnOperation(final Object val) {
        final String canonicalType = val.getClass().getCanonicalName().replaceAll("\\.", "/");
        final String className = canonicalType.substring(canonicalType.lastIndexOf("/") + 1);
        ConstantItem stackItem;
        if ("Integer".equals(className) || "Long".equals(className) || "Float".equals(className) || "Double".equals(className)) {
            stackItem = new ConstantItem(val.toString(), className.toUpperCase());
        } else {
            stackItem = new ConstantItem(val.toString(), canonicalType);
        }
        logger.debug("Pushing " + stackItem);
        operandStack.push(stackItem);
    }

    public void pushConstInsnOperation(final int opcode) {
        final String line = ASMifier.OPCODES[opcode];
        final String type;
        OperandItem item = null;

        if (line.charAt(0) == 'L') type = "LONG";
        else type = ASMParserUtil.getReadableType(line.charAt(0));

        if (ASMParserUtil.isConstant(line)) {
            String val = line.substring(line.indexOf('_') + 1).replace("M", "-");
            item = new ConstantItem(val, type);
        } else if (ASMParserUtil.isOperator(line)) {
            item = new OperatorItem(line.substring(1), type);
        }

        if (Objects.nonNull(item)) {
            logger.debug("Pushing " + item);
            operandStack.push(item);
        }
    }

    public void pushConstInsnOperation(final int opcode, final int operand) {
        final String type = ASMParserUtil.getReadableType(ASMifier.OPCODES[opcode].charAt(0));
        final ConstantItem item = new ConstantItem(String.valueOf(operand), type);
        logger.debug("Pushing " + item);
        operandStack.push(item);
    }

    public void pushVarInc(final int var, final int increment) {
        // TODO: This still has to be implemented
    }

    /**
     * Handles visitVarInsn if the opcode is a store operation.
     *
     * @param operation the store operation.
     * @param varName   the variable name.
     */
    public void pushVarInsnStore(final int varName, final String operation) {
        final OperandItem operandItem = operandStack.pop();
        final String varType = ASMParserUtil.getStackOperationType(operation);
        final VariableItem variableItem = getOrPutVariable(varName, varType);
        final BlockVertex baseBlock = new BlockVertex("STORE", order++, 1, varType, currentLineNo);
        final StoreBlock storeBlock = new StoreBlock(order, currentLabel);

        if (!bHistory.empty())
            hook().assignToBlock(currentMethod, baseBlock, bHistory.peek().order);
        else
            hook().assignToBlock(currentMethod, baseBlock, 0);

        storeBlock.setL(variableItem);
        storeBlock.setR(operandItem);
        logger.debug("Pushing " + storeBlock);

        bHistory.push(storeBlock);

        final LocalVertex leftChild = new LocalVertex(variableItem.id, variableItem.id, variableItem.type, currentLineNo, order++);
        hook().assignToBlock(currentMethod, leftChild, baseBlock.order);
        if (operandItem instanceof OperatorItem) {
            handleOperator(baseBlock, (OperatorItem) operandItem);
        } else if (operandItem instanceof ConstantItem) {
            hook().assignToBlock(
                    currentMethod,
                    new LiteralVertex(operandItem.id, order++, 1, varType, currentLineNo),
                    baseBlock.order);
        }

        bHistory.pop();
    }

    /**
     * Handles visitVarInsn if the opcode is a load operation.
     *
     * @param varName   the variable name.
     * @param operation the load operation.
     */
    public void pushVarInsnLoad(final int varName, final String operation) {
        final VariableItem variableItem = getOrPutVariable(varName, ASMParserUtil.getStackOperationType(operation));
        logger.debug("Pushing " + variableItem);
        operandStack.push(variableItem);
    }

    private VariableItem getOrPutVariable(final int varName, final String type) {
        final String varString = String.valueOf(varName);
        return variables.stream()
                .filter(variable -> varString.equals(variable.id))
                .findFirst()
                .orElseGet(() -> {
                    VariableItem temp = new VariableItem(varString, type);
                    variables.add(temp);
                    return temp;
                });
    }

    public void associateLineNumberWithLabel(final int line, final Label start) {
        this.currentLineNo = line;
        this.currentLabel = start;

        if (isJumpDestination(start)) {
            // TODO: Reconstruct rules
            handleJumpDestination(start);
        }

        if (!bHistory.isEmpty() && bHistory.peek() instanceof NestedBodyBlock) {
            if (!super.hook().isBlock(bHistory.peek().order)) {
                final NestedBodyBlock newBlock = ((NestedBodyBlock) bHistory.pop()).setLabel(start);
                final BlockVertex bodyVertex = new BlockVertex(newBlock.position.name(), newBlock.order, 1, "VOID", line);
                if (!bHistory.empty())
                    hook().assignToBlock(currentMethod, bodyVertex, bHistory.peek().order);
                else
                    hook().assignToBlock(currentMethod, bodyVertex, 0);
                bHistory.push(newBlock);
            }
        }


    }

    private void handleJumpDestination(final Label jumpDestination) {
        final List<JumpBlock> associatedJumps = getAssociatedJumps(jumpDestination);
        final Stack<JumpBlock> jumpHistory = getJumpHistory();
        final long numIfCmpAssocs = associatedJumps.stream().filter(g -> g instanceof IfCmpBlock).count();
        final long numGotoAssocs = associatedJumps.stream().filter(g -> g instanceof GotoBlock).count();
        logger.debug("Encountered jump destination #IfCmp:" + numIfCmpAssocs + " #Goto: " + numGotoAssocs);
        logger.debug("Associated jumps: " + associatedJumps);

        while (bHistory.size() > 2
                && !(bHistory.get(bHistory.size() - 2) instanceof JumpBlock)
                && bHistory.stream().anyMatch(g -> g instanceof IfCmpBlock)
        ) {
            bHistory.pop();
            bHistory.pop();
        }

        if (bHistory.size() >= 2) {
            final JumpBlock peekedBlock = (JumpBlock) bHistory.get(bHistory.size() - 2);
            if (peekedBlock.destination == jumpDestination) {
                bHistory.pop();
                if (peekedBlock.position == JumpState.IF_ROOT
                        && peekedBlock instanceof IfCmpBlock
                        && pairedBlocks.containsKey(peekedBlock)
                ) {
                    // Entering else-body
                    bHistory.push(new NestedBodyBlock(order++, currentLabel, JumpState.ELSE_BODY));
                } else {
                    // Exiting if-root
                    bHistory.pop();
                }
            }

        }
    }

    /**
     * Handles visitJumpInsn if the opcode is a nullary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to.
     */
    public void pushNullaryJumps(final String jumpOp, final Label label) {
        final List<JumpBlock> associatedJumps = getAssociatedJumps(label);
        final JumpBlock lastJump = getLastJump();
        assert Objects.nonNull(lastJump);
        final GotoBlock currentBlock = new GotoBlock(order, currentLabel, label, lastJump.position);
        logger.debug("Pushing " + currentBlock);

        allJumps.add(currentBlock);
        tryPairGotoBlock(currentBlock);

        BlockItem blockItem = null;
        while (!bHistory.isEmpty() && !(bHistory.peek() instanceof JumpBlock)) {
            blockItem = bHistory.pop();
        }

        if (Objects.nonNull(blockItem)) {
            if (bHistory.peek() instanceof JumpBlock && ((JumpBlock) bHistory.peek()).label != label) {
                bHistory.push(new NestedBodyBlock(order++, label, JumpState.ELSE_BODY));
            }
        }

    }

    /**
     * Handles visitJumpInsn if the opcode is a binary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    public void pushBinaryJump(final String jumpOp, final Label label) {
        logger.debug("Recognized binary jump " + jumpOp + " with label " + label.toString());
        final String jumpType = ASMParserUtil.getBinaryJumpType(jumpOp);
        // Build if-root and if-cond
        final BlockVertex condRoot = new BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo);
        final BlockVertex condBlock = new BlockVertex(ASMParserUtil.parseAndFlipEquality(jumpOp).toString(), order++, 2, jumpType, currentLineNo);
        if (bHistory.isEmpty())
            hook().assignToBlock(currentMethod, condRoot, 0);
        else
            hook().assignToBlock(currentMethod, condRoot, bHistory.peek().order);
        hook().assignToBlock(currentMethod, condBlock, condRoot.order);
        // Add if-cond operands
        final List<OperandItem> ops = Arrays.asList(operandStack.pop(), operandStack.pop());
        Collections.reverse(ops);
        logger.debug("Jump arguments = [" + ops.get(0) + ", " + ops.get(1) + "]");
        ops.forEach(op -> {
            if (op instanceof ConstantItem)
                hook().assignToBlock(currentMethod, new LiteralVertex(op.id, order++, 1, jumpType, currentLineNo), condBlock.order);
            else if (op instanceof VariableItem)
                hook().assignToBlock(currentMethod, new LocalVertex(op.id, op.id, op.type, currentLineNo, order++), condBlock.order);
        });

        pushJumpBlock(new IfCmpBlock(condRoot.order, currentLabel, label, JumpState.IF_ROOT));
        bHistory.push(new NestedBodyBlock(order++, currentLabel, JumpState.IF_BODY));
    }

    /**
     * Handles visitJumpInsn if the opcode is a unary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    public void pushUnaryJump(final String jumpOp, final Label label) {
        logger.debug("Recognized unary jump " + jumpOp + " with label " + label.toString());
        final OperandItem arg1 = operandStack.pop();
        logger.debug("Jump arguments = [" + arg1 + "]");
    }

    private void pushJumpBlock(final JumpBlock item) {
        bHistory.push(item);
        allJumps.add(item);
    }

    private JumpBlock getLastJump() {
        final ListIterator<BlockItem> listIterator = bHistory.listIterator(bHistory.size());
        while (listIterator.hasPrevious()) {
            final BlockItem prev = listIterator.previous();
            if (prev instanceof IfCmpBlock || prev instanceof GotoBlock) return (JumpBlock) prev;
        }
        return null;
    }

    private void tryPairGotoBlock(GotoBlock gotoBlock) {
        final ListIterator<BlockItem> listIterator = bHistory.listIterator(bHistory.size());
        while (listIterator.hasPrevious()) {
            final BlockItem prev = listIterator.previous();
            if (prev instanceof IfCmpBlock) {
                final IfCmpBlock prevIfCmp = (IfCmpBlock) prev;
                if (prevIfCmp.destination != gotoBlock.destination && !pairedBlocks.containsKey(prevIfCmp)) {
                    pairedBlocks.put(prevIfCmp, gotoBlock);
                }
            }
        }
    }

    private List<JumpBlock> getAssociatedJumps(final Label destination) {
        return allJumps.stream()
                .filter(j -> j.destination == destination)
                .collect(Collectors.toList());
    }

    private Stack<JumpBlock> getJumpHistory() {
        final Stack<JumpBlock> stack = new Stack<>();
        bHistory.stream()
                .filter(j -> j instanceof JumpBlock)
                .forEachOrdered(j -> stack.push((JumpBlock) j));
        return stack;
    }

    private boolean isJumpDestination(final Label label) {
        return allJumps.parallelStream()
                .map(j -> j.destination == label)
                .filter(b -> b)
                .findFirst()
                .orElse(false);
    }

    /**
     * Will handle the case of an operator being nested under a parent block.
     *
     * @param prevBlock    the parent block of this operation.
     * @param operatorItem the operator stack item.
     */
    private void handleOperator(final BlockVertex prevBlock, final OperatorItem operatorItem) {
        logger.debug("Next operator: " + operatorItem);

        final BlockVertex currBlock = new BlockVertex(operatorItem.id, order++, 1, operatorItem.type, currentLineNo);
        hook().assignToBlock(currentMethod, currBlock, prevBlock.order);

        // TODO: Assume all operations that aren't automatically evaluated by compiler are binary
        final int noOperands = 2;
        for (int i = 0; i < noOperands; i++) {
            final OperandItem stackItem = operandStack.pop();
            logger.debug("Next operand: " + stackItem);

            if (stackItem instanceof OperatorItem) {
                handleOperator(currBlock, (OperatorItem) stackItem);
            } else if (stackItem instanceof ConstantItem) {
                final ConstantItem constantItem = (ConstantItem) stackItem;
                final LiteralVertex literalVertex = new LiteralVertex(constantItem.id, order++, 1, constantItem.type, currentLineNo);
                hook().assignToBlock(currentMethod, literalVertex, currBlock.order);
            } else if (stackItem instanceof VariableItem) {
                final VariableItem variableItem = (VariableItem) stackItem;
                final LocalVertex localVertex = new LocalVertex(variableItem.id, variableItem.id, variableItem.type, currentLineNo, order++);
                hook().assignToBlock(currentMethod, localVertex, currBlock.order);
            }
        }
    }

    @Override
    public String toString() {
        return "\n" + this.getClass().getCanonicalName() + "#"
                + currentMethod.name + currentMethod.signature + "\n"
                + "Stack: " + operandStack + "\n"
                + "Variables: " + variables + "\n"
                + "Block history: " + bHistory + "\n";
    }

    public void clear() {
        operandStack.clear();
        variables.clear();
        bHistory.clear();
    }

    private static class Singleton {
        private static final ASTController INSTANCE = new ASTController();
    }

}
