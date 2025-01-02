package com.example.api.service;

import java.util.List;
import com.example.api.model.ScanLog;

public interface ScanLogService {

    List<ScanLog> getScanLogs(int page, int size);

    List<ScanLog> getScanLogsForMailbox(long mailboxId, int page, int size);

    Long getCount(Long mailboxId);

    ScanLog getScanLogById(long theId);

    ScanLog saveScanLog(ScanLog theScanLog);

    void deleteScanLog(long theId);
}
