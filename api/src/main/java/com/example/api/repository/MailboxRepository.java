package com.example.api.repository;

import com.example.api.model.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailboxRepository extends JpaRepository<Mailbox, Integer> {
}
