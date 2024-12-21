package com.example.api.model;

import java.util.List;
import com.example.api.AESConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "mailboxes")
public class Mailbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Convert(converter = AESConverter.class)
    private String password;

    private String type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    @OneToMany(mappedBy = "mailbox", cascade = CascadeType.ALL)
    private List<Email> emails;

    @OneToMany(mappedBy = "mailbox", cascade = CascadeType.ALL)
    private List<ScanLog> scanLogs;

}
