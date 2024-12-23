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

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final EmailRepository emailRepository;
    private final MailboxRepository mailboxRepository;
    private final ExecutorService executorService;
    private volatile boolean isMonitoring = false;
    private Store store;
    private Folder inbox;

    public EmailServiceImpl(EmailRepository emailRepository, MailboxRepository mailboxRepository) {
        this.emailRepository = emailRepository;
        this.mailboxRepository = mailboxRepository;
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

        try {
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
                email.setContent(message.getContent().toString());
                email.setMailbox(mailbox);
                emails.add(email);
            }

            return emails;
        } catch (MessagingException | IOException e) {
            throw new EmailsFetchingException("Error while fetching emails", e);
        }
    }

    private void startMonitoring(EmailConfigRequest config) {
        if (isMonitoring) {
            return;
        }

        isMonitoring = true;
        executorService.submit(() -> {
            try {
                inbox.addMessageCountListener(new MessageCountAdapter() {
                    @Override
                    public void messagesAdded(MessageCountEvent event) {
                        Message[] messages = event.getMessages();
                        for (Message message : messages) {
                            try {
                                Email email = new Email();
                                email.setSubject(message.getSubject());
                                email.setContent(message.getContent().toString());
                                Mailbox mailbox = mailboxRepository.findByEmail(config.username())
                                        .orElseThrow(() -> new RuntimeException("Mailbox not found"));
                                email.setMailbox(mailbox);
                                emailRepository.save(email);
                            } catch (MessagingException | IOException e) {
                                throw new EmailsFetchingException("Error while processing new email", e);
                            }
                        }
                    }
                });

                while (isMonitoring) {
                    if (!inbox.isOpen()) {
                        inbox.open(Folder.READ_WRITE);
                    }

                    IMAPFolder imapFolder = (IMAPFolder) inbox;
                    // IMAP IDLE command
                    imapFolder.idle();

                    // Check connection every 5 minutes
                    Thread.sleep(300000);
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