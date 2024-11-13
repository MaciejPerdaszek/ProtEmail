package com.example.api.repository;

import com.example.api.model.PhishingScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhishingScanResultRepository extends JpaRepository<PhishingScanResult, Integer> {
}
