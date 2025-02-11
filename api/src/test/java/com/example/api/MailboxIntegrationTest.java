package com.example.api;

import com.example.api.model.Mailbox;
import com.example.api.repository.MailboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc(addFilters = false)
class MailboxIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final String userId = "test-user-id";

    @BeforeEach
    void setUp() {
        mailboxRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldGetUserMailboxes() throws Exception {
        Mailbox mailbox = new Mailbox();
        mailbox.setEmail("test@example.com");
        mailbox.setPassword("encrypted-password");
        mailbox.setType("IMAP");
        mailbox.setUserId(userId);
        mailboxRepository.save(mailbox);

        mockMvc.perform(get("/api/mailboxes/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].type").value("IMAP"));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldAddNewMailbox() throws Exception {
        Mailbox newMailbox = new Mailbox();
        newMailbox.setEmail("new@example.com");
        newMailbox.setPassword("password123");
        newMailbox.setType("IMAP");
        newMailbox.setUserId(userId);

        mockMvc.perform(post("/api/mailboxes/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMailbox)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.type").value("IMAP"));

        assertEquals(1, mailboxRepository.findByUserId(userId).size());
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldUpdateMailbox() throws Exception {
        Mailbox existingMailbox = new Mailbox();
        existingMailbox.setEmail("existing@example.com");
        existingMailbox.setPassword("old-password");
        existingMailbox.setType("IMAP");
        existingMailbox.setUserId(userId);
        existingMailbox = mailboxRepository.save(existingMailbox);

        Mailbox updateMailbox = new Mailbox();
        updateMailbox.setEmail("updated@example.com");
        updateMailbox.setPassword("new-password");
        updateMailbox.setType("IMAP");

        mockMvc.perform(put("/api/mailboxes/" + userId + "/" + existingMailbox.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMailbox)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldDeleteMailbox() throws Exception {
        Mailbox mailbox = new Mailbox();
        mailbox.setEmail("delete@example.com");
        mailbox.setPassword("password");
        mailbox.setType("IMAP");
        mailbox.setUserId(userId);
        mailbox = mailboxRepository.save(mailbox);

        mockMvc.perform(delete("/api/mailboxes/" + userId + "/" + mailbox.getId()))
                .andExpect(status().isNoContent());

        assertTrue(mailboxRepository.findById(mailbox.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void shouldNotAllowDuplicateEmailForSameUser() throws Exception {
        Mailbox existingMailbox = new Mailbox();
        existingMailbox.setEmail("duplicate@example.com");
        existingMailbox.setPassword("password123");
        existingMailbox.setType("IMAP");
        existingMailbox.setUserId(userId);
        mailboxRepository.save(existingMailbox);

        Mailbox duplicateMailbox = new Mailbox();
        duplicateMailbox.setEmail("duplicate@example.com");
        duplicateMailbox.setPassword("different-password");
        duplicateMailbox.setType("IMAP");

        mockMvc.perform(post("/api/mailboxes/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateMailbox)))
                .andExpect(status().isConflict());
    }
}