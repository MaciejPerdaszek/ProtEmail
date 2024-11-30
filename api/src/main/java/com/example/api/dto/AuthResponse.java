package com.example.api.dto;

import java.util.Optional;

public record AuthResponse(boolean success, String message, Optional<Boolean> requireRelogin) {
}
