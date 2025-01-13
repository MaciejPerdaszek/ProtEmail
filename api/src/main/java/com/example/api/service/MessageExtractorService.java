package com.example.api.service;

import javax.mail.Message;
import com.example.api.dto.EmailContent;

public interface MessageExtractorService {

    void performPhishingScan(EmailContent emailContent);
}
