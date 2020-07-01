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
package za.ac.sun.grapl.controllers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.enums.JumpState
import za.ac.sun.grapl.domain.enums.ModifierTypes
import za.ac.sun.grapl.domain.meta.JumpInfo
import za.ac.sun.grapl.domain.models.GraPLVertex
import za.ac.sun.grapl.domain.models.vertices.*
import za.ac.sun.grapl.domain.stack.BlockItem
import za.ac.sun.grapl.domain.stack.OperandItem
import za.ac.sun.grapl.domain.stack.block.*
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

class ASTController(
        private val hook: IHook
) : OpStackController() {

    private val logger: Logger = LogManager.getLogger()

    private val bHistory = Stack<BlockItem>()
    private val allJumpsEncountered = HashSet<JumpBlock>()
    private val vertexStack = Stack<Pair<GraPLVertex, Int>>()
    private val pairedBlocks: MutableMap<IfCmpBlock, GotoBlock?> = HashMap()
    private var order = 0
    private var currentLabel: Label? = null
    private var classPath: String? = null
    private var currentClass: FileVertex? = null
    private var currentMethod: MethodVertex? = null
    private var currentLineNo = -1

    lateinit var methodInfo: MethodInfoController

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
    fun pushNewMethod(methodInfo: MethodInfoController) {
        this.clear()
        this.methodInfo = methodInfo
    }

    /**
     * Using the given method info and line number, will project method data on the graph.
     */
    private fun createMethod(methodInfo: MethodInfoController, lineNumber: Int) {
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

    /**
     * Handles visitVarInsn if the opcode is a store operation.
     *
     * @param operation the store operation.
     * @param varName   the variable name.
     */
    override fun pushVarInsnStore(varName: Int, operation: String) {
        val operandItem = operandStack.pop()!!
        val varType = ASMParserUtil.getStackOperationType(operation)
        val variableItem = getOrPutVariable(varName, varType)
        val baseBlock = BlockVertex("STORE", order++, 1, varType, currentLineNo)
        val storeBlock = StoreBlock(order, currentLabel)
        storeBlock.l = variableItem
        // Avoid attaching to loop roots
        while (!bHistory.empty() && methodInfo.isLabelAssociatedWithLoops(bHistory.peek().label!!)) {
            bHistory.pop()
        }
        val maybeTernaryPair = vertexStack.takeIf { it.isNotEmpty() }?.peek()?.second?.let { methodInfo.getAssociatedTernaryJump(it) }
        if (maybeTernaryPair == null) {
            if (!bHistory.empty()) {
                hook.assignToBlock(currentMethod, baseBlock, bHistory.peek().order)
            } else hook.assignToBlock(currentMethod, baseBlock, 0)
            storeBlock.r = operandItem
        } else {
            storeTernaryOperatorBody(baseBlock, storeBlock, operandItem, varType)
        }

        logger.debug("Pushing $storeBlock")
        bHistory.push(storeBlock)
        val leftChild = LocalVertex(variableItem.id, variableItem.id, variableItem.type, currentLineNo, order++)
        hook.assignToBlock(currentMethod, leftChild, baseBlock.order)
        if (maybeTernaryPair == null)
            attachOperandItem(baseBlock, operandItem, varType)
        bHistory.pop()
    }

    private fun storeTernaryOperatorBody(baseBlock: BlockVertex, storeBlock: StoreBlock, operandItem: OperandItem, varType: String) {
        val body = bHistory.pop()
        val root = bHistory.pop()
        val pair = vertexStack.pop()
        if (!bHistory.empty()) hook.joinBlocks(bHistory.peek().order, baseBlock.order)
        else hook.assignToBlock(currentMethod, baseBlock, 0)
        // Join if to store
        val ifRoot = (pair.first as BlockVertex)
        hook.joinBlocks(baseBlock.order, ifRoot.order)
        // Create if-body and join latest stack const
        val ifBodyVertex = BlockVertex("IF_BODY", order++, 1, "BOOLEAN", currentLineNo)
        val elseBodyVertex = BlockVertex("ELSE_BODY", body.order, 1, "BOOLEAN", currentLineNo)
        hook.createFreeBlock(ifBodyVertex)
        hook.createFreeBlock(elseBodyVertex)
        hook.joinBlocks(ifRoot.order, ifBodyVertex.order)
        hook.joinBlocks(ifRoot.order, elseBodyVertex.order)
        val ifBodyOperand = operandStack.pop()!!
        attachOperandItem(ifBodyVertex, ifBodyOperand, ifBodyOperand.type)
        storeBlock.r = root
        hook.assignToBlock(LiteralVertex(operandItem.id, order++, 1, varType, currentLineNo), body.order)
    }

    private fun attachOperandItem(baseBlock: BlockVertex, operandItem: OperandItem, varType: String) {
        when (operandItem) {
            is OperatorItem -> {
                vertexStack.push(Pair(baseBlock, pseudoLineNo))
                handleOperator(operandItem)
            }
            is ConstantItem ->
                hook.assignToBlock(
                        LiteralVertex(operandItem.id, order++, 1, varType, currentLineNo),
                        baseBlock.order)
            is VariableItem ->
                hook.assignToBlock(
                        LocalVertex(operandItem.id, operandItem.id, operandItem.type, currentLineNo, order++),
                        baseBlock.order)
        }
    }

    /**
     * Handles the combination of line number and label.
     */
    fun associateLineNumberWithLabel(line: Int, start: Label) {
        // So that our pre-scan labels and current scan labels can resolve
        if (currentLineNo == -1) {
            createMethod(methodInfo, line - 1)
        }
        currentLineNo = line
        currentLabel = start

        val totalAssociatedJumps = this.methodInfo.getAssociatedJumps(super.pseudoLineNo)
        val jumpCountDifference = totalAssociatedJumps.filter { jumpInfo -> jumpInfo.jumpOp != "GOTO" }.size - JumpStackUtil.getAssociatedJumps(allJumpsEncountered, start).size

        if (JumpStackUtil.isJumpDestination(allJumpsEncountered, start)) handleJumpDestination(start)
        if (jumpCountDifference >= 1) handleLoopDestination(start, super.pseudoLineNo, jumpCountDifference, totalAssociatedJumps)

        if (!bHistory.isEmpty() && bHistory.peek() is NestedBodyBlock) {
            if (!this.hook.isBlock(bHistory.peek().order)) {
                val newBlock = (bHistory.pop() as NestedBodyBlock).setLabel(start)
                val bodyVertex = BlockVertex(newBlock.position.name, newBlock.order, 1, "VOID", line)
                if (!bHistory.empty()) hook.assignToBlock(currentMethod, bodyVertex, bHistory.peek().order) else hook.assignToBlock(currentMethod, bodyVertex, 0)
                bHistory.push(newBlock)
            }
        }
    }

    /**
     * Handles a line that is associated with a jump operation's destination.
     */
    private fun handleJumpDestination(jumpDestination: Label) {
        val associatedJumps = JumpStackUtil.getAssociatedJumps(allJumpsEncountered, jumpDestination)
        val numIfCmpAssocs = associatedJumps.stream().filter { g: JumpBlock? -> g is IfCmpBlock }.count()
        val numGotoAssocs = associatedJumps.stream().filter { g: JumpBlock? -> g is GotoBlock }.count()
        logger.debug("Encountered jump destination @ line $currentLineNo (${super.pseudoLineNo}) #IfCmp: $numIfCmpAssocs #Goto: $numGotoAssocs")
        logger.debug("Associated jumps: $associatedJumps")
        // Handles if-else-if chains
        if (numGotoAssocs + numIfCmpAssocs > 1 && numGotoAssocs >= numIfCmpAssocs && bHistory.size > 2) {
            for (i in 0 until numGotoAssocs * (1 + numIfCmpAssocs)) {
                if (bHistory.size < 2) break
                bHistory.pop()
                bHistory.pop()
            }
        }
        // Makes sure if-else bodies are on the same level
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
                if (peekedBlock.position == JumpState.IF_ROOT
                        && peekedBlock is IfCmpBlock
                        && pairedBlocks.containsKey(peekedBlock)
                        && listOf("WHILE", "DO_WHILE").none { s -> s == methodInfo.getJumpRootName(bHistory.peek()?.label) }) {
                    // Entering else-body (ignore if it's a loop)
                    bHistory.push(NestedBodyBlock(order++, currentLabel, JumpState.ELSE_BODY))
                } else {
                    // Exiting if-root
                    bHistory.pop()
                }
            } else if (lastAssociatedJump is GotoBlock && lastAssociatedJump.destination === jumpDestination) {
                // This pops the current state off of an edge-body, and thus, off of the if-root
                bHistory.pop()
                bHistory.pop()
            }
        }
    }

    /**
     * Handles a loop jump destination e.g. while loop.
     */
    private fun handleLoopDestination(start: Label, line: Int, jumpCountDifference: Int, totalAssociatedJumps: MutableList<JumpInfo>) {
        logger.debug("$start (line $line) is associated with $jumpCountDifference jump(s) that haven't been encountered yet | $totalAssociatedJumps")
        logger.debug("JumpInfo: ${totalAssociatedJumps.size} | Current assoc jumps ${JumpStackUtil.getAssociatedJumps(allJumpsEncountered, start).size}")
        for (i in 0 until jumpCountDifference) {
            val destinationLineNumber = this.methodInfo.getPseudoLineNumber(totalAssociatedJumps.first().currLabel)
            val totalAssociatedJumpsWithDest = this.methodInfo.getAssociatedJumps(destinationLineNumber)
            logger.debug("Line $line vs JumpOrigin $destinationLineNumber = ${if (line < destinationLineNumber) "jump is above" else "jump is below"} which is associated with $totalAssociatedJumpsWithDest")
            if (line < destinationLineNumber && totalAssociatedJumpsWithDest.none { j -> j.jumpOp == "GOTO" }) {
                val condRoot = BlockVertex("DO_WHILE", order++, 1, "BOOLEAN", currentLineNo)
                vertexStack.push(Pair(condRoot, pseudoLineNo))
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

    /**
     * Handles visitJumpInsn if the opcode is a nullary jump.
     *
     * @param label  the label to jump to.
     */
    override fun pushNullaryJumps(label: Label) {
        super.pushNullaryJumps(label)
        val jumpHistory = JumpStackUtil.getJumpHistory(bHistory)
        val lastJump = JumpStackUtil.getLastJump(bHistory) ?: allJumpsEncountered.maxBy { j -> j.order }
        val currentBlock = GotoBlock(order, currentLabel, label, lastJump!!.position)
        logger.debug("Pushing $currentBlock")
        // Retain info learned from this method
        allJumpsEncountered.add(currentBlock)
        this.methodInfo.upsertJumpRootAtLine(super.pseudoLineNo, "GOTO")
        tryPairGotoBlock(currentBlock)
        val destinationLineNumber = this.methodInfo.getPseudoLineNumber(label)
        var blockItem: BlockItem? = null
        while (!bHistory.isEmpty() && bHistory.peek() !is JumpBlock) blockItem = bHistory.pop()
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
            logger.debug("Destination line for nullary jump: $destinationLineNumber vs current ${super.pseudoLineNo}")
            if (destinationLineNumber != -1 && destinationLineNumber < super.pseudoLineNo) {
                // This is GOTO is part of a loop
                pairedBlocks.filter { (_, v) -> v == currentBlock }.keys.forEach { pairedBlock ->
                    logger.debug("This GOTO is part of a loop @ order $order associated with block $pairedBlock")
                    // The label may not be initialized yet and will be updated at a later stage but we can try search
                    // it based on the destination label
                    val name: String?
                    val maybeLineNumber: Int?
                    if (pairedBlock.label != null) {
                        name = "WHILE"
                        maybeLineNumber = methodInfo.getPseudoLineNumber(pairedBlock.label!!)
                        this.methodInfo.upsertJumpRootAtLine(maybeLineNumber, name)
                    } else {
                        name = "DO_WHILE"
                        maybeLineNumber = methodInfo.findJumpLineBasedOnDestLabel(pairedBlock.destination)
                    }
                    if (maybeLineNumber != null) {
                        this.methodInfo.upsertJumpRootAtLine(maybeLineNumber, name)
                    }
                    this.hook.updateBlockProperty(currentMethod, pairedBlock.order, "name", name)

                    if (bHistory.peek() !is NestedBodyBlock)
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
    override fun pushBinaryJump(jumpOp: String, label: Label): List<OperandItem> {
        logger.debug("Recognized binary jump $jumpOp with label $label")
        val jumpType = ASMParserUtil.getBinaryJumpType(jumpOp)
        val maybeTernaryPair = methodInfo.getAssociatedTernaryJump(pseudoLineNo)
        // If, as in the case of do-while, the if block happens after the body and thus the if-node already exists,
        // we should fetch the corresponding if-node
        val condRoot: BlockVertex = if (!vertexStack.none { pair -> pair.first is BlockVertex }) {
            // Determine if the last future jump block is correlated to this jump
            val vertexLinePair = vertexStack.peek()
            if (!methodInfo.isJumpVertexAssociatedWithGivenLine(vertexLinePair.second, pseudoLineNo))
                BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo)
            else {
                vertexStack.pop().first as BlockVertex
            }
        } else BlockVertex("IF", order++, 1, "BOOLEAN", currentLineNo)
        this.methodInfo.upsertJumpRootAtLine(pseudoLineNo, condRoot.name)

        val condBlock = BlockVertex(ASMParserUtil.parseAndFlipEquality(jumpOp).toString(), order++, 2, jumpType, currentLineNo)
        logger.debug("Using ${if (condRoot.order == order - 2) "new" else "existing ${condRoot.name}"} vertex to represent IF_CMP")
        // We can tell if it's a brand new conditional route by checking the order
        if (condRoot.order == order - 2) {
            if (maybeTernaryPair != null) {
               prepareStackForTernaryJump(label, condRoot, condBlock)

            } else {
                if (bHistory.isEmpty()) {
                    hook.assignToBlock(currentMethod, condRoot, 0)
                }
                else hook.assignToBlock(currentMethod, condRoot, bHistory.peek().order)
                pushJumpBlock(IfCmpBlock(condRoot.order, currentLabel, label, JumpState.IF_ROOT))
                bHistory.push(NestedBodyBlock(order++, currentLabel, JumpState.IF_BODY))
            }


        } else {
            // We need to find the corresponding ifcmp block since its current label property is null by #associateLineNumberWithLabel
            allJumpsEncountered.find { jumpBlock -> jumpBlock.order == condRoot.order }?.label = label
            if (condRoot.name == "DO_WHILE") {
                bHistory.pop()
                bHistory.pop()
            }
        }

        return buildJumpCondition(condBlock, condRoot, jumpOp, label, jumpType)
    }

    private fun prepareStackForTernaryJump(label: Label, condRoot: BlockVertex, condBlock: BlockVertex) {
        logger.debug("Preparing the stack for a ternary jump")
        hook.createFreeBlock(condRoot)
        vertexStack.push(Pair(condBlock, pseudoLineNo))
        vertexStack.push(Pair(condRoot, pseudoLineNo))
        pushJumpBlock(IfCmpBlock(condRoot.order, currentLabel, label, JumpState.IF_ROOT))
        bHistory.push(NestedBodyBlock(order++, currentLabel, JumpState.IF_BODY))
    }

    private fun buildJumpCondition(condBlock: BlockVertex, condRoot: BlockVertex, jumpOp: String, label: Label, jumpType: String): List<OperandItem> {
        hook.createFreeBlock(condBlock)
        hook.joinBlocks(condRoot.order, condBlock.order)
        // Add if-cond operands
        val ops = super.pushBinaryJump(jumpOp, label)
        logger.debug("Jump arguments = [" + ops[0] + ", " + ops[1] + "]")
        ops.forEach(Consumer { op: OperandItem ->
            when (op) {
                is ConstantItem ->
                    hook.assignToBlock(LiteralVertex(op.id, order++, 1, jumpType, currentLineNo), condBlock.order)
                is VariableItem ->
                    hook.assignToBlock(LocalVertex(op.id, op.id, op.type, currentLineNo, order++), condBlock.order)
            }
        })
        return ops
    }

    /**
     * Handles visitJumpInsn if the opcode is a unary jump.
     *
     * @param jumpOp the jump operation.
     * @param label  the label to jump to if the jump condition is satisfied.
     */
    override fun pushUnaryJump(jumpOp: String, label: Label): OperandItem? {
        logger.debug("Recognized unary jump $jumpOp with label $label")
        val arg1 = super.pushUnaryJump(jumpOp, label)
        logger.debug("Jump arguments = [$arg1]")
        return arg1
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
     * @param operatorItem the operator stack item.
     */
    override fun handleOperator(operatorItem: OperatorItem) {
        logger.debug("Next operator: $operatorItem")
        val currBlock = BlockVertex(operatorItem.id, order++, 1, operatorItem.type, currentLineNo)
        val prevBlock = vertexStack.pop().first as BlockVertex
        hook.createFreeBlock(currBlock)
        hook.joinBlocks(prevBlock.order, currBlock.order)

        // TODO: Assume all operations that aren't automatically evaluated by compiler are binary
        val noOperands = 2
        for (i in 0 until noOperands) {
            val stackItem = operandStack.pop()
            logger.debug("Next operand: $stackItem")
            when (stackItem) {
                is OperatorItem -> {
                    vertexStack.push(Pair(currBlock, pseudoLineNo))
                    handleOperator(stackItem)
                }
                is ConstantItem -> {
                    val literalVertex = LiteralVertex(stackItem.id, order++, 1, stackItem.type, currentLineNo)
                    hook.assignToBlock(literalVertex, currBlock.order)
                }
                is VariableItem -> {
                    val localVertex = LocalVertex(stackItem.id, stackItem.id, stackItem.type, currentLineNo, order++)
                    hook.assignToBlock(localVertex, currBlock.order)
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

    override fun clear(): ASTController {
        super.clear()
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

}
