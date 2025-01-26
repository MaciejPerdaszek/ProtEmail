package com.example.api.service;

import java.util.Map;
import com.example.api.dto.EmailConfigRequest;

public interface MailboxConnectionService {

    Map<String, Boolean> getMailboxConnectionStates(String userId);

    void startMonitoring(EmailConfigRequest config);

    void stopMailboxMonitoring(String email, String userId);

    void stopAllMailboxMonitoring(String userId);
}
