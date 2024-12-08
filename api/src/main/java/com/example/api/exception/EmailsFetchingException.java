package com.example.api.exception;

public class EmailsFetchingException extends RuntimeException {

    public EmailsFetchingException(Throwable cause) {
        super(cause);
    }

    public EmailsFetchingException(String message) {
        super(message);
    }

    public EmailsFetchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
