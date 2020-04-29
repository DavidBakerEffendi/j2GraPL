package za.ac.sun.grapl.util

import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.stack.BlockItem
import za.ac.sun.grapl.domain.stack.block.GotoBlock
import za.ac.sun.grapl.domain.stack.block.IfCmpBlock
import za.ac.sun.grapl.domain.stack.block.JumpBlock
import java.util.*
import java.util.stream.Collectors

class JumpStackUtil {
    companion object {
        fun getLastJump(blockHistory: Stack<BlockItem>): JumpBlock? {
            val listIterator: ListIterator<BlockItem> = blockHistory.listIterator(blockHistory.size)
            while (listIterator.hasPrevious()) {
                val prev = listIterator.previous()
                if (prev is IfCmpBlock || prev is GotoBlock) return prev as JumpBlock
            }
            return null
        }

        fun getAssociatedJumps(allJumps: HashSet<JumpBlock>, destination: Label): List<JumpBlock?> {
            return allJumps.stream()
                    .filter { j: JumpBlock -> j.destination === destination }
                    .collect(Collectors.toList())
        }

        fun getJumpHistory(blockHistory: Stack<BlockItem>): Stack<JumpBlock> {
            val stack = Stack<JumpBlock>()
            blockHistory.stream()
                    .filter { j: BlockItem? -> j is JumpBlock }
                    .forEachOrdered { j: BlockItem -> stack.push(j as JumpBlock) }
            return stack
        }

        fun isJumpDestination(allJumps: HashSet<JumpBlock>, label: Label): Boolean {
            return allJumps.parallelStream()
                    .map { j: JumpBlock -> j.destination === label }
                    .filter { b: Boolean? -> b!! }
                    .findFirst()
                    .orElse(false)
        }
    }
}