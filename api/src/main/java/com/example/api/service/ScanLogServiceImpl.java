package com.example.api.service;

import java.util.List;
import com.example.api.model.ScanLog;
import com.example.api.repository.ScanLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ScanLogServiceImpl implements ScanLogService {

    private final ScanLogRepository scanLogRepository;

    public ScanLogServiceImpl(ScanLogRepository scanLogRepository) {
        this.scanLogRepository = scanLogRepository;
    }

    @Override
    public Page<ScanLog> getScanLogs(List<Long> mailboxIds, Pageable pageable) {
        return scanLogRepository.findByMailboxIdIn(mailboxIds, pageable);
    }
}
