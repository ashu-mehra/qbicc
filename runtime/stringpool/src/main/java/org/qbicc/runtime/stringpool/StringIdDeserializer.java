package org.qbicc.runtime.stringpool;

import org.qbicc.runtime.stdc.Stdint;

public class StringIdDeserializer {
    public static StringId deserialize(Stdint.uint32_t id) {
        return new OffsetBasedStringPool.OffsetBasedStringId(id.intValue());
    }
}
