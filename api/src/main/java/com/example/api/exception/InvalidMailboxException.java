package com.example.api.exception;

public class InvalidMailboxException extends RuntimeException {

    public InvalidMailboxException(String message) {
        super(message);
    }

    public InvalidMailboxException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidMailboxException(Throwable cause) {
        super(cause);
    }
}
