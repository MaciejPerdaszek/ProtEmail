package com.example.api.service;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Autowired
    public MessageExtractorServiceImpl(ScanLogRepository scanLogRepository, MailboxRepository mailboxRepository) {
        this.scanLogRepository = scanLogRepository;
        this.mailboxRepository = mailboxRepository;
    }

    @Override
    public void performPhishingScan(Message message, String email) {
        try {
            String content = getMessageContent(message);
            List<String> urls = extractUrls(content);

            ScanLog scanLog = createScanLog(message, email, content);
            processScanResults(scanLog, urls);

            log.info("Scan completed for email from: {} subject: {}", scanLog.getSender(), scanLog.getSubject());
        } catch (Exception e) {
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
        return urls;
    }

    private ScanLog createScanLog(Message message, String email, String content) throws MessagingException {
        ScanLog scanLog = new ScanLog();
        scanLog.setSender(extractSender(message));
        scanLog.setSubject(message.getSubject());
        scanLog.setContent(content);
        scanLog.setScanDate(new Date());
        scanLog.setMailbox(mailboxRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Mailbox not found: " + email)));
        scanLog.setScanStatus("Pending");
        scanLog.setComment("URL scan in progress");
        return scanLogRepository.save(scanLog);
    }

    private String extractSender(Message message) throws MessagingException {
        String from = message.getFrom()[0].toString();
        Matcher matcher = Pattern.compile("<(.+?)>").matcher(from);
        return matcher.find() ? matcher.group(1) : from;
    }

    private void processScanResults(ScanLog scanLog, List<String> urls) {
        scanLog.setScanStatus("Completed");
        scanLog.setComment("URLs " + urls);
        scanLogRepository.save(scanLog);
    }
    private String getMessageContent(Message message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof Multipart) {
            return handleMultipart((Multipart) content);
        }
        return content.toString();
    }

    private String handleMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.getContentType().toLowerCase().startsWith("text/plain")) {
                contentBuilder.append(bodyPart.getContent().toString());
            }
        }
        return contentBuilder.toString();
    }
}


