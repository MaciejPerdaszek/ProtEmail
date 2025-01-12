package com.example.api.exception;

public class SafeBrowsingApiException extends RuntimeException {

    public SafeBrowsingApiException(Throwable cause) {
        super(cause);
    }

    public SafeBrowsingApiException(String message) {
        super(message);
    }

    public SafeBrowsingApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
