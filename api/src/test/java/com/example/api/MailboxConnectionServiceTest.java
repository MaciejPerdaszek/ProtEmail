package com.example.api;

import com.example.api.dto.EmailConfigRequest;
import com.example.api.model.Mailbox;
import com.example.api.repository.MailboxRepository;
import com.example.api.service.MailboxConnectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailboxConnectionServiceTest {

    @Mock
    private MailboxRepository mailboxRepository;

    @InjectMocks
    private MailboxConnectionServiceImpl mailboxConnectionService;

    private EmailConfigRequest testConfig;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_USER_ID = "test-123";

    @BeforeEach
    void setUp() {
        testConfig = new EmailConfigRequest(
                "imap.test.com",
                "993",
                "imap",
                TEST_EMAIL,
                TEST_USER_ID
        );

        Mailbox testMailbox = new Mailbox();
        testMailbox.setEmail(TEST_EMAIL);
        testMailbox.setPassword("password");

        Mockito.lenient().when(mailboxRepository.findByEmailAndUserId(TEST_EMAIL, TEST_USER_ID))
                .thenReturn(Optional.of(testMailbox));
    }

    @Test
    void startMonitoring_ShouldCreateNewPollingTask() {
        Mailbox mailbox = new Mailbox();
        mailbox.setEmail(TEST_EMAIL);
        mailbox.setPassword("password");
        when(mailboxRepository.findByEmailAndUserId(TEST_EMAIL, TEST_USER_ID))
                .thenReturn(Optional.of(mailbox));

        mailboxConnectionService.startMonitoring(testConfig);

        Map<String, Boolean> states = mailboxConnectionService.getMailboxConnectionStates(TEST_USER_ID);
        assertTrue(states.get(TEST_EMAIL));
    }

    @Test
    void stopMonitoring_ShouldRemovePollingTask() {
        mailboxConnectionService.startMonitoring(testConfig);
        mailboxConnectionService.stopMailboxMonitoring(TEST_EMAIL, TEST_USER_ID);

        Map<String, Boolean> states = mailboxConnectionService.getMailboxConnectionStates(TEST_USER_ID);
        assertFalse(states.containsKey(TEST_EMAIL));
    }

    @Test
    void getMailboxConnectionStates_ShouldReturnCorrectStates() {
        Mailbox mailbox1 = new Mailbox();
        mailbox1.setEmail(TEST_EMAIL);
        mailbox1.setPassword("password");
        when(mailboxRepository.findByEmailAndUserId(TEST_EMAIL, TEST_USER_ID))
                .thenReturn(Optional.of(mailbox1));

        String email2 = "test2@example.com";
        Mailbox mailbox2 = new Mailbox();
        mailbox2.setEmail(email2);
        mailbox2.setPassword("password");
        when(mailboxRepository.findByEmailAndUserId(email2, TEST_USER_ID))
                .thenReturn(Optional.of(mailbox2));

        mailboxConnectionService.startMonitoring(testConfig);

        EmailConfigRequest config2 = new EmailConfigRequest(
                "imap.test.com",
                "993",
                "imap",
                email2,
                TEST_USER_ID
        );
        mailboxConnectionService.startMonitoring(config2);

        Map<String, Boolean> states = mailboxConnectionService.getMailboxConnectionStates(TEST_USER_ID);
        assertEquals(2, states.size());
        assertTrue(states.get(TEST_EMAIL));
        assertTrue(states.get(email2));
    }

    @Test
    void stopAllMailboxMonitoring_ShouldStopAllTasks() {
        Mailbox mailbox1 = new Mailbox();
        mailbox1.setEmail(TEST_EMAIL);
        mailbox1.setPassword("password");
        when(mailboxRepository.findByEmailAndUserId(TEST_EMAIL, TEST_USER_ID))
                .thenReturn(Optional.of(mailbox1));

        String email2 = "test2@example.com";
        Mailbox mailbox2 = new Mailbox();
        mailbox2.setEmail(email2);
        mailbox2.setPassword("password");
        when(mailboxRepository.findByEmailAndUserId(email2, TEST_USER_ID))
                .thenReturn(Optional.of(mailbox2));

        mailboxConnectionService.startMonitoring(testConfig);

        EmailConfigRequest config2 = new EmailConfigRequest(
                "imap.test.com",
                "993",
                "imap",
                email2,
                TEST_USER_ID
        );
        mailboxConnectionService.startMonitoring(config2);

        mailboxConnectionService.stopAllMailboxMonitoring(TEST_USER_ID);

        Map<String, Boolean> states = mailboxConnectionService.getMailboxConnectionStates(TEST_USER_ID);
        assertTrue(states.isEmpty());
    }

    @Test
    void cleanup_ShouldStopAllTasksAndExecutors() {
        mailboxConnectionService.startMonitoring(testConfig);

        ReflectionTestUtils.invokeMethod(mailboxConnectionService, "cleanup");

        Map<String, Boolean> states = mailboxConnectionService.getMailboxConnectionStates(TEST_USER_ID);
        assertTrue(states.isEmpty());
    }
}