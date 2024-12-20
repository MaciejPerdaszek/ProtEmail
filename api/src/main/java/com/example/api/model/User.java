package com.example.api.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name = "users")

public class User {
    @Id
    private String id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Mailbox> mailboxes;
}

