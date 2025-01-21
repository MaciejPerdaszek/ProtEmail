package com.example.api.exception;

public class MailboxAlreadyExistsException extends  RuntimeException {

    public MailboxAlreadyExistsException(String message) {
        super(message);
    }

    public MailboxAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public MailboxAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
