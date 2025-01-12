package com.example.api.exception;

public class AiModelException extends RuntimeException {

    public AiModelException(Throwable cause) {
        super(cause);
    }

    public AiModelException(String message) {
        super(message);
    }

    public AiModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
