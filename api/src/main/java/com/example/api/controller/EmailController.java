package com.example.api.controller;

import java.util.Map;
import com.example.api.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/connection-states")
    public ResponseEntity<Map<String, Boolean>> getConnectionStates() {
        Map<String, Boolean> states = emailService.getMailboxConnectionStates();
        return ResponseEntity.ok(states);
    }
}
