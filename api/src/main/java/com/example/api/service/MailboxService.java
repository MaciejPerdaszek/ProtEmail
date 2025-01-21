package com.example.api.service;

import java.util.List;
import com.example.api.model.Mailbox;

public interface MailboxService {

    List<Mailbox> getUserMailboxes(String userId);

    Mailbox addMailbox(Mailbox mailbox, String userId);

    Mailbox updateMailbox(Mailbox mailbox, String userId, Long mailboxId);

    void deleteMailbox(long theId);
}
