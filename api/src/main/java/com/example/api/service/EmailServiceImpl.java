package com.example.api.service;

import java.util.List;
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
}
