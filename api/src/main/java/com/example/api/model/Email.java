package com.example.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "emails")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String subject;
    private String content;

    @ManyToOne
    @JoinColumn(name = "mailbox_id")
    private Mailbox mailbox;

    @OneToOne(mappedBy = "email", cascade = CascadeType.ALL)
    private ScanResult phishingScanResult;
}
