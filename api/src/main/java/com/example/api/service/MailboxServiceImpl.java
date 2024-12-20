package com.example.api.service;

import java.util.List;
import com.example.api.model.Mailbox;
import com.example.api.model.User;
import com.example.api.repository.MailboxRepository;
import com.example.api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailboxServiceImpl implements MailboxService {

   private final MailboxRepository mailboxRepository;
   private final UserRepository userRepository;

    @Autowired
    public MailboxServiceImpl(MailboxRepository mailboxRepository, UserRepository userRepository) {
        this.mailboxRepository = mailboxRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Mailbox> getUserMailboxes(String userId) {
        return mailboxRepository.findByUserId(userId);
    }

    @Override
    public Mailbox getMailboxById(long theId) {
        return mailboxRepository.findById(theId).orElseThrow(() -> new RuntimeException("Did not find mailbox id - " + theId));
    }

    @Override
    public Mailbox updateMailbox(Mailbox mailbox, String userId, Long mailboxId) {  // Renamed for clarity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Did not find user id - " + userId));
        mailbox.setId(mailboxId);

        if (mailbox.getId() == null) {
            throw new IllegalArgumentException("Mailbox ID cannot be null for update operation");
        }

        Mailbox existingMailbox = mailboxRepository.findById(mailbox.getId())
                .orElseThrow(() -> new RuntimeException("Did not find mailbox id - " + mailbox.getId()));

        existingMailbox.setEmail(mailbox.getEmail());
        existingMailbox.setPassword(mailbox.getPassword());
        existingMailbox.setType(mailbox.getType());
        existingMailbox.setUser(user);

        return mailboxRepository.save(existingMailbox);
    }

    public Mailbox addMailbox(Mailbox mailbox, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Did not find user id - " + userId));

        if (mailbox.getId() != null) {
            throw new IllegalArgumentException("New mailbox should not have an ID");
        }

        mailbox.setUser(user);
        return mailboxRepository.save(mailbox);
    }

    @Override
    public void deleteMailbox(long theId) {
        mailboxRepository.deleteById(theId);
    }
}
