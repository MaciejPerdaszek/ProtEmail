package com.example.api.service;

import javax.mail.MessagingException;
import java.util.List;
import com.example.api.dto.EmailConfigRequest;
import com.example.api.model.Email;

public interface EmailService {

    List<Email> getEmailsForMailbox(long mailboxId);

    Email getEmailById(long theId);

    Email saveEmail(Email theEmail);

    void deleteEmail(long theId);

    List<Email> getEmailsFromMailbox(EmailConfigRequest config);

    void startMonitoring(EmailConfigRequest config);

    void stopMailboxMonitoring(String email);
}
