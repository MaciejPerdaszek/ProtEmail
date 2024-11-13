package com.example.api.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "mailboxes")
public class Mailbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String emailAddress;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "mailbox", cascade = CascadeType.ALL)
    private List<Email> emails;

    @OneToMany(mappedBy = "mailbox", cascade = CascadeType.ALL)
    private List<ScanLog> scanLogs;

}
