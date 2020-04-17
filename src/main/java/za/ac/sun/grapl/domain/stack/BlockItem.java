package za.ac.sun.grapl.domain.stack;

import org.objectweb.asm.Label;

public abstract class BlockItem {

    public final int order;
    public final Label label;

    public BlockItem(int order, Label label) {
        this.order = order;
        this.label = label;
    }

}
