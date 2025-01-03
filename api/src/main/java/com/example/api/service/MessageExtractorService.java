package com.example.api.service;

import javax.mail.Message;

public interface MessageExtractorService {

    void performPhishingScan(Message message, String email);
}
