package com.example.api.exception;

public class UrlScanApiException extends RuntimeException {

    public UrlScanApiException(Throwable cause) {
        super(cause);
    }

    public UrlScanApiException(String message) {
        super(message);
    }

    public UrlScanApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
