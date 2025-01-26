package com.example.api.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.api.dto.EmailContent;
import com.example.api.model.PhishingScanResult;
import com.example.api.model.ScanLog;
import com.example.api.repository.MailboxRepository;
import com.example.api.repository.ScanLogRepository;
import org.nibor.autolink.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageExtractorServiceImpl implements MessageExtractorService {

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
            List<String> urls = extractUrls(emailContent.subject() + " " + emailContent.content());
            String cleanHtml = emailContent.subject() + " " + cleanHtml(emailContent.content());
            log.info("Content object: {}", cleanHtml);

            scanLog = createScanLog(emailContent);

            PhishingScanResult scanResult = phishingScannerService.scanEmail(extractSender(emailContent), emailContent.subject(), cleanHtml, urls);

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
        LinkExtractor linkExtractor = LinkExtractor.builder()
                .linkTypes(EnumSet.of(LinkType.URL)) // limit to URLs
                .build();
        List<String> links = new ArrayList<>();
        for (var span : linkExtractor.extractLinks(content)) {
            var link = content.substring(span.getBeginIndex(), span.getEndIndex());
            links.add(link);
        }

        Jsoup.parse(content).select("a[href]").forEach(element -> {
            links.add(element.attr("href"));
        });

        log.info("Extracted URLs: {}", links);
        return links;
    }

    private ScanLog createScanLog(EmailContent emailContent) {
        ScanLog scanLog = new ScanLog();
        scanLog.setSender(extractSender(emailContent));
        if (emailContent.subject() == null || emailContent.subject().isEmpty()) {
            scanLog.setSubject("No subject");
        } else {
            scanLog.setSubject(emailContent.subject());
        }
        scanLog.setScanDate(new Date());
        scanLog.setMailbox(mailboxRepository.findByEmailAndUserId(emailContent.username(), emailContent.currentUserId())
                .orElseThrow(() -> new RuntimeException("Mailbox not found: " + emailContent.username())));
        scanLog.setThreatLevel("Pending");
        scanLog.setComment("URL scan in progress");

        ScanLog savedLog = scanLogRepository.save(scanLog);
        notificationService.sendScanLog(emailContent.username(), emailContent.currentUserId(), savedLog);
        return savedLog;
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
        notificationService.sendScanLog(scanLog.getMailbox().getEmail(), savedLog.getMailbox().getUserId(), savedLog);

        if (!scanResult.getThreats().isEmpty() ||
                !"Low".equalsIgnoreCase(scanResult.getRiskLevel())) {
            notificationService.notifyThreatDetected(
                    scanLog.getMailbox().getEmail(),
                    scanLog.getMailbox().getUserId(),
                    savedLog
            );
        }
    }

    private String cleanHtml(String content) {
        content = Jsoup.parse(content).text();
        content = content.replaceAll("<\\s+", "<")
                .replaceAll("\\s+>", ">")
                .replaceAll("\\s+/\\s+", "/")
                .replaceAll("\\s+/>", "/>");
        return Jsoup.parse(content).text();
    }

    private void handleAbortScan(ScanLog scanLog) {
        scanLog.setThreatLevel("Error");
        scanLog.setComment("Phishing scan aborted");
        ScanLog savedLog = scanLogRepository.save(scanLog);
        notificationService.sendScanLog(scanLog.getMailbox().getEmail(), savedLog.getMailbox().getUserId(), savedLog);
    }
}


