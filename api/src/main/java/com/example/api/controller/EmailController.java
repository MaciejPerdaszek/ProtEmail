package com.example.api.controller;

import java.util.List;
import java.util.Map;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.model.Email;
import com.example.api.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/{mailboxId}")
    public ResponseEntity<List<Email>> getEmailsForMailbox(@PathVariable long mailboxId) {
        List<Email> emails = emailService.getEmailsForMailbox(mailboxId);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/{emailId}")
    public ResponseEntity<Email> getEmail(@PathVariable long emailId) {
        Email email = emailService.getEmailById(emailId);
        return ResponseEntity.ok(email);
    }

    @GetMapping("/")
    public ResponseEntity<Email> saveEmail(@RequestBody Email email) {
        Email savedEmail = emailService.saveEmail(email);
        return ResponseEntity.ok(savedEmail);
    }

    @DeleteMapping("/{emailId}")
    public ResponseEntity<Void> deleteEmail(@PathVariable long emailId) {
        emailService.deleteEmail(emailId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fetch")
    public List<Email> getEmails(@RequestBody EmailConfigRequest config) {
        return emailService.getEmailsFromMailbox(config);
    }

    @GetMapping("/monitored-emails")
    public Map<String, Boolean> getMonitoredEmails() {
        return emailService.getMailboxConnectionStates();
    }
}
