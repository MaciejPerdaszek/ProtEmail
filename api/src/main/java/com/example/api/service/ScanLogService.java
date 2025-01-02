package com.example.api.service;

import java.util.List;
import com.example.api.model.ScanLog;

public interface ScanLogService {

    List<ScanLog> getScanLogs();

    List<ScanLog> getScanLogsForMailbox(long mailboxId);

    ScanLog getScanLogById(long theId);

    ScanLog saveScanLog(ScanLog theScanLog);

    void deleteScanLog(long theId);
}
