package com.example.api.repository;

import java.util.List;
import java.util.Optional;
import com.example.api.model.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
    List<Mailbox> findByUserId(String userId);
    Optional<Mailbox> findByEmail(String email);
}
