package com.example.api.service;

import com.example.api.dto.ThreatNotification;
import com.example.api.dto.WebSocketResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.example.api.model.ScanLog;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    private String getTopicPath(String basePath, String email, String userId) {
        return basePath + "/" + email + "/" + userId;
    }

    public void notifyThreatDetected(String email, String userId, ScanLog scanLog) {
        ThreatNotification notification = new ThreatNotification(
                scanLog.getThreatLevel()
        );

        messagingTemplate.convertAndSend(
                getTopicPath("/topic/emails", email, userId),
                notification
        );
    }

    public void notifyConnectionError(String email, String userId, String cause) {
        WebSocketResponse notification = new WebSocketResponse(
                "ERROR", cause
        );

        messagingTemplate.convertAndSend(
                getTopicPath("/topic/connect", email, userId),
                notification
        );
    }

    public void notifyConnectionSuccess(String email, String userId) {
        WebSocketResponse notification = new WebSocketResponse(
                "SUCCESS", ""
        );

        messagingTemplate.convertAndSend(
                getTopicPath("/topic/connect", email, userId),
                notification
        );
    }

    public void sendScanLog(String email, String userId, ScanLog scanLog) {
        messagingTemplate.convertAndSend(
                getTopicPath("/topic/scanlog", email, userId),
                scanLog
        );
    }
}
