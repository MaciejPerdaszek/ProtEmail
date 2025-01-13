package com.example.api.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.api.dto.EmailContent;
import com.example.api.model.PhishingScanResult;
import com.example.api.model.ScanLog;
import com.example.api.repository.MailboxRepository;
import com.example.api.repository.ScanLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageExtractorServiceImpl implements MessageExtractorService {

    private static final String URL_REGEX = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";

    private final ScanLogRepository scanLogRepository;
    private final MailboxRepository mailboxRepository;
    private final PhishingScannerService phishingScannerService;
    private final WebSocketNotificationService notificationService;

    @Autowired
    public MessageExtractorServiceImpl(ScanLogRepository scanLogRepository, MailboxRepository mailboxRepository,
                                       PhishingScannerService phishingScannerService, WebSocketNotificationService notificationService) {
        this.scanLogRepository = scanLogRepository;
        this.mailboxRepository = mailboxRepository;
        this.phishingScannerService = phishingScannerService;
        this.notificationService = notificationService;
    }

    @Override
    public void performPhishingScan(EmailContent emailContent) {
        ScanLog scanLog = null;
        try {
            log.info("Email content details:");
            log.info("From: {}", emailContent.from() != null ? Arrays.toString(emailContent.from()) : "null");
            log.info("Subject: {}", emailContent.subject());
            log.info("Content object: {}", emailContent.content());
            log.info("Content class: {}", emailContent.content() != null ? emailContent.content().getClass().getName() : "null");

            List<String> urls = extractUrls(emailContent.content());
            log.info("Performing phishing scan for email from: {} subject: {}", extractSender(emailContent), emailContent.subject());

            scanLog = createScanLog(emailContent);

            PhishingScanResult scanResult = phishingScannerService.scanEmail(extractSender(emailContent), emailContent.subject(), emailContent.content(), urls);

            processScanResults(scanLog, scanResult);

            log.info("Scan completed for email from: {} subject: {}", scanLog.getSender(), scanLog.getSubject());
        } catch (Exception e) {
            if (scanLog != null) {
                handleAbortScan(scanLog);
            }
            log.error("Error during phishing scan: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to perform phishing scan", e);
        }
    }

    private List<String> extractUrls(String content) {
        List<String> urls = new ArrayList<>();
        Pattern pattern = Pattern.compile(URL_REGEX);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String url = matcher.group();
            urls.add(url);

        }
        log.info("Extracted URLs: {}", urls);
        return urls;
    }

    private ScanLog createScanLog(EmailContent emailContent) {
        ScanLog scanLog = new ScanLog();
        scanLog.setSender(extractSender(emailContent));
        scanLog.setSubject(emailContent.subject());
        scanLog.setScanDate(new Date());
        scanLog.setMailbox(mailboxRepository.findByEmail(emailContent.username())
                .orElseThrow(() -> new RuntimeException("Mailbox not found: " + emailContent.username())));
        scanLog.setThreatLevel("Pending");
        scanLog.setComment("URL scan in progress");
        return scanLogRepository.save(scanLog);
    }

    private String extractSender(EmailContent emailContent) {
        String from = emailContent.from()[0].toString();
        Matcher matcher = Pattern.compile("<(.+?)>").matcher(from);
        return matcher.find() ? matcher.group(1) : from;
    }

    private void processScanResults(ScanLog scanLog, PhishingScanResult scanResult) {
        scanLog.setThreatLevel(scanResult.getRiskLevel());
        scanLog.setComment(String.join("; ", scanResult.getThreats()));
        ScanLog savedLog = scanLogRepository.save(scanLog);

        if (!scanResult.getThreats().isEmpty() ||
                !"Low".equalsIgnoreCase(scanResult.getRiskLevel())) {
            notificationService.notifyThreatDetected(
                    scanLog.getMailbox().getEmail(),
                    savedLog
            );
        }
    }

    private void handleAbortScan(ScanLog scanLog) {
        scanLog.setThreatLevel("Error");
        scanLog.setComment("Phishing scan aborted");
        scanLogRepository.save(scanLog);
    }
}


