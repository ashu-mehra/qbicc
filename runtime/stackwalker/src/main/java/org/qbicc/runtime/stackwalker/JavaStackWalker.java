package org.qbicc.runtime.stackwalker;

import org.qbicc.runtime.methoddata.MethodData;

public class JavaStackWalker implements StackFrameVisitor {
    private JavaStackFrameVisitor visitor;
    private int javaFrameCount;

    private JavaStackWalker(JavaStackFrameVisitor visitor) {
        this.visitor = visitor;
        javaFrameCount = 0;
    }

    public static int getFrameCount() {
        JavaStackWalker javaStackWalker = new JavaStackWalker(new NopVisitor());
        StackWalker.walkStack(javaStackWalker);
        return javaStackWalker.javaFrameCount;
    }

    public static void walkStack(JavaStackFrameVisitor visitor) {
        StackFrameVisitor javaStackWalker = new JavaStackWalker(visitor);
        StackWalker.walkStack(javaStackWalker);
    }

    private int findInstructionInMethodData(long ip) {
        // do a binary search in instruction table
        int upper = MethodData.getInstructionListSize();
        int lower = 0;
        while (upper >= lower) {
            int mid = ( upper + lower ) >>> 1;
            long addr = MethodData.getInstructionAddress(mid);
            if (ip == addr) {
                return mid;
            } else if (ip > addr) {
                lower = mid+1;
            } else {
                upper = mid-1;
            }
        }
        return -1;
    }

    public void visitFrame(int frameIndex, long ip, long sp) {
        int index = findInstructionInMethodData(ip);
        if (index != -1) {
            int scIndex = MethodData.getSourceCodeInfoIndex(index);
            visitor.visitFrame(javaFrameCount, scIndex);
            javaFrameCount += 1;
            int inlinedAtIndex = MethodData.getInlinedAtIndex(scIndex);
            while (inlinedAtIndex != -1) {
                visitor.visitFrame(javaFrameCount, inlinedAtIndex);
                javaFrameCount += 1;
                inlinedAtIndex = MethodData.getInlinedAtIndex(inlinedAtIndex);
            }
        } else {
            // skip this frame; probably a native frame
        }
    }

    private static class NopVisitor implements JavaStackFrameVisitor {
        @Override
        public void visitFrame(int frameIndex, int scIndex) { /* no-op */ }
    }
}
