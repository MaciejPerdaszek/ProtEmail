package com.example.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import com.example.api.model.PhishingScanResult;
import org.springframework.stereotype.Service;

@Service
public class PhishingScannerServiceImpl implements PhishingScannerService {
    private static final Pattern IP_URL_PATTERN = Pattern.compile("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "verify your account", "confirm your identity", "unusual activity",
            "password expired", "security alert", "account suspended"
    );
    private static final List<String> SUSPICIOUS_DOMAINS = List.of(
            "secure-login", "account-verify", "banking-secure"
    );

    @Override
    public PhishingScanResult scanEmail(String sender, String subject, String content, List<String> urls) {
        List<String> threats = new ArrayList<>();
        int riskScore = 0;

        // Check for suspicious keywords
        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (content.toLowerCase().contains(keyword.toLowerCase()) ||
                    subject.toLowerCase().contains(keyword.toLowerCase())) {
                threats.add("Suspicious keyword found: " + keyword);
                riskScore += 10;
            }
        }

        // Check URLs
        for (String url : urls) {
            if (IP_URL_PATTERN.matcher(url).find()) {
                threats.add("IP-based URL detected: " + url);
                riskScore += 20;
            }

            for (String suspiciousDomain : SUSPICIOUS_DOMAINS) {
                if (url.toLowerCase().contains(suspiciousDomain.toLowerCase())) {
                    threats.add("Suspicious domain found: " + url);
                    riskScore += 15;
                }
            }
        }

        // Check for urgent language
        if (content.toLowerCase().contains("urgent") ||
                content.toLowerCase().contains("immediate action") ||
                subject.toLowerCase().contains("urgent")) {
            threats.add("Urgent language detected");
            riskScore += 10;
        }

        String riskLevel = calculateRiskLevel(riskScore);
        return new PhishingScanResult(riskScore, riskLevel, threats);
    }

    private String calculateRiskLevel(int riskScore) {
        if (riskScore >= 40) return "High";
        if (riskScore >= 20) return "Medium";
        if (riskScore > 0) return "Low";
        return "No risk detected";
    }
}
