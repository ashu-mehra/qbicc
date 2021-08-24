package org.qbicc.runtime.stringpool;

import static org.qbicc.runtime.stdc.Stdint.*;

public class StringPoolAccessor {
    public static String getString(uint32_t serializedId) {
        StringId id = StringIdDeserializer.deserialize(serializedId);
        return StringPool.getString(id);
    }
}
