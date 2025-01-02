package com.example.api.service;

import java.util.List;
import java.util.stream.Collectors;
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
    public List<ScanLog> getScanLogs(int page, int size) {
        return scanLogRepository.findAll()
                .stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScanLog> getScanLogsForMailbox(long mailboxId, int page, int size) {
        return scanLogRepository.findByMailboxId(mailboxId)
                .stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public Long getCount(Long mailboxId) {
        if (mailboxId != null) {
            return scanLogRepository.countByMailboxId(mailboxId);
        } else {
            return scanLogRepository.count();
        }
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
