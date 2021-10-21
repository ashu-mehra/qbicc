package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.CNative;

@FunctionalInterface
public interface JavaStackFrameVisitor {
    void visitFrame(int frameIndex, int scIndex);

/*    // Used in intrinsic for Throwable#fillInStackTrace
    public int getDepth() {
        return depth;
    }

    // Used in intrinsic for Throwable#fillInStackTrace
    public Object getBacktrace() {
        return instructionIndexList;
    }

    //private static native void fillStackTraceElement(StackTraceElement element, int instructionIndexList);

    @CNative.extern
    public static native int putchar(int arg);

    // helper to print stack trace for debugging
    private static void printString(String string) {
        char[] contents = string.toCharArray();
        for (char ch: contents) {
            putchar((byte)ch);
        }
        putchar('\n');
    }

    // Used in intrinsic for StackTraceElement#initStackTraceElements
    public static void fillStackTraceElements(StackTraceElement[] steArray, Object backtrace, int depth) {
        int sourceCodeIndexList[] = (int[]) backtrace;
        for (int i = 0; i < depth; i++) {
*//*            int scIndex = MethodData.getSourceCodeInfoIndex(sourceCodeIndexList[i]);
            int minfoIndex = MethodData.getMethodInfoIndex(scIndex);
            String className = MethodData.getClassName(minfoIndex);
            String fileName = MethodData.getFileName(minfoIndex);
            String methodName = MethodData.getMethodName(minfoIndex);
            printString(className + "#" + methodName + "(" + fileName + ")");*//*
            fillStackTraceElement(steArray[i], sourceCodeIndexList[i]);
        }
    }*/
}
