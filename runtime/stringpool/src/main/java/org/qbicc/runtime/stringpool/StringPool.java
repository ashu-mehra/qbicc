package org.qbicc.runtime.stringpool;

class StringPool {
    public static String getString(StringId stringId) {
        // based on type of string pool, call
        return OffsetBasedStringPool.getString(stringId);
    }
}
