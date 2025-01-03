package com.example.api.controller;

import java.util.Map;
import com.example.api.service.MailboxConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
public class MailboxConnectionController {

    private final MailboxConnectionService mailboxConnectionService;

    public MailboxConnectionController(MailboxConnectionService mailboxConnectionService) {
        this.mailboxConnectionService = mailboxConnectionService;
    }

    @GetMapping("/connection-states")
    public ResponseEntity<Map<String, Boolean>> getConnectionStates() {
        Map<String, Boolean> states = mailboxConnectionService.getMailboxConnectionStates();
        return ResponseEntity.ok(states);
    }
}
