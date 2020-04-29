package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.enums.JumpState;
import za.ac.sun.grapl.domain.stack.BlockItem;

public abstract class JumpBlock extends BlockItem {

    public final Label destination;
    public final JumpState position;

    public JumpBlock(final int order, final Label label, final Label destination, final JumpState position) {
        super(order, label);
        this.destination = destination;
        this.position = position;
    }
}
