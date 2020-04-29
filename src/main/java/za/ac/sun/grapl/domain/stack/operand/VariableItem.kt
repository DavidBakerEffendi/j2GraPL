package za.ac.sun.grapl.domain.stack.operand;

import za.ac.sun.grapl.domain.stack.OperandItem;

public final class VariableItem extends OperandItem {

    public VariableItem(final String id, final String type) {
        super(id, type);
    }

    @Override
    public String toString() {
        return "VAR {id: " + id + ", type: " + type + "}";
    }

}
