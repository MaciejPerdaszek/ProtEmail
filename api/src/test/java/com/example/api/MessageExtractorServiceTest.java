package com.example.api;

import com.example.api.dto.EmailContent;
import com.example.api.model.Mailbox;
import com.example.api.model.PhishingScanResult;
import com.example.api.model.ScanLog;
import com.example.api.repository.MailboxRepository;
import com.example.api.repository.ScanLogRepository;
import com.example.api.service.MessageExtractorServiceImpl;
import com.example.api.service.PhishingScannerService;
import com.example.api.service.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageExtractorServiceTest {

    @Mock
    private ScanLogRepository scanLogRepository;

    @Mock
    private MailboxRepository mailboxRepository;

    @Mock
    private PhishingScannerService phishingScannerService;

    @Mock
    private WebSocketNotificationService notificationService;

    @InjectMocks
    private MessageExtractorServiceImpl messageExtractorService;

    private EmailContent testEmailContent;
    private Mailbox testMailbox;

    @BeforeEach
    void setUp() throws Exception {
        testMailbox = new Mailbox();
        testMailbox.setId(1L);
        testMailbox.setEmail("test@example.com");
        testMailbox.setUserId("test-user-id");

        Address[] fromAddresses = new Address[]{
                new InternetAddress("sender@example.com")
        };

        testEmailContent = new EmailContent(
                "test@example.com",
                "test-user-id",
                "test-message-id",
                "Test content",
                "Test subject",
                fromAddresses
        );
    }

    @Test
    void performPhishingScan_Success() throws Exception {
        when(mailboxRepository.findByEmailAndUserId(anyString(), anyString()))
                .thenReturn(Optional.of(testMailbox));

        when(phishingScannerService.scanEmail(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(new PhishingScanResult(0, "Low", Collections.emptyList()));

        when(scanLogRepository.save(any(ScanLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        messageExtractorService.performPhishingScan(testEmailContent);

        verify(scanLogRepository, times(2)).save(any(ScanLog.class));
        verify(notificationService, times(2)).sendScanLog(anyString(), anyString(), any(ScanLog.class));
    }

    @Test
    void performPhishingScan_WithThreatDetected() throws Exception {
        when(mailboxRepository.findByEmailAndUserId(anyString(), anyString()))
                .thenReturn(Optional.of(testMailbox));

        PhishingScanResult threatResult = new PhishingScanResult(
                50,
                "High",
                Collections.singletonList("Suspicious content detected")
        );

        when(phishingScannerService.scanEmail(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(threatResult);

        when(scanLogRepository.save(any(ScanLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        messageExtractorService.performPhishingScan(testEmailContent);

        verify(scanLogRepository, times(2)).save(any(ScanLog.class));
        verify(notificationService, times(2)).sendScanLog(anyString(), anyString(), any(ScanLog.class));
        verify(notificationService, times(1)).notifyThreatDetected(anyString(), anyString(), any(ScanLog.class));
    }
}
