package za.ac.sun.grapl.domain.stack.block

import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.enums.JumpState
import za.ac.sun.grapl.domain.stack.BlockItem

abstract class JumpBlock(order: Int, label: Label?, val destination: Label, val position: JumpState) : BlockItem(order, label)