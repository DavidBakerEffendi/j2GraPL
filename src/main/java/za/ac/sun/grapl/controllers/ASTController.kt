package za.ac.sun.grapl.controllers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.Label
import org.objectweb.asm.util.ASMifier
import za.ac.sun.grapl.domain.enums.JumpState
import za.ac.sun.grapl.domain.enums.ModifierTypes
import za.ac.sun.grapl.domain.meta.MethodInfo
import za.ac.sun.grapl.domain.models.vertices.BlockVertex
import za.ac.sun.grapl.domain.models.vertices.FileVertex
import za.ac.sun.grapl.domain.models.vertices.LiteralVertex
import za.ac.sun.grapl.domain.models.vertices.LocalVertex
import za.ac.sun.grapl.domain.models.vertices.MethodParameterInVertex
import za.ac.sun.grapl.domain.models.vertices.MethodReturnVertex
import za.ac.sun.grapl.domain.models.vertices.MethodVertex
import za.ac.sun.grapl.domain.models.vertices.ModifierVertex
import za.ac.sun.grapl.domain.models.vertices.NamespaceBlockVertex
import za.ac.sun.grapl.domain.stack.BlockItem
import za.ac.sun.grapl.domain.stack.OperandItem
import za.ac.sun.grapl.domain.stack.block.GotoBlock
import za.ac.sun.grapl.domain.stack.block.IfCmpBlock
import za.ac.sun.grapl.domain.stack.block.JumpBlock
import za.ac.sun.grapl.domain.stack.block.NestedBodyBlock
import za.ac.sun.grapl.domain.stack.block.StoreBlock
import za.ac.sun.grapl.domain.stack.operand.ConstantItem
import za.ac.sun.grapl.domain.stack.operand.OperatorItem
import za.ac.sun.grapl.domain.stack.operand.VariableItem
import za.ac.sun.grapl.hooks.IHook
import za.ac.sun.grapl.util.ASMParserUtil
import za.ac.sun.grapl.util.JumpStackUtil
import java.util.*
import java.util.function.Consumer
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.absoluteValue

class ASTController(
        private val hook: IHook
) : AbstractController {
    private val operandStack = Stack<OperandItem?>()
    private val variables = HashSet<VariableItem>()
    private val bHistory = Stack<BlockItem>()
    private val allJumpsEncountered = HashSet<JumpBlock>()
    private val futureIfBlock = Stack<BlockVertex>()
    private val pairedBlocks: MutableMap<IfCmpBlock, GotoBlock?> = HashMap()
    private var order = 0
    private var currentLabel: Label? = null
    private var classPath: String? = null
    private var currentClass: FileVertex? = null
    private var currentMethod: MethodVertex? = null
    private var currentLineNo = -1
    private lateinit var methodInfo: MethodInfo

    /**
     * Given a package name signature and the current class, will and resolve common package chains with the
     * class name as a {@link FileVertex} at the end.
     *
     * @param namespace the full package declaration.
     * @param className the name of the class.
     */
    fun projectFileAndNamespace(namespace: String, className: String) {
        classPath = if (namespace.isEmpty()) className else "$namespace.$className"
        // Build NAMESPACE_BLOCK if packages are present
        var nbv: NamespaceBlockVertex? = null
        if (namespace.isNotEmpty()) {
            // Populate namespace block chain
            val namespaceList = namespace.split(".").toTypedArray()
            if (namespaceList.isNotEmpty()) nbv = populateNamespaceChain(namespaceList)
        }
        currentClass = FileVertex(className, order++)
        // Join FILE and NAMESPACE_BLOCK if namespace is present
        if (!Objects.isNull(nbv)) {
            this.hook.joinFileVertexTo(currentClass, nbv)
        }
    }

    /**
     * Creates a change of namespace block vertices and returns the final one in the chain.
     *
     * @param namespaceList a list of package names
     * @return the final namespace block vertex in the chain (the one associated with the file)
     */
    private fun populateNamespaceChain(namespaceList: Array<String>): NamespaceBlockVertex? {
        var prevNamespaceBlock: NamespaceBlockVertex? = NamespaceBlockVertex(namespaceList[0], namespaceList[0], order++)
        if (namespaceList.size == 1) return prevNamespaceBlock
        var currNamespaceBlock: NamespaceBlockVertex? = null
        val namespaceBuilder = StringBuilder(namespaceList[0])
        for (i in 1 until namespaceList.size) {
            namespaceBuilder.append("." + namespaceList[i])
            currNamespaceBlock = NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++)
            this.hook.joinNamespaceBlocks(prevNamespaceBlock, currNamespaceBlock)
            prevNamespaceBlock = currNamespaceBlock
        }
        return currNamespaceBlock
    }

    /**
     * Generates the method meta-data vertices describing the method being visited.
     */
    fun pushNewMethod(methodInfo: MethodInfo) {
        this.clear()
        this.methodInfo = methodInfo
    }

    /**
     * Using the given method info and line number, will project method data on the graph.
     */
    private fun createMethod(methodInfo: MethodInfo, lineNumber: Int) {
        val methodName = methodInfo.methodName
        val methodSignature = methodInfo.methodSignature
        val access = methodInfo.access
        // Create METHOD
        val shortName = methodName.substring(methodName.lastIndexOf('.') + 1)
        currentMethod = MethodVertex(shortName, "$classPath.$methodName", methodSignature, lineNumber, order++)
        // Join FILE and METHOD
        hook.joinFileVertexTo(currentClass, currentMethod)
        // Create METHOD_PARAM_IN
        ASMParserUtil.obtainParameters(methodSignature)
                .forEach(Consumer { p: String ->
                    hook.createAndAddToMethod(
                            currentMethod,
                            MethodParameterInVertex(
                                    methodSignature,
                                    ASMParserUtil.getReadableType(p),
                                    ASMParserUtil.determineEvaluationStrategy(p, false),
                                    p, lineNumber, order++))
                })
        // Create METHOD_RETURN
        val returnType = ASMParserUtil.obtainMethodReturnType(methodSignature)
        val eval = ASMParserUtil.determineEvaluationStrategy(returnType, true)
        hook.createAndAddToMethod(
                currentMethod,
                MethodReturnVertex(ASMParserUtil.getReadableType(returnType), returnType, eval, lineNumber, order++)
        )
        // Create MODIFIER
        ASMParserUtil.determineModifiers(access, methodName)
                .forEach(Consumer { m: ModifierTypes? -> hook.createAndAddToMethod(currentMethod, ModifierVertex(m, order++)) })
    }

    fun pushConstInsnOperation(`val`: Any) {
        val canonicalType = `val`.javaClass.canonicalName.replace("\\.".toRegex(), "/")
        val className = canonicalType.substring(canonicalType.lastIndexOf("/") + 1)
        val stackItem: ConstantItem
        stackItem = if ("Integer" == className || "Long" == className || "Float" == className || "Double" == className) {
            ConstantItem(`val`.toString(), className.toUpperCase())
        } else {
            ConstantItem(`val`.toString(), canonicalType)
        }
        logger.debug("Pushing $stackItem")
        operandStack.push(stackItem)
    }

    fun pushConstInsnOperation(opcode: Int) {
        val line = ASMifier.OPCODES[opcode]
        val type: String
        var item: OperandItem? = null
        type = if (line[0] == 'L') "LONG" else ASMParserUtil.getReadableType(line[0])
        if (ASMParserUtil.isConstant(line)) {
            val `val` = line.substring(line.indexOf('_') + 1).replace("M", "-")
            item = ConstantItem(`val`, type)
        } else if (ASMParserUtil.isOperator(line)) {
            item = OperatorItem(line.substring(1), type)
        }
        if (Objects.nonNull(item)) {
            logger.debug("Pushing $item")
            operandStack.push(item)
        }
    }

    fun pushConstInsnOperation(opcode: Int, operand: Int) {
        val type = ASMParserUtil.getReadableType(ASMifier.OPCODES[opcode][0])
        val item = ConstantItem(operand.toString(), type)
        logger.debug("Pushing $item")
        operandStack.push(item)
    }

    /**
     * Handles visitIincInsn and artificially coordinates an arithmetic operation with STORE call. This is only called
     * for integers - other types go through the usual CONST/STORE/OPERATOR hooks.
     *
     * @param var the variable being incremented.
     * @param increment the amount by which `var` is being incremented.
     */
    fun pushVarInc(`var`: Int, increment: Int) {
        val opType = "INTEGER"
        val op = if (increment > 0) OperatorItem("ADD", opType) else OperatorItem("SUB", opType)
        val varItem = VariableItem(`var`.toString(), opType)
        val constItem = ConstantItem(increment.absoluteValue.toString(), opType)
        operandStack.addAll(listOf(varItem, constItem, op))
        pushVarInsnStore(`var`, "I${op.id}")
    }

    /**
     * Handles visitVarInsn if the opcode is a store operation.
     *
     * @param operation the store operation.
     * @param varName   the variable name.
     */
    fun pushVarInsnStore(varName: Int, operation: String) {
        val operandItem = operandStack.pop()
        val varType = ASMParserUtil.getStackOperationType(operation)
        val variableItem = getOrPutVariable(varName, varType)
        val baseBlock = BlockVertex("STORE", order++, 1, varType, currentLineNo)
        val storeBlock = StoreBlock(order, currentLabel)
        // Avoid attaching to loop roots
        while (!bHistory.empty() && methodInfo.isLabelAssociatedWithLoops(bHistory.peek().label!!)) {
            bHistory.pop()
        }
        if (!bHistory.empty()) hook.assignToBlock(currentMethod, baseBlock, bHistory.peek().order)
        else hook.assignToBlock(currentMethod, baseBlock, 0)
        storeBlock.l = variableItem
        storeBlock.r = operandItem
        logger.debug("Pushing $storeBlock")
        bHistory.push(storeBlock)
        val leftChild = LocalVertex(variableItem.id, variableItem.id, variableItem.type, currentLineNo, order++)
        hook.assignToBlock(currentMethod, leftChild, baseBlock.order)
        when (operandItem) {
            is OperatorItem -> {
                handleOperator(baseBlock, operandItem)
            }
            is ConstantItem -> {
                hook.assignToBlock(
                        currentMethod,
                        LiteralVertex(operandItem.id, order++, 1, varType, currentLineNo),
                        baseBlock.order)
            }
            is VariableItem -> {
                hook.assignToBlock(
                        currentMethod,
                        LocalVertex(operandItem.id, operandItem.id, operandItem.type, currentLineNo, order++),
                        baseBlock.order)
            }
        }
        bHistory.pop()
    }

    /**
     * Handles visitVarInsn if the opcode is a load operation.
     *
     * @param varName   the variable name.
     * @param operation the load operation.
     */
    fun pushVarInsnLoad(varName: Int, operation: String) {
        val variableItem = getOrPutVariable(varName, ASMParserUtil.getStackOperationType(operation))
        logger.debug("Pushing $variableItem")
        operandStack.push(variableItem)
    }

    private fun getOrPutVariable(varName: Int, type: String): VariableItem {
        val varString = varName.toString()
        return variables.stream()
                .filter { variable: VariableItem -> varString == variable.id }
                .findFirst()
                .orElseGet {
                    val temp = VariableItem(varString, type)
                    variables.add(temp)
                    temp
                }
    }

    fun associateLineNumberWithLabel(line: Int, start: Label) {
        // So that our pre-scan labels and current scan labels can resolve
        this.methodInfo.addLabel(line, start)

        if (currentLineNo == -1) {
            createMethod(methodInfo, line - 1)
        }
        currentLineNo = line
        currentLabel = start

        if (JumpStackUtil.isJumpDestination(allJumpsEncountered, start)) handleJumpDestination(start)

        val totalAssociatedJumps = this.methodInfo.getAssociatedJumps(line)
        val jumpCountDifference = totalAssociatedJumps.filter { jumpInfo -> jumpInfo.jumpOp != "GOTO" }.size - JumpStackUtil.getAssociatedJumps(allJumpsEncountered, start).size
        if (jumpCountDifference >= 1) {
            logger.debug("$start (line $line) is associated with $jumpCountDifference jump(s) that haven't been encountered yet | $totalAssociatedJumps")
            logger.debug("JumpInfo: ${totalAssociatedJumps.size} | Current assoc jumps ${JumpStackUtil.getAssociatedJumps(allJumpsEncountered, start).size}")
            // TODO get jump info since this current line no is technically wrong
            for (i in 0 until jumpCountDifference) {
                val destinationLineNumber = this.methodInfo.getLineNumber(totalAssociatedJumps.first().currLabel)
                val totalAssociatedJumpsWithDest = this.methodInfo.getAssociatedJumps(destinationLineNumber)
                logger.debug("Line $line vs JumpOrigin $destinationLineNumber = ${if (line < destinationLineNumber) "jump is above" else "jump is below"} which is associated with $totalAssociatedJumpsWithDest")
                if (line < destinationLineNumber && totalAssociatedJumpsWithDest.none { j -> j.jumpOp == "GOTO" }) {
                    val condRoot = BlockVertex("DO_WHILE", order++, 1, "BOOLEAN", currentLineNo)
                    futureIfBlock.add(condRoot)
                    if (bHistory.isEmpty()) {
                        hook.assignToBlock(currentMethod, condRoot, 0)
                    } else {
                        // Check if this nested body is not created to its if-root before this statement in Loop8 test
                        if (!hook.isBlock(bHistory.peek().order)) {
                            val bodyBlock = bHistory.pop()
                            val bodyVertex = BlockVertex("IF_BODY", bodyBlock.order, 1, "BOOLEAN", currentLineNo)
                            hook.assignToBlock(currentMethod, bodyVertex, bHistory.peek().order)
                            hook.assignToBlock(currentMethod, condRoot, bodyVertex.order)
                            bHistory.push(bodyBlock)
                        } else {
                            hook.assignToBlock(currentMethod, condRoot, bHistory.peek().order)
                        }
                    }

                    // We do not know the new label of the ifCmpBlock that we expect to appear later in the bytecode
                    val nestedBodyBlock = NestedBodyBlock(order++, currentLabel, JumpState.IF_BODY)
                    pushJumpBlock(IfCmpBlock(condRoot.order, null, start, JumpState.IF_ROOT))
                    bHistory.push(nestedBodyBlock)

                    // Account for the nested case
                    if (i != jumpCountDifference - 1 && jumpCountDifference > 1) {
                        val bodyVertex = BlockVertex(nestedBodyBlock.position.name, nestedBodyBlock.order, 1, "VOID", line)
                        hook.assignToBlock(currentMethod, bodyVertex, bHistory.peek().order - 1)
                    }
                }
            }
        }
        if (!bHistory.isEmpty() && bHistory.peek() is NestedBodyBlock) {
            if (!this.hook.isBlock(bHistory.peek().order)) {
                val newBlock = (bHistory.pop() as NestedBodyBlock).setLabel(start)
                val bodyVertex = BlockVertex(newBlock.position.name, newBlock.order, 1, "VOID", line)
                if (!bHistory.empty()) hook.assignToBlock(currentMethod, bodyVertex, bHistory.peek().order) else hook.assignToBlock(currentMethod, bodyVertex, 0)
                bHistory.push(newBlock)
            }
        }
    }

    private fun handleJumpDestination(jumpDestination: Label) {
        val associatedJumps = JumpStackUtil.getAssociatedJumps(allJumpsEncountered, jumpDestination)
        val numIfCmpAssocs = associatedJumps.stream().filter { g: JumpBlock? -> g is IfCmpBlock }.count()
        val numGotoAssocs = associatedJumps.stream().filter { g: JumpBlock? -> g is GotoBlock }.count()
        logger.debug("Encountered jump destination #IfCmp:$numIfCmpAssocs #Goto: $numGotoAssocs")
        logger.debug("Associated jumps: $associatedJumps")
        while (bHistory.size > 2 && bHistory[bHistory.size - 2] !is JumpBlock
                && bHistory.stream().anyMatch { g: BlockItem? -> g is IfCmpBlock }) {
            bHistory.pop()
            bHistory.pop()
        }
        if (bHistory.size >= 2) {
            val peekedBlock = bHistory[bHistory.size - 2] as JumpBlock
            val lastAssociatedJump = associatedJumps[associatedJumps.size - 1]
            if (peekedBlock.destination === jumpDestination) {
                bHistory.pop()
                if (peekedBlock.position == JumpState.IF_ROOT && peekedBlock is IfCmpBlock
                        && pairedBlocks.containsKey(peekedBlock)) {
                    // Entering else-body
                    bHistory.push(NestedBodyBlock(order++, currentLabel, JumpState.ELSE_BODY))
                } else {
                    // Exiting if-root
                    bHistory.pop()
                }
            } else if (lastAssociatedJump is GotoBlock) {
                // This pops the current state off of an edge-body, and thus, off of the if-root
                if (lastAssociatedJump.destination === jumpDestination) {
                    bHistory.pop()
                    bHistory.pop()
                }
            }
        }
    }

    /**
     * Handles visitJumpInsn if the opcode is a nullary jump.
     *
     * @param label  the label to jump to.
     */
    fun pushNullaryJumps(label: Label) {
        val jumpHistory = JumpStackUtil.getJumpHistory(bHistory)
        // TODO: This shouldn't reach null so this is not an ideal workaround
        val lastJump = JumpStackUtil.getLastJump(bHistory) ?: allJumpsEncountered.maxBy { j -> j.order }
        val currentBlock = GotoBlock(order, currentLabel, label, lastJump!!.position)
        logger.debug("Pushing $currentBlock")
        // Retain info learned from this method
        allJumpsEncountered.add(currentBlock)
        this.methodInfo.upsertJumpRootAtLine(currentLineNo, "GOTO")
        tryPairGotoBlock(currentBlock)
        val destinationLineNumber = this.methodInfo.getLineNumber(label)
        var blockItem: BlockItem? = null
        while (!bHistory.isEmpty() && bHistory.peek() !is JumpBlock) {
            blockItem = bHistory.pop()
        }
        if (Objects.nonNull(blockItem)) {
            // Read the last ifs and find which one is paired with this goto. Pop until I find the IfCmp paired
            while (!jumpHistory.isEmpty() && jumpHistory.size > 1) {
                // This brings the pointer to the correct level in the case of if-root without else-body
                val topBlock = jumpHistory.pop()
                if (topBlock is IfCmpBlock && pairedBlocks[topBlock] != null) {
                    if (pairedBlocks[topBlock] == currentBlock) break
                }
                bHistory.pop()
                bHistory.pop()
            }
            if (bHistory.peek() is JumpBlock && (bHistory.peek() as JumpBlock).label !== label) {
                bHistory.push(NestedBodyBlock(order++, label, JumpState.ELSE_BODY))
            }
            if (destinationLineNumber != -1 && destinationLineNumber < currentLineNo) {
                // This is GOTO is part of a loop
                pairedBlocks.filter { (_, v) -> v == currentBlock }.keys.forEach { pairedBlock ->
                    logger.debug("This GOTO is part of a loop @ order $order associated with block $pairedBlock")
                    // The label may not be initialized yet and will be updated at a later stage but we can try search
                    // it based on the destination label
                    val name: String?
                    val maybeLineNumber: Int?
                    if (pairedBlock.label != null) {
                        name = "WHILE"
                        maybeLineNumber = methodInfo.getLineNumber(pairedBlock.label!!)
                        this.methodInfo.upsertJumpRootAtLine(maybeLineNumber, name)
                    } else {
                        name = "DO_WHILE"
                        maybeLineNumber = methodInfo.findJumpLineBasedOnDestLabel(pairedBlock.destination)
                    }
                    if (maybeLineNumber != null) this.methodInfo.upsertJumpRootAtLine(maybeLineNumber, name)
                    this.hook.updateBlockProperty(currentMethod, pairedBlock.order, "name", name)
                    bHistory.pop()
                }

            }
        }
    }

    /**
     * Handles visitJumpInsn if the opcode is a binary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    fun pushBinaryJump(jumpOp: String, label: Label) {
        logger.debug("Recognized binary jump $jumpOp with label $label")
        val jumpType = ASMParserUtil.getBinaryJumpType(jumpOp)
        // If, as in the case of do-while, the if block happens after the body and thus the if-node already exists,
        // we should fetch the corresponding if-node
        val condRoot: BlockVertex = if (futureIfBlock.isNotEmpty()) {
            // Determine if the last future jump block is correlated to this jump
            if (!this.methodInfo.isJumpVertexAssociatedWithGivenLine(futureIfBlock.peek(), currentLineNo))
                BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo)
            else futureIfBlock.pop()
        } else BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo)
        this.methodInfo.upsertJumpRootAtLine(currentLineNo, condRoot.name)
        logger.debug("Using ${if (condRoot.order == order - 1) "new" else "existing"} vertex to represent IF_CMP")
        // We can tell if it's a brand new conditional route by checking the line number
        if (condRoot.order == order - 1) {
            if (bHistory.isEmpty()) hook.assignToBlock(currentMethod, condRoot, 0) else hook.assignToBlock(currentMethod, condRoot, bHistory.peek().order)
            pushJumpBlock(IfCmpBlock(condRoot.order, currentLabel, label, JumpState.IF_ROOT))
            bHistory.push(NestedBodyBlock(order++, currentLabel, JumpState.IF_BODY))
        } else {
            // We need to find the corresponding ifcmp block since its current label property is null by #associateLineNumberWithLabel
            allJumpsEncountered.find { jumpBlock -> jumpBlock.order == condRoot.order }?.label = label
            if (condRoot.name == "DO_WHILE") {
                bHistory.pop()
                bHistory.pop()
            }
        }

        val condBlock = BlockVertex(ASMParserUtil.parseAndFlipEquality(jumpOp).toString(), order++, 2, jumpType, currentLineNo)
        hook.assignToBlock(currentMethod, condBlock, condRoot.order)
        // Add if-cond operands
        val ops = listOfNotNull(operandStack.pop(), operandStack.pop()).asReversed()
        logger.debug("Jump arguments = [" + ops[0] + ", " + ops[1] + "]")
        ops.forEach(Consumer { op: OperandItem ->
            when (op) {
                is ConstantItem ->
                    hook.assignToBlock(currentMethod, LiteralVertex(op.id, order++, 1, jumpType, currentLineNo), condBlock.order)
                is VariableItem ->
                    hook.assignToBlock(currentMethod, LocalVertex(op.id, op.id, op.type, currentLineNo, order++), condBlock.order)
            }
        })
    }

    /**
     * Handles visitJumpInsn if the opcode is a unary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    fun pushUnaryJump(jumpOp: String, label: Label) {
        logger.debug("Recognized unary jump $jumpOp with label $label")
        val arg1 = operandStack.pop()
        logger.debug("Jump arguments = [$arg1]")
    }

    private fun pushJumpBlock(item: JumpBlock) {
        bHistory.push(item)
        allJumpsEncountered.add(item)
    }

    private fun tryPairGotoBlock(gotoBlock: GotoBlock) {
        val listIterator: ListIterator<BlockItem> = bHistory.listIterator(bHistory.size)
        while (listIterator.hasPrevious()) {
            val prev = listIterator.previous()
            if (prev is IfCmpBlock) {
                if (prev.destination !== gotoBlock.destination && !pairedBlocks.containsKey(prev)) {
                    pairedBlocks[prev] = gotoBlock
                }
            }
        }
    }

    /**
     * Will handle the case of an operator being nested under a parent block.
     *
     * @param prevBlock    the parent block of this operation.
     * @param operatorItem the operator stack item.
     */
    private fun handleOperator(prevBlock: BlockVertex, operatorItem: OperatorItem) {
        logger.debug("Next operator: $operatorItem")
        val currBlock = BlockVertex(operatorItem.id, order++, 1, operatorItem.type, currentLineNo)
        hook.assignToBlock(currentMethod, currBlock, prevBlock.order)

        // TODO: Assume all operations that aren't automatically evaluated by compiler are binary
        val noOperands = 2
        for (i in 0 until noOperands) {
            val stackItem = operandStack.pop()
            logger.debug("Next operand: $stackItem")
            when (stackItem) {
                is OperatorItem -> {
                    handleOperator(currBlock, stackItem)
                }
                is ConstantItem -> {
                    val literalVertex = LiteralVertex(stackItem.id, order++, 1, stackItem.type, currentLineNo)
                    hook.assignToBlock(currentMethod, literalVertex, currBlock.order)
                }
                is VariableItem -> {
                    val localVertex = LocalVertex(stackItem.id, stackItem.id, stackItem.type, currentLineNo, order++)
                    hook.assignToBlock(currentMethod, localVertex, currBlock.order)
                }
            }
        }
    }

    override fun toString(): String {
        return """
            ${this.javaClass.canonicalName}#${currentMethod!!.name}${currentMethod!!.signature}
            Stack: $operandStack
            Variables: $variables
            Block history: $bHistory
            """.trimIndent()
    }

    fun clear(): ASTController {
        operandStack.clear()
        variables.clear()
        bHistory.clear()
        currentLineNo = -1
        return this
    }

    /**
     * Sets the current order counter to the max order found in the currently connected graph database.
     */
    fun resetOrder(): ASTController {
        order = this.hook.maxOrder()
        return this
    }

    companion object {
        val logger: Logger = LogManager.getLogger()
    }

}
