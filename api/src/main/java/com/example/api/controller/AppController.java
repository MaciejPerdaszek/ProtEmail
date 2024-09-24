package com.example.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppController {

    @GetMapping("/")
    public String home() {
        return "Welcome to public endpoint!";
    }

    @GetMapping("/private")
    public String privateEndpoint(Authentication authentication) {
        // Zwróć nazwę użytkownika z tokenu JWT
        return "Zalogowany użytkownik: " + authentication.getName();
    }

//    @GetMapping("/private")
//    public String privateArea() {
//        return "Welcome to private endpoint";
//    }
}
