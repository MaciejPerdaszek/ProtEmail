package com.example.api.controller;

import java.util.List;
import com.example.api.model.ScanLog;
import com.example.api.service.ScanLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/scan-logs")
public class ScanLogController {
    private final ScanLogService scanLogService;

    public ScanLogController(ScanLogService scanLogService) {
        this.scanLogService = scanLogService;
    }

    @GetMapping
    public ResponseEntity<Page<ScanLog>> getScanLogs(
            @RequestParam(required = false) List<Long> mailboxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching scan logs for mailboxIds: {}, page: {}, size: {}",
                mailboxId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("scanDate").descending());
        Page<ScanLog> logs = scanLogService.getScanLogs(mailboxId, pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{scanLogId}")
    public ResponseEntity<ScanLog> getScanLog(@PathVariable long scanLogId) {
        ScanLog scanLog = scanLogService.getScanLogById(scanLogId);
        return ResponseEntity.ok(scanLog);
    }

    @PostMapping("/")
    public ResponseEntity<ScanLog> saveScanLog(@RequestBody ScanLog scanLog) {
        ScanLog savedScanLog = scanLogService.saveScanLog(scanLog);
        return ResponseEntity.ok(savedScanLog);
    }

    @DeleteMapping("/{scanLogId}")
    public ResponseEntity<Void> deleteScanLog(@PathVariable long scanLogId) {
        scanLogService.deleteScanLog(scanLogId);
        return ResponseEntity.noContent().build();
    }
}
