package org.qbicc.runtime.stringpool;

import static org.qbicc.runtime.CNative.word;
import static org.qbicc.runtime.stdc.Stdint.*;

public class StringPoolAccessor {
    public static String getString(int serializedId) {
        uint32_t id = word(serializedId);
        StringId sid = StringIdDeserializer.deserialize(id);
        return StringPool.getString(sid);
    }
}
