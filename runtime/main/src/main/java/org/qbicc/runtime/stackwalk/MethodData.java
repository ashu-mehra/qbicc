package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.CNative;

public final class MethodData {

    public static native String getFileName(int minfoIndex);
    public static native String getClassName(int minfoIndex);
    public static native String getMethodName(int minfoIndex);
    public static native String getMethodDesc(int minfoIndex);
    public static native int getTypeId(int minfoIndex);

    public static native int getMethodInfoIndex(int scIndex);
    public static native int getLineNumber(int scIndex);
    public static native int getBytecodeIndex(int scIndex);
    public static native int getInlinedAtIndex(int scIndex);

    public static native int getSourceCodeInfoIndex(int index);
    public static native long getInstructionAddress(int index);
    public static native int getInstructionListSize();

    private static native void fillStackTraceElement(StackTraceElement element, int scIndex);

    @CNative.extern
    public static native int putchar(int arg);

    // helper to print a string
    private static void printString(String string) {
        char[] contents = string.toCharArray();
        for (char ch: contents) {
            putchar((byte)ch);
        }
        putchar('\n');
    }

    // helper to print a stack frame info
    private static void printFrame(int scIndex) {
        int minfoIndex = getMethodInfoIndex(scIndex);
        String className = getClassName(minfoIndex);
        String fileName = getFileName(minfoIndex);
        String methodName = getMethodName(minfoIndex);
        printString(className + "#" + methodName + "(" + fileName + ")");
    }

    public static void fillStackTraceElements(StackTraceElement[] steArray, Object backtrace, int depth) {
        int[] sourceCodeIndexList = (int[]) backtrace;
        for (int i = 0; i < depth; i++) {
            //printFrame(sourceCodeIndexList[i]);
            fillStackTraceElement(steArray[i], sourceCodeIndexList[i]);
        }
    }
}

