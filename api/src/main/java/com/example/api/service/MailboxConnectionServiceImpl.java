package com.example.api.service;

import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.model.Mailbox;
import com.example.api.repository.MailboxRepository;
import com.sun.mail.imap.IMAPFolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class MailboxConnectionServiceImpl implements MailboxConnectionService {

    private static final long KEEPALIVE_INTERVAL = 5 * 60 * 1000;
    private static final long RECONNECT_DELAY = 10 * 1000;

    private final MailboxRepository mailboxRepository;
    private final MessageExtractorService messageExtractorService;
    private final ExecutorService executorService;
    private final WebSocketService webSocketService;
    private final ConcurrentHashMap<String, MailboxConnection> mailboxConnections;
    private final ConcurrentHashMap<String, EmailConfigRequest> mailboxConfigs;
    private final Set<String> processedMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

    public MailboxConnectionServiceImpl(MailboxRepository mailboxRepository, MessageExtractorService messageExtractorService,
                                        WebSocketService webSocketService) {
        this.mailboxRepository = mailboxRepository;
        this.messageExtractorService = messageExtractorService;
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

    @Override
    public void startMonitoring(EmailConfigRequest config) {
        try {
            MailboxConnection existingConnection = mailboxConnections.get(config.username());
            if (existingConnection != null && existingConnection.isMonitoring) {
                log.info("Mailbox {} is already being monitored", config.username());
                return;
            }

            Mailbox mailbox = mailboxRepository.findByEmail(config.username())
                    .orElseThrow(() -> new RuntimeException("Mailbox not found"));

            MailboxConnection connection = connectToMailbox(config, mailbox);
            connection.isMonitoring = true;
            connection.shouldReconnect = true;

            mailboxConnections.put(config.username(), connection);
            mailboxConfigs.put(config.username(), config);

            executorService.submit(() -> monitorMailbox(config, connection));
        } catch (Exception e) {
            log.error("Error starting mailbox monitoring for {}: {}", config.username(), e.getMessage());
            throw new EmailsFetchingException("Failed to start monitoring", e);
        }
    }


    private void monitorMailbox(EmailConfigRequest config, MailboxConnection connection) {
        try {
            setupMessageListener(connection, config.username());

            while (connection.isMonitoring && connection.shouldReconnect) {
                try {
                    if (!isConnectionValid(connection)) {
                        throw new MessagingException("Connection is no longer valid");
                    }

                    IMAPFolder imapFolder = (IMAPFolder) connection.inbox;

                    imapFolder.idle(false); // false = nie blokuj wątku na zawsze
                    connection.updateLastActivityTime();

                    Thread.sleep(100);

                } catch (Exception e) {
                    log.error("Error in mailbox monitoring for {}: {}", config.username(), e.getMessage());
                    handleConnectionError(config, connection);
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            log.error("Fatal error in mailbox monitoring: ", e);
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
                String messageId = getMessageId(message, email);
                if (processedMessageIds.add(messageId)) {
                    messageExtractorService.performPhishingScan(message, email);
                } else {
                    log.debug("Skipping duplicate message with ID: {}", messageId);
                }
            } catch (MessagingException e) {
                log.error("Error processing message: {}", e.getMessage());
            }
        }
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
    public Map<String, Boolean> getMailboxConnectionStates() {
        Map<String, Boolean> connectionStates = new HashMap<>();
        mailboxConnections.forEach((email, connection) -> {
            boolean isConnected = isConnectionValid(connection) && connection.isMonitoring;
            connectionStates.put(email, isConnected);
        });
        log.info("Mailbox connection states: {}", connectionStates);
        return connectionStates;
    }

    @Override
    public void stopAllMailboxMonitoring() {
        mailboxConnections.forEach((email, _) -> stopMailboxMonitoring(email));
        webSocketService.disconnectAll();
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
            processedMessageIds.clear();
        }
    }

    @PreDestroy
    public void cleanup() {
        stopAllMailboxMonitoring();
        executorService.shutdown();
    }
}