package com.example.api.service;

import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.model.Email;
import com.example.api.model.Mailbox;
import com.example.api.model.ScanLog;
import com.example.api.repository.MailboxRepository;
import com.sun.mail.imap.IMAPFolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final long KEEPALIVE_INTERVAL = 5 * 60 * 1000;
    private static final long RECONNECT_DELAY = 10 * 1000;

    private final MailboxRepository mailboxRepository;
    private final ScanLogService scanLogService;
    private final ExecutorService executorService;
    private final WebSocketService webSocketService;
    private final ConcurrentHashMap<String, MailboxConnection> mailboxConnections;
    private final ConcurrentHashMap<String, EmailConfigRequest> mailboxConfigs;

    private static class MailboxConnection {
        private Store store;
        private Folder inbox;
        private volatile boolean isMonitoring;
        private volatile boolean shouldReconnect;
        private long lastActivityTime;

        public MailboxConnection(Store store, Folder inbox) {
            this.store = store;
            this.inbox = inbox;
            this.isMonitoring = false;
            this.shouldReconnect = true;
            this.lastActivityTime = System.currentTimeMillis();
        }

        public void updateLastActivityTime() {
            this.lastActivityTime = System.currentTimeMillis();
        }

        public boolean isConnectionStale() {
            return System.currentTimeMillis() - lastActivityTime > KEEPALIVE_INTERVAL;
        }
    }

    public EmailServiceImpl(MailboxRepository mailboxRepository, ScanLogService scanLogService,
                            WebSocketService webSocketService) {
        this.mailboxRepository = mailboxRepository;
        this.scanLogService = scanLogService;
        this.webSocketService = webSocketService;
        this.executorService = Executors.newCachedThreadPool();
        this.mailboxConnections = new ConcurrentHashMap<>();
        this.mailboxConfigs = new ConcurrentHashMap<>();
    }

    private MailboxConnection connectToMailbox(EmailConfigRequest config, Mailbox mailbox)
            throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", config.protocol());
        props.put("mail.imap.host", config.host());
        props.put("mail.imap.port", config.port());
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.trust", "*");
        props.put("mail.imap.auth.plain.disable", "true");
        props.put("mail.imap.keepalive", "true");

        Session session = Session.getDefaultInstance(props, null);
        //session.setDebug(true);
        Store store = session.getStore(config.protocol());
        store.connect(config.host(), config.username(), mailbox.getPassword());

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        return new MailboxConnection(store, inbox);
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

    public void startMonitoring(EmailConfigRequest config)  {
        try {
            Mailbox mailbox = mailboxRepository.findByEmail(config.username())
                    .orElseThrow(() -> new RuntimeException("Mailbox not found"));

            MailboxConnection connection = connectToMailbox(config, mailbox);

            mailboxConnections.put(config.username(), connection);
            mailboxConfigs.put(config.username(), config);
            connection = mailboxConnections.get(config.username());
            if (connection == null || connection.isMonitoring) {
                return;
            }

            connection.isMonitoring = true;
            connection.shouldReconnect = true;
            MailboxConnection finalConnection = connection;
            executorService.submit(() -> monitorMailbox(config, finalConnection));
        } catch (Exception e) {
            log.error("Error starting mailbox monitoring for {}: {}", config.username(), e.getMessage());
            throw new EmailsFetchingException("Failed to start monitoring", e);
        }
    }

    private void monitorMailbox(EmailConfigRequest config, MailboxConnection connection) {
        while (connection.isMonitoring && connection.shouldReconnect) {
            try {
                log.info("Monitoring mailbox: {}", config.username());
                setupMessageListener(connection, config.username());

                while (connection.isMonitoring) {
                    if (!isConnectionValid(connection)) {
                        throw new MessagingException("Connection is no longer valid");
                    }

                    IMAPFolder imapFolder = (IMAPFolder) connection.inbox;

                    // Set a timeout for IDLE command
                    imapFolder.idle(true);
                    connection.updateLastActivityTime();

                    log.info("Waiting for new emails for mailbox: {}", config.username());

                    // Perform keepalive check
                    if (connection.isConnectionStale()) {
                        log.info("Performing keepalive for mailbox: {}", config.username());
                        connection.inbox.getMessageCount();
                        connection.updateLastActivityTime();
                    }
                }
            } catch (Exception e) {
                log.error("Error in mailbox monitoring for {}: {}", config.username(), e.getMessage());
                handleConnectionError(config, connection);
            }
        }
    }

    private boolean isConnectionValid(MailboxConnection connection) {
        try {
            return connection.store.isConnected() &&
                    connection.inbox != null &&
                    connection.inbox.isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    private void handleConnectionError(EmailConfigRequest config, MailboxConnection connection) {
        try {
            cleanupConnection(connection);

            if (connection.isMonitoring && connection.shouldReconnect) {

                Thread.sleep(RECONNECT_DELAY);
                reconnectMailbox(config);
            }
        } catch (Exception e) {
            log.error("Error handling connection failure: {}", e.getMessage());
        }
    }

    private void cleanupConnection(MailboxConnection connection) {
        try {
            if (connection.inbox != null && connection.inbox.isOpen()) {
                connection.inbox.close(false);
            }
            if (connection.store != null && connection.store.isConnected()) {
                connection.store.close();
            }
        } catch (MessagingException e) {
            log.error("Error cleaning up connection: {}", e.getMessage());
        }
    }

    private void reconnectMailbox(EmailConfigRequest config) {
        try {
            Mailbox mailbox = mailboxRepository.findByEmail(config.username())
                    .orElseThrow(() -> new RuntimeException("Mailbox not found"));

            MailboxConnection newConnection = connectToMailbox(config, mailbox);
            mailboxConnections.put(config.username(), newConnection);
            newConnection.isMonitoring = true;
            newConnection.shouldReconnect = true;

            log.info("Reconnected to mailbox: {}", config.username());
        } catch (Exception e) {
            log.error("Failed to reconnect: {}", e.getMessage());
            throw new EmailsFetchingException("Failed to reconnect", e);
        }
    }

    private void setupMessageListener(MailboxConnection connection, String email) {
        try {
            connection.inbox.addMessageCountListener(new MessageCountAdapter() {
                @Override
                public void messagesAdded(MessageCountEvent event) {
                    handleNewMessages(event, email);
                    connection.updateLastActivityTime();
                }
            });
        } catch (Exception e) {
            log.error("Error setting up message listener: {}", e.getMessage());
            throw new EmailsFetchingException("Failed to setup message listener", e);
        }
    }

    private void handleNewMessages(MessageCountEvent event, String email) {
        Message[] messages = event.getMessages();

        for (Message message : messages) {
            try {
                ScanLog scanLog = new ScanLog();
                scanLog.setSender(message.getFrom()[0].toString());
                scanLog.setSubject(message.getSubject());
                scanLog.setContent(getMessageContent(message));
                scanLog.setScanDate(new Date());
                scanLog.setScanStatus("Pending");
                scanLog.setComment("Pending");
                scanLog.setMailbox(mailboxRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Mailbox not found")));
                scanLogService.saveScanLog(scanLog);
                log.info("New email received: sender {}  subject {}  content {}", scanLog.getSender(), scanLog.getSubject() , scanLog.getContent());
                //webSocketService.sendMessage(config.username(), "New email received: " + subject);
            } catch (MessagingException e) {
                log.error("Error processing new message: {}", e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Map<String, Boolean> getMailboxConnectionStates() {
        Map<String, Boolean> connectionStates = new HashMap<>();
        mailboxConnections.forEach((email, connection) -> {
            boolean isConnected = isConnectionValid(connection) && connection.isMonitoring;
            connectionStates.put(email, isConnected);
        });
        log.info("Mailbox connection states: {}", connectionStates);
        return connectionStates;
    }

    public void stopMonitoring() {
        mailboxConnections.forEach((email, connection) -> {
            stopMailboxMonitoring(email);
        });
    }

    @Override
    public void stopMailboxMonitoring(String email) {
        MailboxConnection connection = mailboxConnections.get(email);
        if (connection != null) {
            connection.isMonitoring = false;
            connection.shouldReconnect = false;
            cleanupConnection(connection);
            mailboxConnections.remove(email);
            mailboxConfigs.remove(email);
        }
    }

    @PreDestroy
    public void cleanup() {
        stopMonitoring();
        executorService.shutdown();
    }
}