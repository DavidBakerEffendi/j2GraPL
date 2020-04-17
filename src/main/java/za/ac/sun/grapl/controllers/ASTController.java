package za.ac.sun.grapl.controllers;

import org.objectweb.asm.Label;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.IfCmpPosition;
import za.ac.sun.grapl.domain.enums.JumpAssociations;
import za.ac.sun.grapl.domain.models.vertices.*;
import za.ac.sun.grapl.domain.stack.BlockItem;
import za.ac.sun.grapl.domain.stack.OperandItem;
import za.ac.sun.grapl.domain.stack.block.GotoBlock;
import za.ac.sun.grapl.domain.stack.block.IfCmpBlock;
import za.ac.sun.grapl.domain.stack.block.JumpBlock;
import za.ac.sun.grapl.domain.stack.block.StoreBlock;
import za.ac.sun.grapl.domain.stack.operand.ConstantItem;
import za.ac.sun.grapl.domain.stack.operand.OperatorItem;
import za.ac.sun.grapl.domain.stack.operand.VariableItem;
import za.ac.sun.grapl.util.ASMParserUtil;

import java.util.*;

import static za.ac.sun.grapl.domain.enums.JumpAssociations.IF_CMP;
import static za.ac.sun.grapl.domain.enums.JumpAssociations.JUMP;

public class ASTController extends AbstractController {

    private final Map<Label, List<JumpAssociations>> lblJumpAssocs = new HashMap<>();
    private final Stack<OperandItem> operandStack = new Stack<>();
    private final HashSet<VariableItem> variables = new HashSet<>();

    private final Stack<Integer> blockHistory = new Stack<>();
    private final Stack<JumpSnapshot> jumpStateHistory = new Stack<>();

    private final Stack<BlockItem> bHistory = new Stack<>();
    private final HashSet<JumpBlock> allJumps = new HashSet<>();

    private int order;
    private Label currentLabel;
    private String classPath;
    private FileVertex currentClass;
    private MethodVertex currentMethod;
    private boolean enteringJumpBody = false;
    private boolean currentJumpBodyEmpty = false;
    private int currentLineNo = -1;
    private IfCmpPosition jumpState = IfCmpPosition.METHOD_BODY;

    private ASTController() {
        order = 0;
    }

    public static ASTController getInstance() {
        return Singleton.INSTANCE;
    }

    public void resetOrder() {
        order = super.hook().maxOrder();
    }

    public void projectFileAndNamespace(String namespace, String className) {
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
    private NamespaceBlockVertex populateNamespaceChain(String[] namespaceList) {
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
    public void pushMethod(String methodName, String methodSignature, int access) {
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

    public void lineNumberClone(int line, Label start) {
        this.currentLineNo = line;
        this.currentLabel = start;

        if (isJumpDestination(start)) {
            List<JumpAssociations> assocs = lblJumpAssocs.get(start);
            while (jumpStateHistory.size() >= 2 && assocs.contains(IF_CMP)
                    && jumpStateHistory.peek().label != start && !blockHistory.isEmpty()
                    && jumpStateHistory.peek().state == IfCmpPosition.IF_ROOT) {
                popJumpState();
                popJumpState();
            }
            if ((assocs.contains(IF_CMP)) && jumpStateHistory.peek().state == IfCmpPosition.IF_ROOT) {
                pushJumpState(IfCmpPosition.ELSE_BODY, start);
                enteringJumpBody = true;
            } else if ((assocs.contains(IF_CMP) && jumpStateHistory.peek().state == IfCmpPosition.IF_BODY) ||
                    (assocs.contains(JUMP) && jumpStateHistory.peek().state == IfCmpPosition.ELSE_BODY)) {
                popJumpState();
                popJumpState();
            }
        }

        if (enteringJumpBody) {
            enteringJumpBody = false;
            BlockVertex jumpBodyBlock = new BlockVertex(jumpState.name(), order++, 1, "VOID", currentLineNo);
            hook().assignToBlock(currentMethod, jumpBodyBlock, blockHistory.peek());
            blockHistory.push(jumpBodyBlock.order);
        }
    }

    /**
     * Handles visitVarInsn if the opcode is a store operation.
     *
     * @param operation the store operation.
     * @param varName   the variable name.
     */
    public void pushVarInsnStore(int varName, String operation) {
        currentJumpBodyEmpty = false;
        final OperandItem operandItem = operandStack.pop();
        final String varType = ASMParserUtil.getStackOperationType(operation);
        final VariableItem variableItem = getOrPutVariable(varName, varType);
        final String operationType = ASMParserUtil.getStackOperationType(operation);
        final BlockVertex baseBlock = new BlockVertex("STORE", order++, 1, operationType, currentLineNo);

        if (!blockHistory.empty())
            hook().assignToBlock(currentMethod, baseBlock, blockHistory.peek());
        else
            hook().assignToBlock(currentMethod, baseBlock, 0);

        final StoreBlock storeBlock = new StoreBlock(order, currentLabel);
        storeBlock.setL(variableItem);
        storeBlock.setR(operandItem);
        logger.debug("Pushing " + storeBlock);

        blockHistory.push(baseBlock.order);
        bHistory.push(storeBlock);

        LocalVertex leftChild = new LocalVertex(variableItem.id, variableItem.id, variableItem.type, currentLineNo, order++);
        hook().assignToBlock(currentMethod, leftChild, baseBlock.order);
        if (operandItem instanceof OperatorItem) {
            handleOperator(baseBlock, (OperatorItem) operandItem);
        } else if (operandItem instanceof ConstantItem) {
            hook().assignToBlock(
                    currentMethod,
                    new LiteralVertex(operandItem.id, order++, 1, operationType, currentLineNo),
                    baseBlock.order);
        }

        blockHistory.pop();
        bHistory.pop();
    }

    /**
     * Handles visitVarInsn if the opcode is a load operation.
     *
     * @param varName   the variable name.
     * @param operation the load operation.
     */
    public void pushVarInsnLoad(int varName, String operation) {
        final VariableItem variableItem = getOrPutVariable(varName, ASMParserUtil.getStackOperationType(operation));
        logger.debug("Pushing " + variableItem);
        operandStack.push(variableItem);
    }

    private VariableItem getOrPutVariable(int varName, String type) {
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

    /**
     * Will handle the case of an operator being nested under a parent block.
     *
     * @param prevBlock    the parent block of this operation.
     * @param operatorItem the operator stack item.
     */
    private void handleOperator(BlockVertex prevBlock, OperatorItem operatorItem) {
        logger.debug("Next operator: " + operatorItem);

        BlockVertex currBlock = new BlockVertex(operatorItem.id, order++, 1, operatorItem.type, currentLineNo);
        logger.debug(new StringBuilder()
                .append("Joining block (").append(prevBlock.name).append(", ").append(prevBlock.order)
                .append(") -> (").append(currBlock.name).append(", ").append(currBlock.order).append(")"));
        hook().assignToBlock(currentMethod, currBlock, prevBlock.order);

        // TODO: Assume all operations that aren't automatically evaluated by compiler are binary
        int noOperands = 2;
        for (int i = 0; i < noOperands; i++) {
            final OperandItem stackItem = operandStack.pop();
            logger.debug("Next operand: " + stackItem);

            if (stackItem instanceof OperatorItem) {
                handleOperator(currBlock, (OperatorItem) stackItem);
            } else if (stackItem instanceof ConstantItem) {
                ConstantItem constantItem = (ConstantItem) stackItem;
                final LiteralVertex literalVertex = new LiteralVertex(constantItem.id, order++, 1, constantItem.type, currentLineNo);
                hook().assignToBlock(currentMethod, literalVertex, currBlock.order);
            } else if (stackItem instanceof VariableItem) {
                VariableItem variableItem = (VariableItem) stackItem;
                final LocalVertex localVertex = new LocalVertex(variableItem.id, variableItem.id, variableItem.type, currentLineNo, order++);
                hook().assignToBlock(currentMethod, localVertex, currBlock.order);
            }
        }
    }

    /**
     * Handles visitJumpInsn if the opcode is a nullary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to.
     */
    public void pushNullaryJumps(String jumpOp, Label label) {
        logger.debug("Recognized nullary jump " + jumpOp + " with label " + label.toString());
        long numGotoAssocs = lblJumpAssocs.get(label).stream().filter((t) -> t == JUMP).count();

        pushJumpBlock(new GotoBlock(order, currentLabel, label, jumpState));

        if (jumpState == IfCmpPosition.IF_BODY) {
            if (lblJumpAssocs.get(label).contains(IF_CMP) && jumpStateHistory.peek().label == label) {
                popJumpState();
                popJumpState();
            }
            Label tempLabel = jumpStateHistory.peek().label;
            if (!currentJumpBodyEmpty) {
                popJumpState();
            }
            jumpStateHistory.pop(); // So that there aren't duplicate IF_ROOTs - this may be a result of a bug
            pushJumpState(IfCmpPosition.IF_ROOT, tempLabel);
        } else if (numGotoAssocs >= 2) {
            for (int i = 0; i < numGotoAssocs; i++) {
                popJumpState();
            }
            Label tempLabel = jumpStateHistory.peek().label;
            popJumpState();
            pushJumpState(IfCmpPosition.IF_ROOT, tempLabel);
        }
        enteringJumpBody = true;
    }

    /**
     * Handles visitJumpInsn if the opcode is a binary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    public void pushBinaryJump(String jumpOp, Label label) {
        logger.debug("Recognized binary jump " + jumpOp + " with label " + label.toString());
        final String jumpType = ASMParserUtil.getBinaryJumpType(jumpOp);
        // Build if-root and if-cond
        BlockVertex condRoot = new BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo);
        BlockVertex condBlock = new BlockVertex(ASMParserUtil.parseAndFlipEquality(jumpOp).toString(), order++, 2, jumpType, currentLineNo);
        if (blockHistory.isEmpty())
            hook().assignToBlock(currentMethod, condRoot, 0);
        else
            hook().assignToBlock(currentMethod, condRoot, blockHistory.peek());
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

        pushJumpState(IfCmpPosition.IF_ROOT, label);

        pushJumpBlock(new IfCmpBlock(condRoot.order, currentLabel, label, IfCmpPosition.IF_ROOT));

        blockHistory.push(condRoot.order);
        // Let "body" methods know that they need to enter body
        pushJumpState(IfCmpPosition.IF_BODY, label);
        enteringJumpBody = true;
        // Trigger "empty body" which will only be checked before another GOTO is encountered
        currentJumpBodyEmpty = true;
    }

    /**
     * Will associate the jump operation with the label in {@link ASTController#lblJumpAssocs}.
     *
     * @param label  the {@link Label} to associate.
     * @param jumpOp the jump operation to parse and append to the current jump associations.
     */
    public void addJumpLabelAssoc(Label label, String jumpOp) {
        JumpAssociations assoc = ASMParserUtil.parseJumpAssociation(jumpOp);
        if (lblJumpAssocs.containsKey(label)) {
            lblJumpAssocs.get(label).add(assoc);
        } else {
            lblJumpAssocs.put(label, new LinkedList<>(Collections.singletonList(assoc)));
        }
    }

    /**
     * Handles visitJumpInsn if the opcode is a unary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    public void pushUnaryJump(String jumpOp, Label label) {
        logger.debug("Recognized unary jump " + jumpOp + " with label " + label.toString());
        final OperandItem arg1 = operandStack.pop();
        logger.debug("Jump arguments = [" + arg1 + "]");
        enteringJumpBody = true;
    }

    public void pushConstInsnOperation(Object val) {
        String canonicalType = val.getClass().getCanonicalName().replaceAll("\\.", "/");
        String className = canonicalType.substring(canonicalType.lastIndexOf("/") + 1);
        String type;
        if ("Integer".equals(className) || "Long".equals(className) || "Float".equals(className) || "Double".equals(className)) {
            type = className.toUpperCase();
        } else {
            type = canonicalType;
        }
        ConstantItem stackItem = new ConstantItem(val.toString(), type);
        logger.debug("Pushing " + stackItem);
        operandStack.push(stackItem);
    }

    public void pushConstInsnOperation(int opcode) {
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

    public void pushConstInsnOperation(int opcode, int operand) {
        String type = ASMParserUtil.getReadableType(ASMifier.OPCODES[opcode].charAt(0));
        ConstantItem item = new ConstantItem(String.valueOf(operand), type);
        logger.debug("Pushing " + item);
        operandStack.push(item);
    }

    public void pushVarInc(int var, int increment) {
        // TODO: This still has to be implemented
        currentJumpBodyEmpty = false;
    }

    private void pushJumpState(IfCmpPosition state, Label label) {
        jumpState = state;
        jumpStateHistory.push(new JumpSnapshot(state, label));
    }

    private void popJumpState() {
        jumpState = jumpStateHistory.pop().state;
        blockHistory.pop();
    }

    private void pushJumpBlock(JumpBlock item) {
        bHistory.push(item);
        allJumps.add(item);
    }

    private BlockItem getLastJump() {
        ListIterator<BlockItem> listIterator = bHistory.listIterator(bHistory.size());
        while (listIterator.hasPrevious()) {
            BlockItem prev = listIterator.previous();
            if (prev instanceof IfCmpBlock || prev instanceof GotoBlock) return prev;
        }
        return null;
    }

    private boolean isJumpDestination(Label label) {
        return allJumps.parallelStream()
                .map(j -> j.destination == label)
                .filter(b -> b)
                .findFirst()
                .orElse(false);
    }

    @Override
    public String toString() {
        return "DEBUG INFO: " + this.getClass().getCanonicalName() + "\n"
                + "Stack: " + operandStack + "\n"
                + "Variables: " + variables + "\n"
                + "Block history: " + bHistory + "\n";
    }

    public void clear() {
        lblJumpAssocs.clear();
        blockHistory.clear();
        jumpStateHistory.clear();
        operandStack.clear();
        variables.clear();
        bHistory.clear();
    }

    private static class Singleton {
        private static final ASTController INSTANCE = new ASTController();
    }

    private static class JumpSnapshot {

        public final IfCmpPosition state;
        public final Label label;

        public JumpSnapshot(IfCmpPosition state, Label label) {
            this.state = state;
            this.label = label;
        }

        @Override
        public String toString() {
            return "[".concat(state.toString())
                    .concat(" -> ").concat(label.toString()).concat("]");
        }
    }

}
