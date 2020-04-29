package za.ac.sun.grapl.domain.stack.block;

import org.objectweb.asm.Label;
import za.ac.sun.grapl.domain.stack.BlockItem;
import za.ac.sun.grapl.domain.stack.OperandItem;

public final class StoreBlock extends BlockItem {

    private OperandItem l;
    private OperandItem r;

    public StoreBlock(int order, Label label) {
        super(order, label);
    }

    public OperandItem getL() {
        return l;
    }

    public void setL(OperandItem l) {
        this.l = l;
    }

    public OperandItem getR() {
        return r;
    }

    public void setR(OperandItem r) {
        this.r = r;
    }

    @Override
    public String toString() {
        return "STORE {order: " + order + ", left: " + l + ", right: " + r + ", label:" + label + "}";
    }

}
