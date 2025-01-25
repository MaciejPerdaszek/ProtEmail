package com.example.api.dto;

public record DisconnectRequest(
        String email,
        String userId
) {}