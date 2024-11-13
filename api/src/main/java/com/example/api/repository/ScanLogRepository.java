package com.example.api.repository;

import com.example.api.model.ScanLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanLogRepository extends JpaRepository<ScanLog, Integer> {
}
