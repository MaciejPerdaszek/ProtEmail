package com.example.api.controller;

import java.util.List;
import java.util.Objects;
import com.example.api.model.ScanLog;
import com.example.api.service.ScanLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/scan-logs")
public class ScanLogController {
    private final ScanLogService scanLogService;

    public ScanLogController(ScanLogService scanLogService) {
        this.scanLogService = scanLogService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Page<ScanLog>> getScanLogs(
            @PathVariable String userId,
            @RequestParam(required = false) List<Long> mailboxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal OAuth2User user
    ) {

        if (!Objects.equals(user.getAttribute("sub"), userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Fetching scan logs for mailboxIds: {}, page: {}, size: {}",
                mailboxId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("scanDate").descending());
        Page<ScanLog> logs = scanLogService.getScanLogs(mailboxId, pageable);

        return ResponseEntity.ok(logs);
    }
}
