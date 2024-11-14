package com.example.api.service;

import java.util.List;
import com.example.api.model.Mailbox;
import com.example.api.model.User;
import com.example.api.repository.MailboxRepository;
import com.example.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MailboxServiceImpl implements MailboxService {

   private final MailboxRepository mailboxRepository;
   private final UserRepository userRepository;

    @Autowired
    public MailboxServiceImpl(MailboxRepository mailboxRepository, UserRepository userRepository) {
        this.mailboxRepository = mailboxRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Mailbox> getUserMailboxes(long userId) {
        return mailboxRepository.findByUserId(userId);
    }

    @Override
    public Mailbox getMailboxById(long theId) {
        return mailboxRepository.findById(theId).orElseThrow(() -> new RuntimeException("Did not find mailbox id - " + theId));
    }

    @Override
    public Mailbox addMailbox(Mailbox mailbox, long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Did not find user id - " + userId));
        mailbox.setUser(user);
        return mailboxRepository.save(mailbox);
    }

    @Override
    public void deleteMailbox(long theId) {
        mailboxRepository.deleteById(theId);
    }
}
