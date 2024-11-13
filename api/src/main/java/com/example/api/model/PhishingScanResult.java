package com.example.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "phishing_scan_results")
public class PhishingScanResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean isPhishing;
    private Double confidenceScore;
    private String details;

    @OneToOne
    @JoinColumn(name = "email_id")
    private Email email;
}
