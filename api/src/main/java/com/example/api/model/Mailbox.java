package com.example.api.model;

import java.util.List;
import com.example.api.config.AESConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "mailboxes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "userId"})
})
public class Mailbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Convert(converter = AESConverter.class)
    private String password;

    private String type;

    private String userId;

    @OneToMany(mappedBy = "mailbox", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ScanLog> scanLogs;
}
