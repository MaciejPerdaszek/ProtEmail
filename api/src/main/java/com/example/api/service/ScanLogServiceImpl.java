package com.example.api.service;

import java.util.List;
import com.example.api.model.ScanLog;
import com.example.api.repository.ScanLogRepository;
import org.springframework.stereotype.Service;

@Service
public class ScanLogServiceImpl implements ScanLogService {

    private final ScanLogRepository scanLogRepository;

    public ScanLogServiceImpl(ScanLogRepository scanLogRepository) {
        this.scanLogRepository = scanLogRepository;
    }

    @Override
    public List<ScanLog> getScanLogs() {
        return scanLogRepository.findAll();
    }

    @Override
    public List<ScanLog> getScanLogsForMailbox(long mailboxId) {
        return scanLogRepository.findByMailboxId(mailboxId);
    }

    @Override
    public ScanLog getScanLogById(long theId) {
        return scanLogRepository.findById(theId).orElseThrow(() -> new RuntimeException("Did not find scan log id - " + theId));
    }

    @Override
    public ScanLog saveScanLog(ScanLog theScanLog) {
        return scanLogRepository.save(theScanLog);
    }

    @Override
    public void deleteScanLog(long theId) {
        scanLogRepository.deleteById(theId);
    }
}
