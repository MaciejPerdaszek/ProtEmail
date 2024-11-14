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
    public ResponseEntity<List<Mailbox>> getUserMailboxes(@PathVariable long userId) {
        List<Mailbox> mailboxes = mailboxService.getUserMailboxes(userId);
        return ResponseEntity.ok(mailboxes);
    }

    @GetMapping("/user/{mailboxId}")
    public ResponseEntity<Mailbox> getMailbox(@PathVariable long mailboxId) {
        Mailbox mailbox = mailboxService.getMailboxById(mailboxId);
        return ResponseEntity.ok(mailbox);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Mailbox> addMailbox(@RequestBody Mailbox mailbox, @PathVariable long userId) {
        Mailbox savedMailbox = mailboxService.addMailbox(mailbox, userId);
        return ResponseEntity.ok(savedMailbox);
    }

    @DeleteMapping("/{mailboxId}")
    public ResponseEntity<Void> deleteMailbox(@PathVariable long mailboxId) {
        mailboxService.deleteMailbox(mailboxId);
        return ResponseEntity.noContent().build();
    }
}
