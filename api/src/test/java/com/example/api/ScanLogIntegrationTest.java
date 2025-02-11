package com.example.api;

import com.example.api.model.Mailbox;
import com.example.api.model.ScanLog;
import com.example.api.repository.MailboxRepository;
import com.example.api.repository.ScanLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc(addFilters = false)
class ScanLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScanLogRepository scanLogRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    private final String userId = "test-user-id";
    private Mailbox testMailbox1;
    private Mailbox testMailbox2;

    @BeforeEach
    void setUp() {
        scanLogRepository.deleteAll();
        mailboxRepository.deleteAll();

        testMailbox1 = createMailbox("test1@example.com", "IMAP");
        testMailbox2 = createMailbox("test2@example.com", "POP3");
        mailboxRepository.saveAll(List.of(testMailbox1, testMailbox2));
    }

    private Mailbox createMailbox(String email, String type) {
        Mailbox mailbox = new Mailbox();
        mailbox.setUserId(userId);
        mailbox.setEmail(email);
        mailbox.setPassword("encrypted-password");
        mailbox.setType(type);
        return mailbox;
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldGetScanLogsForMultipleMailboxes() throws Exception {
        ScanLog log1 = createScanLog(testMailbox1, "Sender1", "Subject1", "LOW");
        ScanLog log2 = createScanLog(testMailbox2, "Sender2", "Subject2", "HIGH");
        scanLogRepository.saveAll(Arrays.asList(log1, log2));

        mockMvc.perform(get("/api/scan-logs/" + userId)
                        .param("mailboxId", String.valueOf(testMailbox1.getId()))
                        .param("mailboxId", String.valueOf(testMailbox2.getId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldGetScanLogsForSingleMailbox() throws Exception {
        ScanLog log1 = createScanLog(testMailbox1, "Sender1", "Subject1", "LOW");
        ScanLog log2 = createScanLog(testMailbox1, "Sender2", "Subject2", "MEDIUM");
        ScanLog log3 = createScanLog(testMailbox2, "Sender3", "Subject3", "HIGH");
        scanLogRepository.saveAll(Arrays.asList(log1, log2, log3));

        mockMvc.perform(get("/api/scan-logs/" + userId)
                        .param("mailboxId", String.valueOf(testMailbox1.getId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldReturnEmptyPageWhenNoLogs() throws Exception {
        mockMvc.perform(get("/api/scan-logs/" + userId)
                        .param("mailboxId", String.valueOf(testMailbox1.getId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldSupportPagination() throws Exception {
        for (int i = 0; i < 15; i++) {
            ScanLog log = createScanLog(testMailbox1, "Sender" + i, "Subject" + i, "LOW");
            scanLogRepository.save(log);
        }

        mockMvc.perform(get("/api/scan-logs/" + userId)
                        .param("mailboxId", String.valueOf(testMailbox1.getId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(15));

        mockMvc.perform(get("/api/scan-logs/" + userId)
                        .param("mailboxId", String.valueOf(testMailbox1.getId()))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    private ScanLog createScanLog(Mailbox mailbox, String sender, String subject, String threatLevel) {
        ScanLog scanLog = new ScanLog();
        scanLog.setMailbox(mailbox);
        scanLog.setSender(sender);
        scanLog.setSubject(subject);
        scanLog.setThreatLevel(threatLevel);
        scanLog.setScanDate(new Date());
        scanLog.setComment("Test scan log");
        return scanLog;
    }
}