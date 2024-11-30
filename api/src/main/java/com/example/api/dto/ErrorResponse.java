package com.example.api.dto;

public record ErrorResponse(String message, String errorCode, int status) { }