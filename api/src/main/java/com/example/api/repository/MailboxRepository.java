package com.example.api.repository;

import java.util.List;
import java.util.Optional;
import com.example.api.model.Mailbox;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailboxRepository extends JpaRepository<Mailbox, Long> {

    List<Mailbox> findByUserId(String userId);

    Optional<Mailbox> findByEmail(String email);

    @Transactional
    void deleteByUserId(String userId);

    boolean existsByEmailAndUserId(String email, String userId);

    Optional<Mailbox> findByEmailAndUserId(String email, String userId);
}
