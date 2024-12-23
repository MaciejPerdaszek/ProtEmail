package com.example.api.service;

import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.model.Email;
import com.example.api.model.Mailbox;
import com.example.api.repository.EmailRepository;
import com.example.api.repository.MailboxRepository;
import com.sun.mail.imap.IMAPFolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final EmailRepository emailRepository;
    private final MailboxRepository mailboxRepository;
    private final ExecutorService executorService;
    private final WebSocketService webSocketService;
    private volatile boolean isMonitoring = false;
    private Store store;
    private Folder inbox;

    public EmailServiceImpl(EmailRepository emailRepository, MailboxRepository mailboxRepository, WebSocketService webSocketService) {
        this.emailRepository = emailRepository;
        this.mailboxRepository = mailboxRepository;
        this.webSocketService = webSocketService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public List<Email> getEmailsForMailbox(long mailboxId) {
        return emailRepository.findByMailboxId(mailboxId);
    }

    @Override
    public Email getEmailById(long theId) {
        return emailRepository.findById(theId)
                .orElseThrow(() -> new RuntimeException("Did not find email id - " + theId));
    }

    @Override
    public Email saveEmail(Email theEmail) {
        return emailRepository.save(theEmail);
    }

    @Override
    public void deleteEmail(long theId) {
        emailRepository.deleteById(theId);
    }

    @Override
    public List<Email> getEmailsFromMailbox(EmailConfigRequest config) {
        List<Email> emails = fetchEmails(config);
        startMonitoring(config);
        return emails;
    }

    private List<Email> fetchEmails(EmailConfigRequest config) {

        try {
            Mailbox mailbox = mailboxRepository.findByEmail(config.username())
                    .orElseThrow(() -> new RuntimeException("Mailbox not found"));

            Properties props = new Properties();
            props.put("mail.store.protocol", config.protocol());
            props.put("mail.imap.host", config.host());
            props.put("mail.imap.port", config.port());
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.trust", "*");
            props.put("mail.imap.auth.plain.disable", "true");

            log.info("Fetching emails from mailbox: {}", mailbox.getEmail());

            Session session = Session.getDefaultInstance(props, null);
            store = session.getStore(config.protocol());
            store.connect(config.host(), config.username(), mailbox.getPassword());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            Message[] messages = inbox.getMessages();

            log.info("Fetched {} emails", messages.length);

            int startIndex = Math.max(messages.length - config.messageCount(), 0);
            Message[] messagesCount = new Message[Math.min(messages.length - startIndex, config.messageCount())];
            System.arraycopy(messages, startIndex, messagesCount, 0, messagesCount.length);

            List<Email> emails = new ArrayList<>();

            for (Message message : messagesCount) {
                Email email = new Email();
                email.setSubject(message.getSubject());

                // Handle message content properly
                Object content = message.getContent();
                String messageContent;
                if (content instanceof Multipart) {
                    messageContent = handleMultipart((Multipart) content);
                } else {
                    messageContent = content.toString();
                }

                email.setContent(messageContent);
                //email.setMailbox(mailbox);
                emails.add(email);
            }

            log.info("Fetched {} emails", emails.size());
            return emails;

        } catch (MessagingException | IOException e) {
            throw new EmailsFetchingException("Error while fetching emails", e);
        }
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

    private void startMonitoring(EmailConfigRequest config) {
        if (isMonitoring) {
            return;
        }

        log.info("Start monitoring mailbox: {}", config.username());

        isMonitoring = true;
        executorService.submit(() -> {
            try {
                log.info("Monitoring started");
                inbox.addMessageCountListener(new MessageCountAdapter() {
                    @Override
                    public void messagesAdded(MessageCountEvent event) {
                        log.info("New email received");
                        Message[] messages = event.getMessages();

                        log.info("New email received: {}", messages.length);
                        for (Message message : messages) {
                            try {

                                log.info("New email received: {}", message.getSubject());

                                webSocketService.sendMessage(config.username(), "New email received: " + message.getSubject());
                            } catch (MessagingException e) {
                                throw new EmailsFetchingException("Error while processing new email", e);
                            }
                        }
                    }
                });

                while (isMonitoring) {
                    if (!inbox.isOpen()) {
                        inbox.open(Folder.READ_WRITE);
                    }

                    log.info("Monitoring inbox");

                    IMAPFolder imapFolder = (IMAPFolder) inbox;
                    // IMAP IDLE command
                    imapFolder.idle();

                    // Check connection every 5 minutes
//                    Thread.sleep(300000);
                }
            } catch (Exception e) {
                throw new EmailsFetchingException("Error while monitoring inbox", e);
            }
        });
    }

    public void stopMonitoring() {
        isMonitoring = false;
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {
            throw new EmailsFetchingException("Error while stopping monitoring", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        stopMonitoring();
        executorService.shutdown();
    }
}