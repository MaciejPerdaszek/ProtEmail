package com.example.api.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "scan_logs")
public class ScanLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime scanDate;
    private int totalEmailsScanned;
    private int phishingEmailsDetected;

    @ManyToOne
    @JoinColumn(name = "mailbox_id")
    private Mailbox mailbox;

}
