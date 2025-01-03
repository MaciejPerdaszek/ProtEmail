package com.example.api.controller;

import com.example.api.dto.EmailConfigRequest;
import com.example.api.dto.WebSocketResponse;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.service.MailboxConnectionService;
import com.example.api.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final MailboxConnectionService mailboxConnectionService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(WebSocketService webSocketService, MailboxConnectionService mailboxConnectionService, SimpMessagingTemplate messagingTemplate) {
        this.webSocketService = webSocketService;
        this.mailboxConnectionService = mailboxConnectionService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/connect")
    public void connect(@Payload EmailConfigRequest config, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket connection request received for email: {}", config.username());

        try {
            webSocketService.connect(config.username(), sessionId);
            mailboxConnectionService.startMonitoring(config);
            messagingTemplate.convertAndSend(
                    "/topic/connect/" + config.username(),
                    new WebSocketResponse("SUCCESS")
            );

            log.info("Connected to mailbox: {}", config.username());

        } catch (EmailsFetchingException e) {

            webSocketService.disconnect(config.username());
            mailboxConnectionService.stopMailboxMonitoring(config.username());
            messagingTemplate.convertAndSend(
                    "/topic/connect/" + config.username(),
                    new WebSocketResponse("ERROR")
            );
            log.error("Failed to connect to mailbox: {}", config.username(), e);
        } catch (Exception e) {
            webSocketService.disconnect(config.username());
            mailboxConnectionService.stopMailboxMonitoring(config.username());
            messagingTemplate.convertAndSend(
                    "/topic/connect/" + config.username(),
                    new WebSocketResponse("ERROR")
            );
            log.error("Unexpected error during connection: {}", config.username(), e);
        }
    }

    @MessageMapping("/disconnect")
    public void disconnect(@Payload String email) {
        webSocketService.disconnect(email);
        mailboxConnectionService.stopMailboxMonitoring(email);
        log.info("WebSocket disconnection request received for email: {}", email);
    }

    @MessageMapping("/send-message")
    @SendToUser("/topic/messages")
    public String sendMessage(String email, String message) {
        webSocketService.sendMessage(email, message);
        return "Message sent to " + email;
    }
}