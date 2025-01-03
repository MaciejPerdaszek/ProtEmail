package com.example.api.service;

import java.util.List;
import com.example.api.model.PhishingScanResult;

public interface PhishingScannerService {

    PhishingScanResult scanEmail(String sender, String subject, String content, List<String> urls);
}
