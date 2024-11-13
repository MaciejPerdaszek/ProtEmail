package com.example.api.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name = "users")

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Mailbox> mailboxes;
}

