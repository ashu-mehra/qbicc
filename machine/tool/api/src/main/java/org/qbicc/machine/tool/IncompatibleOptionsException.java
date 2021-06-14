package org.qbicc.machine.tool;

public class IncompatibleOptionsException extends Exception {
    private static final long serialVersionUID = 1091309040758491967L;

    /**
     * Constructs a new {@code IncompatibleOptionsException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public IncompatibleOptionsException() {
    }

    /**
     * Constructs a new {@code IncompatibleOptionsException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public IncompatibleOptionsException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code IncompatibleOptionsException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code IncompatibleOptionsException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public IncompatibleOptionsException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code IncompatibleOptionsException} instance with an initial message and cause.
     *
     * @param msg   the message
     * @param cause the cause
     */
    public IncompatibleOptionsException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
