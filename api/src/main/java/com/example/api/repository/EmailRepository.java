package com.example.api.repository;

import java.util.List;
import com.example.api.model.Email;
import com.example.api.model.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<Email, Long> {

        List<Email> findByMailboxId(long mailboxId);
        List<Email> findByMailbox(Mailbox mailbox);
}
