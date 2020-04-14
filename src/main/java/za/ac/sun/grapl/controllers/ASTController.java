package za.ac.sun.grapl.controllers;

import org.objectweb.asm.Label;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.JumpAssociations;
import za.ac.sun.grapl.domain.models.vertices.*;
import za.ac.sun.grapl.util.ASMParserUtil;

import java.util.*;

import static za.ac.sun.grapl.domain.enums.JumpAssociations.IF_CMP;
import static za.ac.sun.grapl.domain.enums.JumpAssociations.JUMP;

public class ASTController extends AbstractController {

    private final Map<Label, Integer> labelBlockNo = new HashMap<>();
    private final Map<String, String> localVars = new HashMap<>();
    private final Map<String, String> varTypes = new HashMap<>();
    private final Map<Label, List<JumpAssociations>> lblJumpAssocs = new HashMap<>();
    private final Stack<String> operandStack = new Stack<>();
    private final Stack<Integer> blockHistory = new Stack<>();
    private final Stack<JumpSnapshot> jumpStateHistory = new Stack<>();
    private int order;
    private String classPath;
    private FileVertex currentClass;
    private MethodVertex currentMethod;
    private boolean enteringJumpBody = false;
    private boolean currentJumpBodyEmpty = false;
    private int currentLineNo = -1;
    private JumpState jumpState = JumpState.METHOD_BODY;

    private ASTController() {
        order = 0;
    }

    public static ASTController getInstance() {
        return Singleton.INSTANCE;
    }

    public void resetOrder() {
        order = 0;
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
        this.labelBlockNo.put(start, line);

        if (lblJumpAssocs.containsKey(start)) {
            List<JumpAssociations> assocs = lblJumpAssocs.get(start);
            while (jumpStateHistory.size() >= 2 && assocs.contains(IF_CMP)
                    && jumpStateHistory.peek().label != start && !blockHistory.isEmpty()
                    && jumpStateHistory.peek().state == JumpState.IF_ROOT) {
                popJumpState();
                popJumpState();
            }
            if ((assocs.contains(IF_CMP)) && jumpStateHistory.peek().state == JumpState.IF_ROOT) {
                pushJumpState(JumpState.ELSE_BODY, start);
                enteringJumpBody = true;
            } else if ((assocs.contains(IF_CMP) && jumpStateHistory.peek().state == JumpState.IF_BODY) ||
                    (assocs.contains(JUMP) && jumpStateHistory.peek().state == JumpState.ELSE_BODY)) {
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
    public void pushVarInsnStore(String operation, String varName) {
        currentJumpBodyEmpty = false;
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

        final BlockVertex baseBlock = new BlockVertex(operation.substring(1), order++, 1, operationType, currentLineNo);
        if (!blockHistory.empty())
            hook().assignToBlock(currentMethod, baseBlock, blockHistory.peek());
        else
            hook().assignToBlock(currentMethod, baseBlock, 0);
        blockHistory.push(baseBlock.order);

        LocalVertex leftChild = new LocalVertex(varName, varName, varTypes.get(varName), currentLineNo, order++);
        hook().assignToBlock(currentMethod, leftChild, baseBlock.order);

        if (ASMParserUtil.isOperator(result)) {
            handleOperator(baseBlock, result, ASMParserUtil.getOperatorType(result));
        } else if (pfx == 'C') {
            hook().assignToBlock(
                    currentMethod,
                    new LiteralVertex(sfx, order++, 1, operationType, currentLineNo),
                    baseBlock.order);
        }
        blockHistory.pop();
    }

    /**
     * Handles visitVarInsn if the opcode is a load operation.
     *
     * @param operation the load operation.
     * @param varName   the variable name.
     */
    public void pushVarInsnLoad(String operation, String varName) {
        final String variableType = ASMParserUtil.getStackOperationType(operation);
        operandStack.push("V".concat(varName));
        varTypes.put(varName, variableType);
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
        hook().assignToBlock(currentMethod, currBlock, prevBlock.order);

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
                hook().assignToBlock(currentMethod, literalVertex, currBlock.order);
            } else if (pfx == 'V') {
                final LocalVertex localVertex = new LocalVertex(sfx, sfx, varTypes.get(sfx), currentLineNo, order++);
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
        logger.debug("Recognized nullary jump ".concat(jumpOp).concat(" with label ".concat(label.toString())));
        long numGotoAssocs = lblJumpAssocs.get(label).stream().filter((t) -> t == JUMP).count();

        if (jumpState == JumpState.IF_BODY) {
            if (lblJumpAssocs.get(label).contains(IF_CMP) && jumpStateHistory.peek().label == label) {
                popJumpState();
                popJumpState();
            }
            Label tempLabel = jumpStateHistory.peek().label;
            if (!currentJumpBodyEmpty) {
                popJumpState();
            }
            jumpStateHistory.pop(); // So that there aren't duplicate IF_ROOTs - this may be a result of a bug
            pushJumpState(JumpState.IF_ROOT, tempLabel);
        } else if (numGotoAssocs >= 2) {
            for (int i = 0; i < numGotoAssocs; i++) {
                popJumpState();
            }
            Label tempLabel = jumpStateHistory.peek().label;
            popJumpState();
            pushJumpState(JumpState.IF_ROOT, tempLabel);
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
        logger.debug("Recognized binary jump ".concat(jumpOp).concat(" with label ".concat(label.toString())));
        final String arg2 = operandStack.pop();
        final String arg1 = operandStack.pop();
        logger.debug("Jump arguments = [".concat(arg1).concat(", ").concat(arg2).concat("]"));
        final char[] pfx = new char[]{arg1.charAt(0), arg2.charAt(0)};
        final String[] sfx = new String[]{arg1.substring(1), arg2.substring(1)};
        final String jumpType = ASMParserUtil.getBinaryJumpType(jumpOp);

        BlockVertex condRoot = new BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo);
        BlockVertex condBlock = new BlockVertex(ASMParserUtil.parseAndFlipEquality(jumpOp).toString(), order++, 2, jumpType, currentLineNo);
        if (blockHistory.isEmpty())
            hook().assignToBlock(currentMethod, condRoot, 0);
        else
            hook().assignToBlock(currentMethod, condRoot, blockHistory.peek());
        hook().assignToBlock(currentMethod, condBlock, condRoot.order);

        for (int i = 0; i < pfx.length; i++) {
            if (pfx[i] == 'C')
                hook().assignToBlock(currentMethod, new LiteralVertex(sfx[i], order++, 1, jumpType, currentLineNo), condBlock.order);
            else if (pfx[i] == 'V')
                hook().assignToBlock(currentMethod, new LocalVertex(sfx[i], sfx[i], varTypes.get(sfx[i]), currentLineNo, order++), condBlock.order);
        }
        pushJumpState(JumpState.IF_ROOT, label);
        blockHistory.push(condRoot.order);
        // Let "body" methods know that they need to enter body
        pushJumpState(JumpState.IF_BODY, label);
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
        logger.debug("Recognized unary jump ".concat(jumpOp).concat(" with label ".concat(label.toString())));
        final String arg1 = operandStack.pop();
        logger.debug("Jump arguments = [".concat(arg1).concat("]"));
        enteringJumpBody = true;
    }

    public void pushConstInsnOperation(Object val) {
        logger.debug("Pushing constant " + val);
        operandStack.push("C".concat(val.toString()));
    }

    public void pushConstInsnOperation(int opcode) {
        final String line = ASMifier.OPCODES[opcode];
        if (ASMParserUtil.isConstant(line)) {
            logger.debug("Pushing constant " + line);
            operandStack.push("C".concat(line.substring(line.indexOf('_') + 1).replace("M", "-")));
        } else if (ASMParserUtil.isOperator(line)) {
            logger.debug("Pushing operator " + line);
            operandStack.push(line);
        }
    }

    public void pushConstInsnOperation(int opcode, int operand) {
        String line = ASMifier.OPCODES[opcode].concat(" ").concat(String.valueOf(operand));
        logger.debug("Pushing constant " + line);
        operandStack.push("C".concat(line.substring(line.indexOf(' ') + 1)));
    }

    public void pushVarInc(int var, int increment) {
        // TODO: This still has to be implemented
        currentJumpBodyEmpty = false;
    }

    private void pushJumpState(JumpState state, Label label) {
        jumpState = state;
        jumpStateHistory.push(new JumpSnapshot(state, label));
    }

    private void popJumpState() {
        jumpState = jumpStateHistory.pop().state;
        blockHistory.pop();
    }

    public void clear() {
        labelBlockNo.clear();
        localVars.clear();
        varTypes.clear();
        lblJumpAssocs.clear();
        operandStack.clear();
        blockHistory.clear();
        jumpStateHistory.clear();
    }

    private enum JumpState {
        METHOD_BODY,
        IF_ROOT,
        IF_BODY,
        ELSE_BODY
    }

    private static class Singleton {
        private static final ASTController INSTANCE = new ASTController();
    }

    private static class JumpSnapshot {

        public final JumpState state;
        public final Label label;

        public JumpSnapshot(JumpState state, Label label) {
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
