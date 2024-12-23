package com.example.api.controller;

import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class WebController {

    @MessageMapping("/emails")
    @SendTo("/topic/emails")
    public ResponseEntity<?> sendEmails() {
        return ResponseEntity.ok("Emails sent at " + LocalDateTime.now());
    }
}