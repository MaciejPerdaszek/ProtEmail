package com.example.api.service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    public WebSocketServiceImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }


    @Override
    public void connect(String email, String sessionId) {
        sessions.put(email, sessionId);
        log.info("Stored session for email: {}", email);
        log.info("Active sessions: {}", sessions);
    }

    @Override
    public void disconnect(String email) {
        sessions.remove(email);
        log.info("Removed session for email: {}", email);
        log.info("Active sessions: {}", sessions);
    }

    @Override
    public void sendMessage(String email, String message) {
        if (sessions.containsKey(email)) {
            messagingTemplate.convertAndSendToUser(email, "/topic/" + email, message);
            log.info("Message sent to {}: {}", email, message);
        } else {
            log.warn("No session found for email: {}", email);
        }
    }
}
