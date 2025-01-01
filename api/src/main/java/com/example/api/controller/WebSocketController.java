package com.example.api.controller;

import com.example.api.dto.EmailConfigRequest;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.service.EmailService;
import com.example.api.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/api")
public class WebSocketController {

    public record WebSocketResponse(
            String error,
            String message
    ) {}

    private final WebSocketService webSocketService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(WebSocketService webSocketService, EmailService emailService, SimpMessagingTemplate messagingTemplate) {
        this.webSocketService = webSocketService;
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/connect")
    public void connect(@Payload EmailConfigRequest config, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket connection request received for email: {}", config.username());

        try {
            webSocketService.connect(config.username(), sessionId);
            // Najpierw próbujemy połączyć się z IMAP
            emailService.startMonitoring(config);

            log.info("111 Mailbox monitoring started for email: {}", config.username());
            // Jeśli się udało, wysyłamy sukces
            messagingTemplate.convertAndSendToUser(
                    config.username(),
                    "/topic/response",
                    new WebSocketResponse("SUCCESS", "Connected successfully")
            );
            log.info("Connected to mailbox: {}", config.username());
        } catch (EmailsFetchingException e) {
            log.error("Failed to connect to mailbox: {}", config.username(), e);
            // Rozłączamy WebSocket w przypadku błędu
            emailService.stopMailboxMonitoring(config.username());
            webSocketService.disconnect(config.username());
            messagingTemplate.convertAndSendToUser(
                    config.username(),
                    "/topic/response",
                    new WebSocketResponse("ERROR", "Failed to connect: " + e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error during connection: {}", config.username(), e);
            emailService.stopMailboxMonitoring(config.username());
            webSocketService.disconnect(config.username());
            messagingTemplate.convertAndSendToUser(
                    config.username(),
                    "/topic/response",
                    new WebSocketResponse("ERROR", "Connection failed - please check your credentials")
            );
        }
    }

    @MessageMapping("/disconnect")
    public void disconnect(@Payload String email) {
        log.info("WebSocket disconnection request received for email: {}", email);
        emailService.stopMailboxMonitoring(email);
        webSocketService.disconnect(email);
    }

    @MessageMapping("/send-message")
    @SendToUser("/topic/messages")
    public String sendMessage(String email, String message) {
        webSocketService.sendMessage(email, message);
        return "Message sent to " + email;
    }
}