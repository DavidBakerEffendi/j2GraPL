package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.enums.JumpState;

public final class IfCmpBlock extends JumpBlock {

    public IfCmpBlock(final int order, final Label label, final Label destination, final JumpState position) {
        super(order, label, destination, position);
    }

    @Override
    public String toString() {
        return "IFCMP {order: " + order + ", position: " + position.name() + ", destination: " + destination + ", label: " + label + "}";
    }

}
