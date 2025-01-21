package com.example.api.service;

import java.util.Map;
import com.example.api.dto.EmailConfigRequest;

public interface MailboxConnectionService {

    Map<String, Boolean> getMailboxConnectionStates();

    void startMonitoring(EmailConfigRequest config, String currentUserId);

    void stopMailboxMonitoring(String email);

    void stopAllMailboxMonitoring();
}
