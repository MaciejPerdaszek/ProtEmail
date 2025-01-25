package com.example.api.controller;

import java.security.Principal;
import java.util.Objects;
import com.example.api.dto.DisconnectRequest;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.service.MailboxConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class WebSocketController {

    private final MailboxConnectionService mailboxConnectionService;

    public WebSocketController(MailboxConnectionService mailboxConnectionService) {
        this.mailboxConnectionService = mailboxConnectionService;
    }

    @MessageMapping("/connect")
    public void connect(@Payload EmailConfigRequest config, Principal principal) {

        if (!Objects.equals(principal.getName(), config.userId())) {
            log.warn("Unauthorized access attempt - user ID mismatch");
            throw new AccessDeniedException("You can only connect to your own mailbox");
        }

        log.info("WebSocket connection request received for email: {}", config.username());
        mailboxConnectionService.startMonitoring(config, config.userId());
        log.info("Connected to mailbox: {}", config.username());
    }

    @MessageMapping("/disconnect")
    public void disconnect(@Payload DisconnectRequest request, Principal principal) {

        if (!Objects.equals(principal.getName(), request.userId())) {
            log.warn("Unauthorized access attempt - user ID mismatch");
            throw new AccessDeniedException("You can only disconnect from your own mailbox");
        }

        mailboxConnectionService.stopMailboxMonitoring(request.email());
        log.info("WebSocket disconnection request received for email: {}", request.email());
    }
}