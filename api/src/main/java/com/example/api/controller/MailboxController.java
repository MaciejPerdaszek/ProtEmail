package com.example.api.controller;

import java.util.List;
import com.example.api.model.Mailbox;
import com.example.api.service.MailboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mailboxes")
public class MailboxController {

    private final MailboxService mailboxService;

    public MailboxController(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Mailbox>> getUserMailboxes(@PathVariable String userId) {
        List<Mailbox> mailboxes = mailboxService.getUserMailboxes(userId);
        return ResponseEntity.ok(mailboxes);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Mailbox> addMailbox(@RequestBody Mailbox mailbox, @PathVariable String userId) {
        Mailbox savedMailbox = mailboxService.addMailbox(mailbox, userId);
        return ResponseEntity.ok(savedMailbox);
    }

    @PutMapping("/{userId}/{mailboxId}")
    public ResponseEntity<Mailbox> updateMailbox(
            @RequestBody Mailbox mailbox,
            @PathVariable String userId,
            @PathVariable Long mailboxId) {
        Mailbox updatedMailbox = mailboxService.updateMailbox(mailbox, userId, mailboxId);
        return ResponseEntity.ok(updatedMailbox);
    }

    @DeleteMapping("/{mailboxId}")
    public ResponseEntity<Void> deleteMailbox(@PathVariable long mailboxId) {
        mailboxService.deleteMailbox(mailboxId);
        return ResponseEntity.noContent().build();
    }
}
