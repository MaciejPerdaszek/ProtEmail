package com.example.api.service;

import java.time.LocalDateTime;
import java.util.List;
import com.example.api.model.Email;
import com.example.api.model.Mailbox;
import com.example.api.model.ScanLog;
import com.example.api.model.ScanResult;
import com.example.api.repository.EmailRepository;
import com.example.api.repository.MailboxRepository;
import com.example.api.repository.ScanLogRepository;
import com.example.api.repository.ScanResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScanResultServiceImpl implements ScanResultService {

    private final EmailRepository emailRepository;
    private final ScanLogRepository scanLogRepository;
    private final ScanResultRepository scanResultRepository;
    private final MailboxRepository mailboxRepository;

    @Autowired
    public ScanResultServiceImpl(EmailRepository emailRepository, ScanLogRepository scanLogRepository, ScanResultRepository scanResultRepository, MailboxRepository mailboxRepository) {
        this.emailRepository = emailRepository;
        this.scanLogRepository = scanLogRepository;
        this.scanResultRepository = scanResultRepository;
        this.mailboxRepository = mailboxRepository;
    }

    @Override
    public ScanLog performPhishingScan(long mailboxId) {
        List<Email> emails = emailRepository.findByMailboxId(mailboxId);
        int phishingCount = 0;

        for (Email email : emails) {
            boolean isPhishing = analyzeEmailForPhishing(email);
            ScanResult result = new ScanResult();
            result.setIsPhishing(isPhishing);
            result.setEmail(email);
            scanResultRepository.save(result);

            if (isPhishing) phishingCount++;
        }

        ScanLog log = new ScanLog();
        log.setScanDate(LocalDateTime.now());
        log.setTotalEmailsScanned(emails.size());
        log.setPhishingEmailsDetected(phishingCount);
        Mailbox mailbox = mailboxRepository.findById(mailboxId).orElseThrow(() -> new RuntimeException("Did not find mailbox id - " + mailboxId));
        log.setMailbox(mailbox);

        return scanLogRepository.save(log);
    }

    private boolean analyzeEmailForPhishing(Email email) {
        //TODO: Implement phishing scan logic
        return false;
    }
}
