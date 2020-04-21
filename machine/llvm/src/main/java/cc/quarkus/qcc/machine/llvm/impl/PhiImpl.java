package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import cc.quarkus.qcc.machine.llvm.BasicBlock;
import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.Phi;

final class PhiImpl extends AbstractYieldingInstruction implements Phi {
    private final AbstractValue type;
    private Set<FastMathFlag> mathFlags = Collections.emptySet();
    private Item lastItem;

    PhiImpl(final BasicBlockImpl block, final AbstractValue type) {
        super(block);
        this.type = type;
    }

    public Phi meta(final String name, final Value data) {
        super.meta(name, data);
        return this;
    }

    public Phi comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Phi item(final Value data, final BasicBlock incoming) {
        lastItem = new Item(lastItem, (AbstractValue) data, (BasicBlockImpl) incoming);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("phi");
        final Set<FastMathFlag> mathFlags = this.mathFlags;
        for (FastMathFlag mathFlag : mathFlags) {
            target.append(' ').append(mathFlag.name());
        }
        target.append(' ');
        type.appendTo(target);
        final Item lastItem = this.lastItem;
        if (lastItem != null) {
            target.append(' ');
            lastItem.appendTo(target);
        }
        return appendTrailer(target);
    }

    static final class Item extends AbstractEmittable {
        private final Item prev;
        private final AbstractValue data;
        private final BasicBlockImpl incoming;

        Item(final Item prev, final AbstractValue data, final BasicBlockImpl incoming) {
            this.prev = prev;
            this.incoming = incoming;
            this.data = data;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            final Item prev = this.prev;
            if (prev != null) {
                prev.appendTo(target);
                target.append(',').append(' ');
            }
            target.append('[').append(' ');
            data.appendTo(target);
            target.append(',').append(' ');
            incoming.appendTo(target);
            return target.append(' ').append(']');
        }
    }
}
