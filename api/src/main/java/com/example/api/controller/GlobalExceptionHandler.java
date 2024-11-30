package com.example.api.controller;

import com.example.api.dto.ErrorResponse;
import com.example.api.exception.AuthException;
import com.example.api.exception.UserAlreadyExistException;
import com.example.api.exception.UserNotFound;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistException.class)
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
}
