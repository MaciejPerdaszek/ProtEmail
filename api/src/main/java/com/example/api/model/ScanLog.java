package com.example.api.model;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Table(name = "scan_logs")
public class ScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;

    private String subject;

    private Date scanDate;

    private String threatLevel;

    private String comment;

    @ManyToOne
    @JoinColumn(name = "mailbox_id")
    @JsonBackReference
    private Mailbox mailbox;
}
