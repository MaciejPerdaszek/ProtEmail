package com.example.api.controller;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.service.EmailService;
import com.example.api.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.socket.WebSocketSession;

@Controller
@Slf4j
@RequestMapping("/api")
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final EmailService emailService;

    public WebSocketController(WebSocketService webSocketService, EmailService emailService) {
        this.webSocketService = webSocketService;
        this.emailService = emailService;
    }

    @MessageMapping("/connect")
    public void connect(@Payload EmailConfigRequest config, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket connection request received for email: {}", config.username());

        // Store session information without WebSocketSession
        webSocketService.connect(config.username(), sessionId);
        emailService.startMonitoring(config);
    }

    @MessageMapping("/disconnect")
    public void disconnect(@Payload String email) {
        log.info("WebSocket disconnection request received for email: {}", email);
        webSocketService.disconnect(email);
        emailService.stopMailboxMonitoring(email);
    }

    @MessageMapping("/send-message")
    @SendToUser("/topic/messages")
    public String sendMessage(String email, String message) {
        webSocketService.sendMessage(email, message);
        return "Message sent to " + email;
    }
}