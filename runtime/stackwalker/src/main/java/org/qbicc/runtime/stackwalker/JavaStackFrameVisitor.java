package org.qbicc.runtime.stackwalker;

import org.qbicc.runtime.methoddata.MethodData;
import org.qbicc.runtime.stringpool.StringPoolAccessor;

import java.util.ArrayList;

public class JavaStackFrameVisitor implements StackFrameVisitor {
    ArrayList<StackTraceElement> stackTrace;

    JavaStackFrameVisitor() {
        stackTrace = new ArrayList<>();
    }

    private StackTraceElement buildStackTraceElement(int instructionIndex) {
        int scIndex = MethodData.getSourceCodeInfoIndex(instructionIndex).intValue();
        int lineNumber = MethodData.getLineNumber(scIndex);
        int minfoIndex = MethodData.getMethodInfoIndex(scIndex);
        String className = StringPoolAccessor.getString(MethodData.getClassNameIndex(minfoIndex));
        String methodName = StringPoolAccessor.getString(MethodData.getMethodNameIndex(minfoIndex));
        String fileName = StringPoolAccessor.getString(MethodData.getFileNameIndex(minfoIndex));
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }

/*    private StackTraceElement buildStackTraceElement(int instructionIndex) {
        int scIndex = MethodData.getSourceCodeInfoIndex(instructionIndex).intValue();
        MethodData.source_code_info scInfo = MethodData.getSourceCodeInfo(scIndex);
        MethodData.method_info minfo = MethodData.getMethodInfo(scInfo.minfo_index.intValue());
        int lineNumber = scInfo.line_number.intValue();
        String className = StringPoolAccessor.getString(minfo.classNameIndex);
        String methodName = StringPoolAccessor.getString(minfo.methodNameIndex);
        String fileName = StringPoolAccessor.getString(minfo.fileNameIndex);
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }*/

    /*
    private int buildStackTraceElement(int index) {
        // returning a struct from a method fails to compile with llvm when statepoint intrincis are used
        MethodData.source_code_info scInfo = MethodData.getSourceCodeInfo(10);
        return 0;
    }*/

    private int findInstructionInMethodData(long ip) {
        // do a binary search in instruction table
        int upper = MethodData.getInstructionListSize();
        int lower = 0;
        while (upper >= lower) {
            int mid = (upper+lower)/2;
            long addr = MethodData.getInstructionAddress(mid).longValue();
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

    public boolean visitFrame(long ip, long sp) {
        int index = findInstructionInMethodData(ip);
        if (index != -1) {
            StackTraceElement element = buildStackTraceElement(index);
            stackTrace.add(element);
        } else {
            // skip this frame; probably a native frame?
        }
        return true;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace.toArray(new StackTraceElement[0]);
    }
}
