package com.rentaltracker.transport.exception;

/** The input ended (Ctrl+D or a finished script). The menu treats this as "exit". */
public class InputClosedException extends RuntimeException {
    public InputClosedException() {
        super("Input closed");
    }
}
