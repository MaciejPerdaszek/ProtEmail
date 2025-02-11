package com.example.api;

import com.example.api.exception.InvalidMailboxException;
import com.example.api.exception.MailboxAlreadyExistsException;
import com.example.api.model.Mailbox;
import com.example.api.repository.MailboxRepository;
import com.example.api.service.MailboxServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailboxServiceTest {

    @Mock
    private MailboxRepository mailboxRepository;

    @InjectMocks
    private MailboxServiceImpl mailboxService;

    private Mailbox testMailbox;
    private Mailbox existingMailbox;
    private final String userId = "test-user-id";
    private final Long mailboxId = 1L;

    @BeforeEach
    void setUp() {
        testMailbox = new Mailbox();
        testMailbox.setId(1L);
        testMailbox.setEmail("test@example.com");
        testMailbox.setPassword("password");
        testMailbox.setType("IMAP");
        testMailbox.setUserId(userId);

        existingMailbox = new Mailbox();
        existingMailbox.setId(mailboxId);
        existingMailbox.setEmail("existing@example.com");
        existingMailbox.setPassword("oldPassword");
        existingMailbox.setType("IMAP");
        existingMailbox.setUserId(userId);
    }


    @Test
    void getUserMailboxes_ShouldReturnUserMailboxes() {
        List<Mailbox> expectedMailboxes = Collections.singletonList(testMailbox);
        when(mailboxRepository.findByUserId(userId)).thenReturn(expectedMailboxes);

        List<Mailbox> actualMailboxes = mailboxService.getUserMailboxes(userId);

        assertEquals(expectedMailboxes, actualMailboxes);
        verify(mailboxRepository).findByUserId(userId);
    }

    @Test
    void addMailbox_ShouldSaveNewMailbox() {
        Mailbox newMailbox = new Mailbox();
        newMailbox.setEmail("new@example.com");
        when(mailboxRepository.existsByEmailAndUserId(anyString(), anyString())).thenReturn(false);
        when(mailboxRepository.save(any(Mailbox.class))).thenReturn(testMailbox);

        Mailbox savedMailbox = mailboxService.addMailbox(newMailbox, userId);

        assertEquals(testMailbox, savedMailbox);
        verify(mailboxRepository).save(any(Mailbox.class));
    }

    @Test
    void addMailbox_WithExistingId_ShouldThrowException() {
        Mailbox mailboxWithId = new Mailbox();
        mailboxWithId.setId(1L);

        assertThrows(InvalidMailboxException.class, () ->
                mailboxService.addMailbox(mailboxWithId, userId)
        );
    }

    @Test
    void addMailbox_WithExistingEmail_ShouldThrowException() {
        Mailbox newMailbox = new Mailbox();
        newMailbox.setEmail("test@example.com");
        newMailbox.setPassword("password");
        newMailbox.setType("IMAP");

        when(mailboxRepository.existsByEmailAndUserId("test@example.com", userId)).thenReturn(true);

        assertThrows(MailboxAlreadyExistsException.class, () ->
                mailboxService.addMailbox(newMailbox, userId)
        );

        verify(mailboxRepository).existsByEmailAndUserId("test@example.com", userId);
        verify(mailboxRepository, never()).save(any(Mailbox.class));
    }

    @Test
    void updateMailbox_WithValidData_ShouldUpdateSuccessfully() {
        Mailbox updateRequest = new Mailbox();
        updateRequest.setEmail("updated@example.com");
        updateRequest.setPassword("newPassword");
        updateRequest.setType("IMAP");

        when(mailboxRepository.findById(mailboxId)).thenReturn(Optional.of(existingMailbox));
        when(mailboxRepository.save(any(Mailbox.class))).thenAnswer(i -> i.getArgument(0));

        Mailbox updatedMailbox = mailboxService.updateMailbox(updateRequest, userId, mailboxId);

        assertEquals("updated@example.com", updatedMailbox.getEmail());
        assertEquals("newPassword", updatedMailbox.getPassword());
        assertEquals("IMAP", updatedMailbox.getType());
        assertEquals(mailboxId, updatedMailbox.getId());
        verify(mailboxRepository).save(any(Mailbox.class));
    }

    @Test
    void updateMailbox_WithNonExistentId_ShouldThrowException() {
        Mailbox updateRequest = new Mailbox();
        updateRequest.setEmail("test@example.com");

        when(mailboxRepository.findById(mailboxId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                mailboxService.updateMailbox(updateRequest, userId, mailboxId)
        );

        assertEquals("Did not find mailbox id - " + mailboxId, exception.getMessage());
        verify(mailboxRepository, never()).save(any(Mailbox.class));
    }

    @Test
    void updateMailbox_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        Mailbox updateRequest = new Mailbox();
        updateRequest.setPassword("newPassword");

        when(mailboxRepository.findById(mailboxId)).thenReturn(Optional.of(existingMailbox));
        when(mailboxRepository.save(any(Mailbox.class))).thenAnswer(i -> i.getArgument(0));

        Mailbox updatedMailbox = mailboxService.updateMailbox(updateRequest, userId, mailboxId);

        assertEquals("existing@example.com", updatedMailbox.getEmail());
        assertEquals("newPassword", updatedMailbox.getPassword());
        assertEquals("IMAP", updatedMailbox.getType());
        verify(mailboxRepository).save(any(Mailbox.class));
    }

    @Test
    void updateMailbox_WithNullId_ShouldThrowException() {
        Mailbox updateRequest = new Mailbox();
        updateRequest.setEmail("test@example.com");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                mailboxService.updateMailbox(updateRequest, userId, null)
        );

        assertEquals("Mailbox ID cannot be null for update operation", exception.getMessage());
        verify(mailboxRepository, never()).save(any(Mailbox.class));
    }

    @Test
    void deleteMailbox_WithValidId_ShouldDeleteSuccessfully() {
        mailboxService.deleteMailbox(mailboxId);

        verify(mailboxRepository).deleteById(mailboxId);
    }
}