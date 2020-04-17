package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.enums.JumpState;

public class GotoBlock extends JumpBlock {

    public GotoBlock(int order, Label label, Label destination, JumpState position) {
        super(order, label, destination, position);
    }

    @Override
    public String toString() {
        return "GOTO {order: " + order + ", position: " + position.name() + ", destination: " + destination + ", label: " + label + "}";
    }
}
