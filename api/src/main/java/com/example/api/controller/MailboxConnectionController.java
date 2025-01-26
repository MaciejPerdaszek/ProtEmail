package com.example.api.controller;

import java.util.Map;
import java.util.Objects;
import com.example.api.service.MailboxConnectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mailbox-connections")
public class MailboxConnectionController {

    private final MailboxConnectionService mailboxConnectionService;

    public MailboxConnectionController(MailboxConnectionService mailboxConnectionService) {
        this.mailboxConnectionService = mailboxConnectionService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Boolean>> getConnectionStates(@PathVariable String userId, @AuthenticationPrincipal OAuth2User user) {

        if (!Objects.equals(user.getAttribute("sub"), userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Boolean> states = mailboxConnectionService.getMailboxConnectionStates(userId);
        return ResponseEntity.ok(states);
    }
}
