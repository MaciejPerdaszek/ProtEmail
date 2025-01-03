package com.example.api.repository;

import java.util.List;
import com.example.api.model.ScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {

    Page<ScanLog> findByMailboxIdIn(List<Long> mailboxIds, Pageable pageable);
}
