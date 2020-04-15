package za.ac.sun.grapl.domain.stack.operand;

import za.ac.sun.grapl.domain.stack.OperandItem;

public class ConstantItem extends OperandItem {

    public ConstantItem(String id, String type) {
        super(id, type);
    }

    @Override
    public String toString() {
        return "CONST {value: " + id + ", type: " + type + "}";
    }

}
