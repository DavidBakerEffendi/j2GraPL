package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.enums.JumpState;
import za.ac.sun.grapl.domain.stack.BlockItem;

public class NestedBodyBlock extends BlockItem {

    public final JumpState position;

    public NestedBodyBlock(int order, Label label, JumpState position) {
        super(order, label);
        this.position = position;
    }

    public NestedBodyBlock setLabel(Label label) {
        return new NestedBodyBlock(this.order, label, this.position);
    }

    @Override
    public String toString() {
        return "NESTED BODY {order: " + order + ", position: " + position.name() + ", label: " + label + "}";
    }
}
