package za.ac.sun.grapl.domain.stack;

import java.util.Objects;

public abstract class OperandItem {

    public final String id;
    public final String type;

    public OperandItem(String id, String type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperandItem that = (OperandItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
