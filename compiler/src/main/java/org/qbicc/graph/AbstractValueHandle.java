package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractValueHandle extends AbstractNode implements ValueHandle {
    AbstractValueHandle(final Node callSite, final ExecutableElement element, final int line, final int bci) {
        super(callSite, element, line, bci);
    }


}
