package org.qbicc.runtime.gc.nogc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Stdlib.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;

/**
 *
 */
public final class NoGcHelpers {
    private NoGcHelpers() {}

    @Hidden
    public static Object allocate(long size, int align) {
        if (false && Build.Target.isPosix()) {
            void_ptr ptr = auto();
            c_int res = posix_memalign(addr_of(ptr), word((long)align), word(size));
            if (res.intValue() != 0) {
                // todo: read errno
                throw new OutOfMemoryError(/*"Allocation failed"*/);
            }
            return ptr;
        } else {
            char_ptr ptr = malloc(word(size + align));
            if (ptr.isNull()) {
                throw new OutOfMemoryError(/*"Allocation failed"*/);
            }
            long mask = align - 1;
            long misAlign = ptr.longValue() & mask;
            if (misAlign != 0) {
                ptrdiff_t word = word(((~ misAlign) & mask) + 1);
                ptr = ptr.plus(word);
            }
            return ptrToRef(ptr);
        }
    }

    @Hidden
    public static void clear(Object ptr, long size) { memset(refToPtr(ptr), word(0), word(size)); }

    @Hidden
    public static void copy(Object to, Object from, long size) {
        memcpy(refToPtr(to), refToPtr(from), word(size));
    }
}
