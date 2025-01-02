package com.example.api.model;

import lombok.Data;

@Data
public class Email {
    private String sender;
    private String subject;
    private String content;
}
