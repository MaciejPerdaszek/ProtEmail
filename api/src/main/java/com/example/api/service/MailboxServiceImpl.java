package com.example.api.service;

import java.util.List;
import com.example.api.exception.InvalidMailboxException;
import com.example.api.exception.MailboxAlreadyExistsException;
import com.example.api.model.Mailbox;
import com.example.api.repository.MailboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailboxServiceImpl implements MailboxService {

   private final MailboxRepository mailboxRepository;

    @Autowired
    public MailboxServiceImpl(MailboxRepository mailboxRepository) {
        this.mailboxRepository = mailboxRepository;
    }

    @Override
    public List<Mailbox> getUserMailboxes(String userId) {
        return mailboxRepository.findByUserId(userId);
    }

    @Override
    public Mailbox addMailbox(Mailbox mailbox, String userId) {
        if (mailbox.getId() != null) {
            throw new InvalidMailboxException("New mailbox should not have an ID");
        }

        if (mailboxRepository.existsByEmailAndUserId(mailbox.getEmail(), userId)) {
            throw new MailboxAlreadyExistsException("Email " + mailbox.getEmail() + " already exists for this user");
        }

        mailbox.setUserId(userId);
        return mailboxRepository.save(mailbox);
    }

    @Override
    public Mailbox updateMailbox(Mailbox mailbox, String userId, Long mailboxId) {  // Renamed for clarity
        mailbox.setId(mailboxId);

        if (mailbox.getId() == null) {
            throw new IllegalArgumentException("Mailbox ID cannot be null for update operation");
        }

        Mailbox existingMailbox = mailboxRepository.findById(mailbox.getId())
                .orElseThrow(() -> new RuntimeException("Did not find mailbox id - " + mailbox.getId()));

        if (mailbox.getEmail() != null) {
            existingMailbox.setEmail(mailbox.getEmail());
        }
        if (mailbox.getPassword() != null) {
            existingMailbox.setPassword(mailbox.getPassword());
        }
        if (mailbox.getType() != null) {
            existingMailbox.setType(mailbox.getType());
        }

        return mailboxRepository.save(existingMailbox);
    }

    @Override
    public void deleteMailbox(long theId) {
        mailboxRepository.deleteById(theId);
    }
}
