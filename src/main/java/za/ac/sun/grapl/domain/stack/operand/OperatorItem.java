package za.ac.sun.grapl.domain.stack.operand;

import za.ac.sun.grapl.domain.stack.OperandItem;

public class OperatorItem extends OperandItem {

    public OperatorItem(String id, String type) {
        super(id, type);
    }

    @Override
    public String toString() {
        return "OPERATOR {value: " + id + ", type: " + type + "}";
    }

}
