package com.example.api.service;

import java.util.List;
import com.example.api.model.ScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScanLogService {

    Page<ScanLog> getScanLogs(List<Long> mailboxIds, Pageable pageable);
}
