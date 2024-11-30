package com.example.api.exception;

public class UserNotFound extends RuntimeException {

    public UserNotFound(Throwable cause) {
        super(cause);
    }

    public UserNotFound(String message) {
        super(message);
    }

    public UserNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}
