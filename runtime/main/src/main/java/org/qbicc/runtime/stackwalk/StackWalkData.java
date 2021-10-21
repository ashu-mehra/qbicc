package org.qbicc.runtime.stackwalk;

public class StackWalkData {
    private int nativeFrameCount;
    private int javaFrameCount;
    private int instructionIndexList[];
    private int instructionCount;

    public int getNativeFrameCount() {
        return nativeFrameCount;
    }

    public void setNativeFrameCount(int nativeFrameCount) {
        this.nativeFrameCount = nativeFrameCount;
    }

    public int getJavaFrameCount() {
        return javaFrameCount;
    }

    public void setJavaFrameCount(int javaFrameCount) {
        this.javaFrameCount = javaFrameCount;
    }

    public int[] getInstructionIndexList() {
        return instructionIndexList;
    }

    public void setInstructionIndexList(int[] instructionIndexList) {
        this.instructionIndexList = instructionIndexList;
    }

    public void initializeInstructionList(int size) {
        instructionIndexList = new int[size];
        instructionCount = 0;
    }

    public void storeInstructionIndex(int index) {
        instructionIndexList[instructionCount] = index;
        instructionCount += 1;
    }

    public void incrementJavaFrameCount(int count) {
        this.javaFrameCount += count;
    }
}
