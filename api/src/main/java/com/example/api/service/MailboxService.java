package com.example.api.service;

import java.util.List;
import com.example.api.model.Mailbox;
import com.example.api.model.User;

public interface MailboxService {

    List<Mailbox> getUserMailboxes(String userId);

    Mailbox getMailboxById(long theId);

    Mailbox addMailbox(Mailbox mailbox, String userId);

    Mailbox updateMailbox(Mailbox mailbox, String userId, Long mailboxId);

    void deleteMailbox(long theId);
}
