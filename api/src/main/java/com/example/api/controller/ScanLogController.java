package com.example.api.controller;

import java.util.List;
import com.example.api.model.ScanLog;
import com.example.api.service.ScanLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scan-logs")
public class ScanLogController {

    private final ScanLogService scanLogService;

    public ScanLogController(ScanLogService scanLogService) {
        this.scanLogService = scanLogService;
    }

    @GetMapping("/mailbox/{mailboxId}")
    public ResponseEntity<List<ScanLog>> getScanLogsForMailbox(@PathVariable long mailboxId) {
        List<ScanLog> scanLogs = scanLogService.getScanLogsForMailbox(mailboxId);
        return ResponseEntity.ok(scanLogs);
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
