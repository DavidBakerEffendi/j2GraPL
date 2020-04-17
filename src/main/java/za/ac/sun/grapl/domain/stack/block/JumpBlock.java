package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.enums.IfCmpPosition;
import za.ac.sun.grapl.domain.stack.BlockItem;

public abstract class JumpBlock extends BlockItem {
    public final Label destination;
    public final IfCmpPosition position;

    public JumpBlock(int order, Label label, Label destination, IfCmpPosition position) {
        super(order, label);
        this.destination = destination;
        this.position = position;
    }
}
