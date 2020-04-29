package za.ac.sun.grapl.domain.stack.operand;

import za.ac.sun.grapl.domain.stack.OperandItem;

public final class OperatorItem extends OperandItem {

    public OperatorItem(final String id, final String type) {
        super(id, type);
    }

    @Override
    public String toString() {
        return "OPERATOR {value: " + id + ", type: " + type + "}";
    }

}
