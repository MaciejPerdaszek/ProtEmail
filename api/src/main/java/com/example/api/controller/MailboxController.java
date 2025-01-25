package com.example.api.controller;

import java.util.List;
import java.util.Objects;
import com.example.api.model.Mailbox;
import com.example.api.service.MailboxService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mailboxes")
public class MailboxController {

    private final MailboxService mailboxService;

    public MailboxController(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Mailbox>> getUserMailboxes(@PathVariable String userId,
                                                          @AuthenticationPrincipal OAuth2User user) {

        if (!Objects.equals(user.getAttribute("sub"), userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Mailbox> mailboxes = mailboxService.getUserMailboxes(userId);
        return ResponseEntity.ok(mailboxes);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Mailbox> addMailbox(@RequestBody Mailbox mailbox, @PathVariable String userId,
                                              @AuthenticationPrincipal OAuth2User user) {

        if (!Objects.equals(user.getAttribute("sub"), userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Mailbox savedMailbox = mailboxService.addMailbox(mailbox, userId);
        return ResponseEntity.ok(savedMailbox);
    }

    @PutMapping("/{userId}/{mailboxId}")
    public ResponseEntity<Mailbox> updateMailbox(@RequestBody Mailbox mailbox, @PathVariable String userId,
                                                 @PathVariable Long mailboxId,
                                                 @AuthenticationPrincipal OAuth2User user) {

        if (!Objects.equals(user.getAttribute("sub"), userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Mailbox updatedMailbox = mailboxService.updateMailbox(mailbox, userId, mailboxId);
        return ResponseEntity.ok(updatedMailbox);
    }

    @DeleteMapping("/{userId}/{mailboxId}")
    public ResponseEntity<Void> deleteMailbox(@PathVariable String userId, @PathVariable long mailboxId,
                                              @AuthenticationPrincipal OAuth2User user) {

        if (!Objects.equals(user.getAttribute("sub"), userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        mailboxService.deleteMailbox(mailboxId);
        return ResponseEntity.noContent().build();
    }
}
