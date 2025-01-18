package com.example.api.exception;

public class EmailProcessingException extends RuntimeException {

    public EmailProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailProcessingException(String message) {
        super(message);
    }

    public EmailProcessingException(Throwable cause) {
        super(cause);
    }


}
