package org.qbicc.runtime.methoddata;

import org.qbicc.runtime.CNative;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

public final class MethodData {

    @internal
    public static final class method_info extends object {
        public uint8_t_ptr fileName;
        public uint8_t_ptr className;
        public uint8_t_ptr methodName;
        public uint8_t_ptr methodDesc;
    }

    public static final class method_info_ptr extends ptr<method_info> {}
    public static final class const_method_info_ptr extends ptr<@c_const uint32_t> {}
    public static final class method_info_ptr_ptr extends ptr<method_info_ptr> {}
    public static final class const_method_info_ptr_ptr extends ptr<const_method_info_ptr> {}
    public static final class method_info_ptr_const_ptr extends ptr<@c_const method_info_ptr> {}
    public static final class const_method_info_ptr_const_ptr extends ptr<@c_const const_method_info_ptr> {}

    @extern
    public static method_info[] qbicc_method_info_table;

/*    public static int getFileNameIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].fileNameIndex.intValue();
    }

    public static int getClassNameIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].classNameIndex.intValue();
    }

    public static int getMethodNameIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].methodNameIndex.intValue();
    }

    public static int getMethodDescIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].methodDescIndex.intValue();
    }*/

/*    public static method_info getMethodInfo(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex];
    }*/

    @internal
    public static final class source_code_info extends object {
        public uint32_t minfo_index;
        public uint32_t line_number;
        public uint32_t bc_index;
        public uint32_t inlined_at_index;
    }

    public static final class source_code_info_ptr extends ptr<source_code_info> {}
    public static final class const_source_code_info_ptr extends ptr<@c_const uint32_t> {}
    public static final class source_code_info_ptr_ptr extends ptr<source_code_info_ptr> {}
    public static final class const_source_code_info_ptr_ptr extends ptr<const_source_code_info_ptr> {}
    public static final class source_code_info_ptr_const_ptr extends ptr<@c_const source_code_info_ptr> {}
    public static final class const_source_code_info_ptr_const_ptr extends ptr<@c_const const_source_code_info_ptr> {}

    @extern
    public static source_code_info[] qbicc_source_code_info_table;

/*
    public static int getMethodInfoIndex(int scIndex) {
        return qbicc_source_code_info_table[scIndex].minfo_index.intValue();
    }

    public static int getLineNumber(int scIndex) {
        return qbicc_source_code_info_table[scIndex].line_number.intValue();
    }
*/


    /*public static source_code_info getSourceCodeInfo(int scIndex) {
        return qbicc_source_code_info_table[scIndex];
    }*/

    @extern
    public static uint64_t[] qbicc_instruction_list;

    @extern
    public static int qbicc_instruction_list_size;

/*    public static uint64_t getInstructionAddress(int index) {
        return qbicc_instruction_list[index];
    }*/

    /*public static int getInstructionListSize() {
        return qbicc_instruction_list_size;
    }*/

    public static native String getFileName(int minfoIndex);
    public static native String getClassName(int minfoIndex);
    public static native String getMethodName(int minfoIndex);

    public static native int getMethodInfoIndex(int scIndex);
    public static native int getLineNumber(int scIndex);
    public static native int getInlinedAtIndex(int scIndex);

    public static native int getSourceCodeInfoIndex(int index);
    public static native long getInstructionAddress(int index);
    public static native int getInstructionListSize();

    private static native void fillStackTraceElement(StackTraceElement element, int scIndex);

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

    public static void fillStackTraceElements(StackTraceElement[] steArray, Object backtrace, int depth) {
        int sourceCodeIndexList[] = (int[]) backtrace;
        for (int i = 0; i < depth; i++) {
            int scIndex = sourceCodeIndexList[i];
            int minfoIndex = getMethodInfoIndex(scIndex);
            String className = getClassName(minfoIndex);
            String fileName = getFileName(minfoIndex);
            String methodName = getMethodName(minfoIndex);
            printString(className + "#" + methodName + "(" + fileName + ")");
            fillStackTraceElement(steArray[i], sourceCodeIndexList[i]);
        }
    }


    @extern
    public static uint32_t[] qbicc_source_code_index_list;

/*    public static uint32_t getSourceCodeInfoIndex(int index) {
        return qbicc_source_code_index_list[index];
    }*/


/*    @extern
    public static uint32_t_ptr table;*/

/*    @export
    public static int testArray(int index, int value) {
        return table.get(index).intValue();
    }*/

/*    @export
    public static int testArray(int index, int value) {
        uint32_t[] tableArray = table.asArray();
        return tableArray[index].intValue();
    }*/

/*    @extern
    public static native int putchar(int arg);

    @export
    public static long getClassName(int index) {
        int length = qbicc_classname_table[index].length.intValue();
        uint8_t_ptr className = qbicc_classname_table[index].data;

        for (int i = 0; i < length; i++) {
            putchar(className.get(i).intValue());
        }

        long address = qbicc_instruction_map_table[index].function_addr.longValue();
        return address;
    }*/

}

