package com.example.api.controller;

import com.example.api.dto.ErrorResponse;
import com.example.api.exception.AuthException;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.exception.UserAlreadyExistException;
import com.example.api.exception.UserNotFound;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUserAlreadyExistException(UserAlreadyExistException ex) {
        return new ErrorResponse(ex.getMessage(), "USER_ALREADY_EXISTS", 400);
    }

    @ExceptionHandler(UserNotFound.class)
    public ErrorResponse handleUserNotFoundException(UserNotFound ex) {
        return new ErrorResponse(ex.getMessage(), "USER_NOT_FOUND", 404);
    }

    @ExceptionHandler(AuthException.class)
    public ErrorResponse handleAuthException(AuthException ex) {
        return new ErrorResponse(ex.getMessage(), "AUTH_ERROR", 401);
    }

    @ExceptionHandler(EmailsFetchingException.class)
    public ErrorResponse handleEmailsFetchingException(EmailsFetchingException ex) {
        return new ErrorResponse(ex.getMessage(), "EMAILS_FETCHING_ERROR", 500);
    }
}
