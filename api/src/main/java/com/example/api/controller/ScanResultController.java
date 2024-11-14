package com.example.api.controller;
import com.example.api.model.ScanLog;

import com.example.api.service.ScanResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scan-results")
public class ScanResultController {

    private final ScanResultService scanResultService;

    public ScanResultController(ScanResultService scanResultService) {
        this.scanResultService = scanResultService;
    }

    @PostMapping("/mailbox/{mailboxId}")
    public ResponseEntity<ScanLog> saveScanResult(@PathVariable long mailboxId) {
        ScanLog savedScanResult = scanResultService.performPhishingScan(mailboxId);
        return ResponseEntity.ok(savedScanResult);
    }

}
