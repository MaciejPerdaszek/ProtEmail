package com.example.api.repository;

import java.util.List;
import com.example.api.model.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
    List<Mailbox> findByUserId(long userId);
}
