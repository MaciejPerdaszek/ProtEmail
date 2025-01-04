package com.example.api.service;

import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
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
    private static final String URLSCAN_SUBMIT_URL = "https://urlscan.io/api/v1/scan/";
    private static final String SAFE_BROWSE_API_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find";

    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "verify your account", "confirm your identity", "unusual activity",
            "password expired", "security alert", "account suspended"
    );

    @Value("${google.safebrowsing.api.key}")
    private String apiKey;

    @Value("${urlscan.io.api.key}")
    private String urlscanApiKey;

    private final HttpClient httpClient;


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

        for (String url : urls) {

            // Google Safe Browsing API check
            try {
                if (checkUrlWithSafeBrowsing(url)) {
                    threats.add("URL flagged by Google Safe Browsing: " + url);
                    riskScore += 30;
                }
            } catch (Exception e) {
                log.error("Error checking URL with Safe Browsing API", e);
            }

            // URLScan.io check
            try {
                if (checkUrlWithUrlScan(url)) {
                    threats.add("URL flagged by URLScan.io: " + url);
                    riskScore += 30;
                }
            } catch (Exception e) {
                log.error("Error checking URL with URLScan.io API", e);
            }
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

        String fullUrl = SAFE_BROWSE_API_URL + "?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

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

    private boolean checkUrlWithUrlScan(String url) throws IOException, InterruptedException {
        log.info("Checking URL with URLScan.io: {}", url);

        String submitRequestBody = """
            {
                "url": "%s",
                "visibility": "private"
            }
            """.formatted(url);

        HttpRequest submitRequest = HttpRequest.newBuilder()
                .uri(URI.create(URLSCAN_SUBMIT_URL))
                .header("Content-Type", "application/json")
                .header("API-Key", urlscanApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(submitRequestBody))
                .build();

        HttpResponse<String> submitResponse = httpClient.send(submitRequest, HttpResponse.BodyHandlers.ofString());

        if (submitResponse.statusCode() != 200) {
            log.error("Error submitting URL to URLScan.io: {} - {}", submitResponse.statusCode(), submitResponse.body());
            throw new IOException("URLScan.io API returned status code: " + submitResponse.statusCode());
        }

        String result = extractResultFromResponse(submitResponse.body());
        Thread.sleep(15000);

        HttpRequest resultRequest = HttpRequest.newBuilder()
                .uri(URI.create(result))
                .header("API-Key", urlscanApiKey)
                .GET()
                .build();

        HttpResponse<String> resultResponse = httpClient.send(resultRequest, HttpResponse.BodyHandlers.ofString());

        if (resultResponse.statusCode() != 200) {
            log.error("Error getting results from URLScan.io: {} - {}", resultResponse.statusCode(), resultResponse.body());
            throw new IOException("URLScan.io API returned status code: " + resultResponse.statusCode());
        }

        boolean isMalicious = analyzeScanResults(resultResponse.body());
        log.info("URL {} is {}", url, isMalicious ? "potentially malicious" : "likely safe");
        return isMalicious;
    }

    private String extractResultFromResponse(String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

        if (jsonObject.has("result")) {
            return jsonObject.get("result").getAsString();
        } else {
            throw new IllegalArgumentException("Field 'result' not found in response body");
        }
    }

    private boolean analyzeScanResults(String results) {
        // Check for common malicious indicators in URLScan.io results
        return results.contains("malicious") ||
                results.contains("phishing") ||
                results.contains("suspicious") ||
                results.contains("blacklisted");
    }

    private String calculateRiskLevel(int riskScore) {
        if (riskScore >= 40) return "High";
        if (riskScore >= 20) return "Medium";
        if (riskScore > 0) return "Low";
        return "No risk detected";
    }
}