package com.example.api.service;

import com.example.api.model.Mailbox;
import com.example.api.model.ScanLog;

public interface ScanResultService {

    ScanLog performPhishingScan(long mailboxId);
}
