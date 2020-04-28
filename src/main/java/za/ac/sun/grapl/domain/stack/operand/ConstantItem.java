package za.ac.sun.grapl.domain.stack.operand;

import za.ac.sun.grapl.domain.stack.OperandItem;

public final class ConstantItem extends OperandItem {

    public ConstantItem(final String id, final String type) {
        super(id, type);
    }

    @Override
    public String toString() {
        return "CONST {value: " + id + ", type: " + type + "}";
    }

}
