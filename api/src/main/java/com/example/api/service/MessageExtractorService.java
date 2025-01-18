package com.example.api.service;

import com.example.api.dto.EmailContent;

public interface MessageExtractorService {

    void performPhishingScan(EmailContent emailContent);
}
