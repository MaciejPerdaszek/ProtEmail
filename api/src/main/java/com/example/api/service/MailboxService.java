package com.example.api.service;

import java.util.List;
import com.example.api.model.Mailbox;
import com.example.api.model.User;

public interface MailboxService {

    List<Mailbox> getUserMailboxes(long userId);

    Mailbox getMailboxById(long theId);

    Mailbox addMailbox(Mailbox mailbox, long userId);

    void deleteMailbox(long theId);
}
