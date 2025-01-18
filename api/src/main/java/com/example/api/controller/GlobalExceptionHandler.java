package com.example.api.controller;

import com.example.api.dto.ErrorResponse;
import com.example.api.exception.AuthException;
import com.example.api.exception.EmailsFetchingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ErrorResponse handleAuthException(AuthException ex) {
        return new ErrorResponse(ex.getMessage(), "AUTH_ERROR", 401);
    }

    @ExceptionHandler(EmailsFetchingException.class)
    public ErrorResponse handleEmailsFetchingException(EmailsFetchingException ex) {
        return new ErrorResponse(ex.getMessage(), "EMAILS_FETCHING_ERROR", 500);
    }
}
