package org.qbicc.runtime.stackwalker;

@FunctionalInterface
public interface StackFrameVisitor {
    void visitFrame(int frameIndex, long ip, long sp);
}
