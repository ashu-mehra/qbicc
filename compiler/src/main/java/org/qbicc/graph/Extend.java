package org.qbicc.graph;

import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Extend extends AbstractWordCastValue {
    Extend(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value value, final WordType toType) {
        super(callSite, element, line, bci, value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Extend";
    }
}
