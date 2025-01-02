package com.example.api.controller;

import java.util.List;
import com.example.api.model.ScanLog;
import com.example.api.service.ScanLogService;
import lombok.extern.slf4j.Slf4j;
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

    @GetMapping()
    public List<ScanLog> getScanLogs(
            @RequestParam(required = false) Long mailboxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Received request with mailboxId: {}, page: {}, size: {}", mailboxId, page, size);

        if (mailboxId != null) {
            return scanLogService.getScanLogsForMailbox(mailboxId, page, size);
        }
        return scanLogService.getScanLogs(page, size);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCount(@RequestParam(required = false) Long mailboxId) {
        return ResponseEntity.ok(scanLogService.getCount(mailboxId));
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
