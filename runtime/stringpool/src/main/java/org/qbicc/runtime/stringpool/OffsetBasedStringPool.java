package org.qbicc.runtime.stringpool;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.stdc.Stdint;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OffsetBasedStringPool {
    @CNative.extern
    static Stdint.uint8_t[] qbicc_string_pool;

    private static Map<StringId, String> stringMap = new HashMap<>(); // should use ConcurrentHashMap

    public static String getString(StringId stringId) {
        if (stringMap.containsKey(stringId)) {
            return stringMap.get(stringId);
        }
        if (stringId.equals(OffsetBasedStringId.NULL_STRING_ID)) {
            return null;
        } else if (stringId == OffsetBasedStringId.EMPTY_STRING_ID) {
            return "";
        } else {
            OffsetBasedStringId id = (OffsetBasedStringId)stringId;
            int offset = id.getOffset();

            byte ch;
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            while ((ch = qbicc_string_pool[offset].byteValue()) != 0) {
                stream.write(ch);
                offset += 1;
            }
            return stringMap.put(stringId, new String(stream.toByteArray(), StandardCharsets.UTF_16));
        }
    }

    static class OffsetBasedStringId extends StringId {
        private static final StringId EMPTY_STRING_ID = new OffsetBasedStringId(0);
        private static final StringId NULL_STRING_ID = new OffsetBasedStringId(-1);
        int offset;

        OffsetBasedStringId(int id) {
            this.offset = id;
        }

        int getOffset() {
            return offset;
        }
    }
}
