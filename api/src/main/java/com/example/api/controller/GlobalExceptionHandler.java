package com.example.api.controller;

import com.example.api.dto.ErrorResponse;
import com.example.api.exception.AuthException;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.exception.InvalidMailboxException;
import com.example.api.exception.MailboxAlreadyExistsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage(), "AUTH_ERROR", 401);
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(EmailsFetchingException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleEmailsFetchingException(EmailsFetchingException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage(), "EMAILS_FETCHING_ERROR", 500);
        return ResponseEntity.status(500).body(response);
    }

    @ExceptionHandler(InvalidMailboxException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleInvalidMailboxException(InvalidMailboxException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage(), "INVALID_MAILBOX_ERROR", 400);
        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(MailboxAlreadyExistsException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleMailboxAlreadyExistsException(MailboxAlreadyExistsException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage(), "MAILBOX_ALREADY_EXISTS_ERROR", 409);
        return ResponseEntity.status(409).body(response);
    }
}
