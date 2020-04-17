package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.enums.IfCmpPosition;

public class IfCmpBlock extends JumpBlock {

    public IfCmpBlock(int order, Label label, Label destination, IfCmpPosition position) {
        super(order, label, destination, position);
    }

    @Override
    public String toString() {
        return "IFCMP {order: " + order + ", position: " + position.name() + ", destination: " + destination + ", label: " + label + "}";
    }

}
