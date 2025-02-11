package com.example.api;

import com.example.api.dto.ThreatNotification;
import com.example.api.dto.WebSocketResponse;
import com.example.api.model.ScanLog;
import com.example.api.service.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketNotificationService notificationService;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;

    @Captor
    private ArgumentCaptor<String> destinationCaptor;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "user123";

    @BeforeEach
    void setUp() {
        notificationService = new WebSocketNotificationService(messagingTemplate);
    }

    @Test
    void notifyThreatDetected_ShouldSendCorrectNotification() {
        ScanLog scanLog = new ScanLog();
        scanLog.setThreatLevel("HIGH");

        notificationService.notifyThreatDetected(TEST_EMAIL, TEST_USER_ID, scanLog);

        verify(messagingTemplate).convertAndSend(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        String expectedDestination = "/topic/emails/" + TEST_EMAIL + "/" + TEST_USER_ID;
        assertEquals(expectedDestination, destinationCaptor.getValue());

        ThreatNotification capturedNotification = (ThreatNotification) messageCaptor.getValue();
        assertEquals("HIGH", capturedNotification.threatLevel());
    }

    @Test
    void notifyConnectionError_ShouldSendCorrectErrorMessage() {
        String errorCause = "Connection timeout";

        notificationService.notifyConnectionError(TEST_EMAIL, TEST_USER_ID, errorCause);

        verify(messagingTemplate).convertAndSend(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        String expectedDestination = "/topic/connect/" + TEST_EMAIL + "/" + TEST_USER_ID;
        assertEquals(expectedDestination, destinationCaptor.getValue());

        WebSocketResponse capturedResponse = (WebSocketResponse) messageCaptor.getValue();
        assertEquals("ERROR", capturedResponse.error());
        assertEquals(errorCause, capturedResponse.cause());
    }

    @Test
    void notifyConnectionSuccess_ShouldSendSuccessMessage() {
        notificationService.notifyConnectionSuccess(TEST_EMAIL, TEST_USER_ID);

        verify(messagingTemplate).convertAndSend(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        String expectedDestination = "/topic/connect/" + TEST_EMAIL + "/" + TEST_USER_ID;
        assertEquals(expectedDestination, destinationCaptor.getValue());

        WebSocketResponse capturedResponse = (WebSocketResponse) messageCaptor.getValue();
        assertEquals("SUCCESS", capturedResponse.error());
        assertEquals("", capturedResponse.cause());
    }

    @Test
    void sendScanLog_ShouldSendCorrectScanLog() {
        ScanLog scanLog = new ScanLog();
        scanLog.setThreatLevel("LOW");

        notificationService.sendScanLog(TEST_EMAIL, TEST_USER_ID, scanLog);

        verify(messagingTemplate).convertAndSend(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        String expectedDestination = "/topic/scanlog/" + TEST_EMAIL + "/" + TEST_USER_ID;
        assertEquals(expectedDestination, destinationCaptor.getValue());

        ScanLog capturedScanLog = (ScanLog) messageCaptor.getValue();
        assertEquals(scanLog, capturedScanLog);
    }
}