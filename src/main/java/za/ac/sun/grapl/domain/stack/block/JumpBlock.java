package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.enums.JumpState;
import za.ac.sun.grapl.domain.stack.BlockItem;

public abstract class JumpBlock extends BlockItem {
    public final Label destination;
    public final JumpState position;

    public JumpBlock(int order, Label label, Label destination, JumpState position) {
        super(order, label);
        this.destination = destination;
        this.position = position;
    }
}
