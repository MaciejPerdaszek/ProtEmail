package com.example.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.api.model.PhishingScanResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PhishingScannerServiceImpl implements PhishingScannerService {
    private static final Pattern IP_URL_PATTERN = Pattern.compile("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "verify your account", "confirm your identity", "unusual activity",
            "password expired", "security alert", "account suspended"
    );
    private static final List<String> SUSPICIOUS_DOMAINS = List.of(
            "secure-login", "account-verify", "banking-secure"
    );

    @Value("${google.safebrowsing.api.key}")
    private String apiKey;

    private final HttpClient httpClient;
    private static final String SAFE_BROWSE_API_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find";

    public PhishingScannerServiceImpl() {
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public PhishingScanResult scanEmail(String sender, String subject, String content, List<String> urls) {
        List<String> threats = new ArrayList<>();
        int riskScore = 0;

        // Existing checks
        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (content.toLowerCase().contains(keyword.toLowerCase()) ||
                    subject.toLowerCase().contains(keyword.toLowerCase())) {
                threats.add("Suspicious keyword found: " + keyword);
                riskScore += 10;
            }
        }

        // Check URLs using both existing logic and Google Safe Browsing API
        for (String url : urls) {
            // Existing URL checks
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

            // Google Safe Browsing API check
            try {
                if (checkUrlWithSafeBrowsing(url)) {
                    threats.add("URL flagged by Google Safe Browsing: " + url);
                    riskScore += 30;
                }
            } catch (Exception e) {
                threats.add("Error checking URL with Safe Browsing API: " + e.getMessage());
            }
        }

        // Remaining existing checks
        if (content.toLowerCase().contains("urgent") ||
                content.toLowerCase().contains("immediate action") ||
                subject.toLowerCase().contains("urgent")) {
            threats.add("Urgent language detected");
            riskScore += 10;
        }

        String riskLevel = calculateRiskLevel(riskScore);
        return new PhishingScanResult(riskScore, riskLevel, threats);
    }

    private boolean checkUrlWithSafeBrowsing(String url) throws IOException, InterruptedException {
        log.info("Checking URL with Google Safe Browsing API: {}", url);
        String requestBody = """
            {
                "client": {
                    "clientId": "ProtEmail",
                    "clientVersion": "1.0.0"
                },
                "threatInfo": {
                    "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"],
                    "platformTypes": ["ANY_PLATFORM"],
                    "threatEntryTypes": ["URL"],
                    "threatEntries": [
                        {"url": "%s"}
                    ]
                }
            }
            """.formatted(url);

        log.info("Request body: {}", requestBody);

        String fullUrl = SAFE_BROWSE_API_URL + "?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        log.info("Request headers: {}", request.headers());

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Response status code: {}", response.statusCode());
            log.info("Response headers: {}", response.headers());
            log.info("Response body: {}", response.body());

            if (response.statusCode() != 200) {
                log.error("Error from Safe Browsing API: {} - {}", response.statusCode(), response.body());
                throw new IOException("Safe Browsing API returned status code: " + response.statusCode());
            }

            boolean hasMatches = response.body().contains("matches");
            log.info("URL {} is {}", url, hasMatches ? "unsafe" : "safe");
            return hasMatches;
        } catch (Exception e) {
            log.error("Exception while calling Safe Browsing API", e);
            throw e;
        }
    }

    private String calculateRiskLevel(int riskScore) {
        if (riskScore >= 40) return "High";
        if (riskScore >= 20) return "Medium";
        if (riskScore > 0) return "Low";
        return "No risk detected";
    }
}