package com.example.api.service;

import javax.mail.*;
import java.util.*;
import java.util.concurrent.*;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.dto.EmailContent;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.model.Mailbox;
import com.example.api.repository.MailboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class MailboxConnectionServiceImpl implements MailboxConnectionService {

    private static final long POLLING_INTERVAL = 60 * 1000; // 1 minute
    private static final int CONNECTION_TIMEOUT = 60000; // 60 seconds
    private final MailboxRepository mailboxRepository;
    private final WebSocketNotificationService notificationService;
    private final MessageExtractorService messageExtractorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService phishingScanExecutor;
    private final BlockingQueue<EmailContent> phishingScanQueue;
    private final ConcurrentHashMap<String, EmailConfigRequest> mailboxConfigs;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pollingTasks;
    private final ConcurrentHashMap<String, Date> lastCheckTimes;
    private final Set<String> processedMessageIds;
    private final Set<String> initialConnectionNotified = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean isRunning = true;


    public MailboxConnectionServiceImpl(MailboxRepository mailboxRepository,
                                        MessageExtractorService messageExtractorService, WebSocketNotificationService notificationService) {
        this.mailboxRepository = mailboxRepository;
        this.notificationService = notificationService;
        this.messageExtractorService = messageExtractorService;
        this.scheduledExecutor = Executors.newScheduledThreadPool(5);
        this.phishingScanExecutor = Executors.newFixedThreadPool(10);
        this.phishingScanQueue = new LinkedBlockingQueue<>();
        this.mailboxConfigs = new ConcurrentHashMap<>();
        this.pollingTasks = new ConcurrentHashMap<>();
        this.lastCheckTimes = new ConcurrentHashMap<>();
        this.processedMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        startPhishingScanWorkers();
    }


    private void startPhishingScanWorkers() {
        int numberOfWorkers = 5;
        for (int i = 0; i < numberOfWorkers; i++) {
            phishingScanExecutor.submit(() -> {
                while (isRunning) {
                    try {
                        EmailContent emailContent = phishingScanQueue.poll(1, TimeUnit.SECONDS);
                        if (emailContent != null) {
                            try {
                                messageExtractorService.performPhishingScan(
                                        emailContent
                                );
                                log.debug("Completed phishing scan for message: {}", emailContent.messageId());
                            } catch (Exception e) {
                                log.error("Error during phishing scan for message {}: {}",
                                        emailContent.messageId(), e.getMessage());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }

    private Properties createMailProperties() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.trust", "*");
        props.put("mail.imap.connectiontimeout", CONNECTION_TIMEOUT);
        props.put("mail.imap.timeout", CONNECTION_TIMEOUT);
        props.put("mail.imap.writetimeout", CONNECTION_TIMEOUT);
        return props;
    }

    private String getMailboxKey(String email, String userId) {
        return email + "_" + userId;
    }

    private void pollMailbox(EmailConfigRequest config) {
        Store store = null;
        Folder inbox = null;
        String mailboxKey = getMailboxKey(config.username(), config.userId());
        try {
            log.info("Starting polling cycle for {}", config.username());

            Mailbox mailbox = mailboxRepository.findByEmailAndUserId(config.username(), config.userId())
                    .orElseThrow(() -> new RuntimeException("Mailbox not found for this user"));

            Session session = Session.getInstance(createMailProperties());
            //session.setDebug(true);
            store = session.getStore(config.protocol());
            try {
                store.connect(config.host(), config.username(), mailbox.getPassword());
            } catch (AuthenticationFailedException e) {
                notificationService.notifyConnectionError(config.username(), config.userId(), "Invalid credentials");
                return;
            }

            if (initialConnectionNotified.add(mailboxKey)) {
                notificationService.notifyConnectionSuccess(config.username(), config.userId());
            }

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -1);
            Date oneMinuteAgo = cal.getTime();

            int messageCount = inbox.getMessageCount();
            int startIndex = Math.max(messageCount - 10, 1);
            Message[] lastMessages = inbox.getMessages(startIndex, messageCount);

            List<Message> recentMessages = new ArrayList<>();
            for (Message message : lastMessages) {
                Date receivedDate = message.getReceivedDate();
                Date sentDate = message.getSentDate();

                if ((receivedDate != null && receivedDate.after(oneMinuteAgo)) ||
                        (sentDate != null && sentDate.after(oneMinuteAgo))) {
                    recentMessages.add(message);
                    log.debug("Found recent message - Received: {}, Sent: {}", receivedDate, sentDate);
                }
            }

            log.info("Found {} messages for {} in last minute", recentMessages.size(), config.username());

            for (Message message : recentMessages) {
                try {
                    String messageId = getMessageId(message, config.username());
                    if (processedMessageIds.add(messageId)) {
                        EmailContent emailContent = EmailContent.fromMessage(
                                message,
                                config.username(),
                                config.userId(),
                                messageId
                        );

                        if (!phishingScanQueue.offer(emailContent)) {
                            log.warn("Unable to add message {} to phishing scan queue - queue might be full", messageId);
                        }
                    }
                } catch (MessagingException e) {
                    log.error("Error processing message: {}", e.getMessage());
                }
            }

            lastCheckTimes.put(config.username(), new Date());
            log.info("Completed polling cycle for {}, queued {} messages",
                    config.username(), recentMessages.size());

        } catch (Exception e) {
            log.error("Error during polling for {}: {}", config.username(), e.getMessage(), e);
            throw new EmailsFetchingException("Failed to poll mailbox", e);
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException e) {
                log.error("Error cleaning up resources: {}", e.getMessage());
            }
        }
    }

    @Override
    public void startMonitoring(EmailConfigRequest config) {
        String mailboxKey = getMailboxKey(config.username(), config.userId());

        if (pollingTasks.containsKey(mailboxKey)) {
            log.info("Mailbox {} for user {} is already being monitored", config.username(), config.userId());
            return;
        }

        mailboxConfigs.put(mailboxKey, config);
        lastCheckTimes.put(mailboxKey, new Date());

        ScheduledFuture<?> pollingTask = scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        pollMailbox(config);
                    } catch (Exception e) {
                        log.error("Error during polling: {}", e.getMessage());
                        stopMailboxMonitoring(config.username(), config.userId());
                        notificationService.notifyConnectionError(config.username(), config.userId(), "Connection dropped by server");
                    }
                },
                0,
                POLLING_INTERVAL,
                TimeUnit.MILLISECONDS
        );

        pollingTasks.put(mailboxKey, pollingTask);
        log.info("Started polling monitoring for mailbox: {} with user {}", config.username(), config.userId());
    }

    private String getMessageId(Message message, String email) throws MessagingException {
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(email).append("_");

        String[] headers = message.getHeader("Message-ID");
        if (headers != null && headers.length > 0) {
            idBuilder.append(headers[0]);
        } else {
            idBuilder.append(message.getSubject())
                    .append("_")
                    .append(message.getSentDate().getTime());

            Address[] from = message.getFrom();
            if (from != null && from.length > 0) {
                idBuilder.append("_").append(from[0].toString());
            }
        }

        return idBuilder.toString();
    }

    @Override
    public Map<String, Boolean> getMailboxConnectionStates(String userId) {
        Map<String, Boolean> states = new HashMap<>();
        pollingTasks.forEach((key, future) -> {
            if (key.endsWith("_" + userId)) {
                String email = key.substring(0, key.lastIndexOf('_'));
                states.put(email, !future.isDone() && !future.isCancelled());
            }
        });
        return states;
    }

    @Override
    public void stopMailboxMonitoring(String email, String userId) {
        String mailboxKey = getMailboxKey(email, userId);
        ScheduledFuture<?> task = pollingTasks.remove(mailboxKey);
        if (task != null) {
            task.cancel(true);
        }
        mailboxConfigs.remove(mailboxKey);
        lastCheckTimes.remove(mailboxKey);
        initialConnectionNotified.remove(mailboxKey);
        log.info("Stopped monitoring mailbox: {} for user {}", email, userId);
    }

    @Override
    public void stopAllMailboxMonitoring(String userId) {
        pollingTasks.forEach((key, task) -> {
            if (key.endsWith("_" + userId)) {
                task.cancel(true);
                String email = key.substring(0, key.lastIndexOf('_'));
                mailboxConfigs.remove(key);
                lastCheckTimes.remove(key);
                initialConnectionNotified.remove(key);
                log.info("Stopped monitoring mailbox: {} for user {}", email, userId);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        isRunning = false;

        Set<String> allUserIds = new HashSet<>();
        pollingTasks.keySet().forEach(key -> {
            String userId = key.substring(key.lastIndexOf('_') + 1);
            allUserIds.add(userId);
        });

        allUserIds.forEach(this::stopAllMailboxMonitoring);

        scheduledExecutor.shutdown();
        phishingScanExecutor.shutdown();

        try {
            if (!scheduledExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            if (!phishingScanExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                phishingScanExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            phishingScanExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}