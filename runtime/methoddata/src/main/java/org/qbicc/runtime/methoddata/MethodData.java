package org.qbicc.runtime.methoddata;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

public final class MethodData {
    @internal
    public static final class method_info extends object {
        public uint32_t fileNameIndex;
        public uint32_t classNameIndex;
        public uint32_t methodNameIndex;
        public uint32_t methodDescIndex;
    }

    public static final class method_info_ptr extends ptr<method_info> {}
    public static final class const_method_info_ptr extends ptr<@c_const uint32_t> {}
    public static final class method_info_ptr_ptr extends ptr<method_info_ptr> {}
    public static final class const_method_info_ptr_ptr extends ptr<const_method_info_ptr> {}
    public static final class method_info_ptr_const_ptr extends ptr<@c_const method_info_ptr> {}
    public static final class const_method_info_ptr_const_ptr extends ptr<@c_const const_method_info_ptr> {}

    @extern
    public static method_info[] qbicc_method_info_table;

    public static method_info getMethodInfo(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex];
    }

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

    public static source_code_info getSourceCodeInfo(int scIndex) {
        return qbicc_source_code_info_table[scIndex];
    }

    @extern
    public static uint64_t[] qbicc_instruction_list;

    public static uint64_t getInstructionAddress(int index) {
        return qbicc_instruction_list[index];
    }

    public static int getInstructionListSize() {
        return qbicc_instruction_list.length;
    }

    @extern
    public static uint32_t[] qbicc_source_code_index_list;

    public static uint32_t getSourceCodeInfoIndex(int index) {
        return qbicc_source_code_index_list[index];
    }

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

