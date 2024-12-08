package com.example.api.service;

import javax.mail.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.exception.EmailsFetchingException;
import com.example.api.model.Email;
import com.example.api.repository.EmailRepository;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final EmailRepository emailRepository;

    private EmailServiceImpl(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @Override
    public List<Email> getEmailsForMailbox(long mailboxId) {
        return emailRepository.findByMailboxId(mailboxId);
    }

    @Override
    public Email getEmailById(long theId) {
        return emailRepository.findById(theId).orElseThrow(() -> new RuntimeException("Did not find email id - " + theId));
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
        Properties props = new Properties();
        props.put("mail.store.protocol", config.protocol());
        props.put("mail.imap.host", config.host());
        props.put("mail.imap.port", config.port());
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.trust", "*");
        props.put("mail.imap.auth.plain.disable", "true");

        try {
            Session session = Session.getDefaultInstance(props, null);

            Store store = session.getStore(config.protocol());
            store.connect(config.host(), config.username(), config.password());

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            Message[] messages = inbox.getMessages();

            int startIndex = Math.max(messages.length - config.messageCount(), 0);
            Message[] messagesCount = new Message[config.messageCount()];
            System.arraycopy(messages, startIndex, messagesCount, 0, messagesCount.length);

            List<Email> emails = new ArrayList<>();

            for (Message message : messagesCount) {
                Email email = new Email();
                email.setSubject(message.getSubject());
                email.setContent(message.getContent().toString());
                emails.add(email);
            }

            inbox.close(false);
            store.close();

            return emails;
        } catch (MessagingException | IOException e) {
            throw new EmailsFetchingException("Error while fetching emails", e);
        }
    }
}
