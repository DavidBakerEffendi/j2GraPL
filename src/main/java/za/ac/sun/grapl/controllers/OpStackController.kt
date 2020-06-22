package za.ac.sun.grapl.controllers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.Label
import org.objectweb.asm.util.ASMifier
import za.ac.sun.grapl.domain.stack.OperandItem
import za.ac.sun.grapl.domain.stack.operand.ConstantItem
import za.ac.sun.grapl.domain.stack.operand.OperatorItem
import za.ac.sun.grapl.domain.stack.operand.VariableItem
import za.ac.sun.grapl.util.ASMParserUtil
import java.util.*
import kotlin.math.absoluteValue

abstract class OpStackController : AbstractController {

    protected val operandStack = Stack<OperandItem?>()
    protected val variables = HashSet<VariableItem>()
    private val logger: Logger = LogManager.getLogger()

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

    fun getOrPutVariable(varName: Int, type: String): VariableItem {
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

    open fun handleOperator(operatorItem: OperatorItem) {
        val noOperands = 2
        for (i in 0 until noOperands) {
            when (val stackItem = operandStack.pop()) {
                is OperatorItem -> {
                    handleOperator(stackItem)
                }
            }
        }
    }

    open fun pushVarInsnStore(varName: Int, operation: String) {
        when (val operandItem = operandStack.pop()) {
            is OperatorItem -> {
                handleOperator(operandItem)
            }
        }
    }

    open fun pushBinaryJump(jumpOp: String, label: Label) {
        listOfNotNull(operandStack.pop(), operandStack.pop()).asReversed()
    }

    open fun pushUnaryJump(jumpOp: String, label: Label) {
        operandStack.pop()
    }

    override fun toString(): String {
        return """
            ${this.javaClass.canonicalName}
            Stack: $operandStack
            Variables: $variables
            """.trimIndent()
    }

    open fun clear(): AbstractController {
        operandStack.clear()
        variables.clear()
        return this
    }
}