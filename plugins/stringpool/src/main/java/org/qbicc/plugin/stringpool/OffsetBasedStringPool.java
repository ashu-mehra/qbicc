package org.qbicc.plugin.stringpool;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.object.Data;
import org.qbicc.object.Section;
import org.qbicc.type.TypeSystem;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

/**
 * This StringPool implementation stores strings in a single array with each string separated by a null character.
 * First character is always null character and is used for empty strings.
 * {@link StringId} is just an offset of the string in the array.
 * @see OffsetBasedStringId
 */
public class OffsetBasedStringPool implements StringPool {
    private final HashMap<String, StringId> stringToIdMap = new LinkedHashMap<>();
    private final HashMap<StringId, String> idToStringMap = new HashMap<>();
    private int nextOffset;

    OffsetBasedStringPool() {
        nextOffset = 1; // 0 is for null character to indicate empty pool
    }

    public StringId add(String str) {
        if (str == null) {
            return OffsetBasedStringId.NULL_STRING_ID;
        } else if (str.equals("")) {
            return OffsetBasedStringId.EMPTY_STRING_ID;
        }
        StringId id;
        synchronized (this) {
            id = stringToIdMap.computeIfAbsent(str, k -> {
                StringId newId = new OffsetBasedStringId(nextOffset);
                int numChars = str.toCharArray().length + 1;
                nextOffset += numChars;
                //int numBytes = str.getBytes(StandardCharsets.UTF_16).length + 1; // +1 for null character
                //nextOffset += numBytes;
                return newId;
            });
        }
        idToStringMap.putIfAbsent(id, str);
        return id;
    }

    @Override
    public String findString(StringId id) {
        if (id == OffsetBasedStringId.NULL_STRING_ID) {
            return null;
        } else if (id == OffsetBasedStringId.EMPTY_STRING_ID) {
            return "";
        } else {
            return idToStringMap.get(id);
        }
    }


    public Data emit(CompilationContext context) {
        //byte[] pool = new byte[nextOffset];
        char[] pool = new char[nextOffset];
        pool[0] = 0;
        int cursor = 1;
        for (String str: stringToIdMap.keySet()) {
            //byte[] chars = str.getBytes(StandardCharsets.UTF_16);
            char[] chars = str.toCharArray();
            assert cursor == ((OffsetBasedStringId) stringToIdMap.get(str)).offset;
            System.arraycopy(chars, 0, pool, cursor, chars.length);
            cursor += chars.length;
            pool[cursor] = '\0'; // append null at the end of string
            cursor += 1;
        }

        TypeSystem ts = context.getTypeSystem();
        LiteralFactory lf = context.getLiteralFactory();
        //Literal literal = lf.literalOf(ts.getArrayType(ts.getUnsignedInteger8Type(), pool.length), pool);
        ArrayList<Literal> charLiterals = new ArrayList<>();
        for (char ch: pool) {
            charLiterals.add(lf.literalOf(ch));
        }
        Literal literal = lf.literalOf(ts.getArrayType(ts.getUnsignedInteger16Type(), pool.length), charLiterals);
        Section section = context.getImplicitSection(context.getDefaultTypeDefinition());
        return section.addData(null, "qbicc_string_pool", literal);
    }

    @Override
    public int count() {
        return stringToIdMap.size();
    }

    @Override
    public int size() {
        return nextOffset;
    }

    @Override
    public void displayStats() {
        slog.debug("String pool stats");
        slog.debug("-----------------");
        slog.debugf("Number of strings: %d", count());
        slog.debugf("Pool size in bytes: %d", size());
    }

    private static final class OffsetBasedStringId implements StringId {
        private static final StringId EMPTY_STRING_ID = new OffsetBasedStringId(0);
        private static final StringId NULL_STRING_ID = new OffsetBasedStringId(-1);
        int offset;

        OffsetBasedStringId(int offset) {
            this.offset = offset;
        }

        @Override
        public Literal serialize(CompilationContext context) {
            return context.getLiteralFactory().literalOf(offset);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OffsetBasedStringId that = (OffsetBasedStringId) o;
            return offset == that.offset;
        }

        @Override
        public int hashCode() {
            return offset;
        }
    }
}
