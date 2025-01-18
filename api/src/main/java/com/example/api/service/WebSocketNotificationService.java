package com.example.api.service;

import com.example.api.dto.ThreatNotification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.example.api.model.ScanLog;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyThreatDetected(String email, ScanLog scanLog) {
        ThreatNotification notification = new ThreatNotification(
                scanLog.getThreatLevel()
        );

        messagingTemplate.convertAndSend("/topic/emails/" + email, notification);
    }
}
