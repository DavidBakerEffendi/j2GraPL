package za.ac.sun.grapl.visitors

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.ASMifier
import za.ac.sun.grapl.controllers.OpStackController
import za.ac.sun.grapl.util.ASMParserUtil
import za.ac.sun.grapl.util.ASMParserUtil.isLoad
import za.ac.sun.grapl.util.ASMParserUtil.isStore

open class OpStackMethodVisitor(
        mv: MethodVisitor?,
        private val controller: OpStackController
) : MethodVisitor(Opcodes.ASM5, mv), Opcodes {
    override fun visitCode() {
        super.visitCode()
        controller.initializeMethod()
    }

    override fun visitLabel(label: Label) {
        controller.pushNewLabel(label)
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        controller.pushConstInsnOperation(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        super.visitIntInsn(opcode, operand)
        controller.pushConstInsnOperation(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        super.visitVarInsn(opcode, `var`)
        val operation = ASMifier.OPCODES[opcode]
        if (isLoad(operation)) controller.pushVarInsnLoad(`var`, operation) else if (isStore(operation)) controller.pushVarInsnStore(`var`, operation)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        super.visitJumpInsn(opcode, label)
        val jumpOp = ASMifier.OPCODES[opcode]
        when {
            ASMParserUtil.NULLARY_JUMPS.contains(jumpOp) -> controller.pushNullaryJumps(label)
            ASMParserUtil.UNARY_JUMPS.contains(jumpOp) -> controller.pushUnaryJump(jumpOp, label)
            ASMParserUtil.BINARY_JUMPS.contains(jumpOp) -> controller.pushBinaryJump(jumpOp, label)
        }
    }

    override fun visitLdcInsn(`val`: Any) {
        super.visitLdcInsn(`val`)
        controller.pushConstInsnOperation(`val`)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        super.visitIincInsn(`var`, increment)
        controller.pushVarInc(`var`, increment)
    }

}