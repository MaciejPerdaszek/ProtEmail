package com.example.api.repository;

import java.util.List;
import com.example.api.model.ScanLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {

    List<ScanLog> findByMailboxId(long mailboxId);
}
