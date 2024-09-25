package com.example.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public String privateEndpoint(@AuthenticationPrincipal OAuth2User user) {
        return "Zalogowany użytkownik: " + user.getAttribute("name");
    }
}
