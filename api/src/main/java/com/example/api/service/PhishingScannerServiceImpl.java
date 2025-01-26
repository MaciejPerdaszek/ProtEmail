package com.example.api.service;

import com.example.api.exception.AiModelException;
import com.example.api.exception.SafeBrowsingApiException;
import com.example.api.exception.UrlScanApiException;
import com.nimbusds.jose.shaded.gson.*;
import com.nimbusds.jose.shaded.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.api.model.PhishingScanResult;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PhishingScannerServiceImpl implements PhishingScannerService {
    private static final String URLSCAN_SUBMIT_URL = "https://urlscan.io/api/v1/scan/";
    private static final String SAFE_BROWSE_API_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find";
    private static final String URLSCAN_API_URL = "http://ai:8000/analyze-email";
    //private static final String URLSCAN_API_URL = "http://localhost:8000/analyze-email";

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
        log.info("Scanning email from: {} with content: {}", sender, content);
        List<String> threats = new ArrayList<>();
        int riskScore = 0;

        try {
            float probability;
            if (subject.isEmpty()) {
                probability = checkEmailContent(content);
            } else {
                probability = checkEmailContent(subject + " " + content);
            }

            if (probability > 0.9) {
                threats.add("Very high probability of phishing or spam content");
                riskScore += 30;
            } else if (probability > 0.7) {
                threats.add("High probability of phishing or spam content");
                riskScore += 20;
            } else if (probability > 0.5) {
                threats.add("Medium probability of phishing or spam content");
                riskScore += 10;
            } else if (probability > 0.3) {
                threats.add("Low probability of phishing or spam content");
                riskScore += 5;
            } else if (probability == -1) {
                threats.add("AI model check failed");
            }
        } catch (AiModelException e) {
            log.error("AI model check failed", e);
            threats.add("AI content analysis failed");
        }

        for (String url : urls) {
            try {
                if (checkUrlWithSafeBrowsing(url)) {
                    threats.add("URL flagged by Google Safe Browsing: " + url);
                    riskScore += 30;
                }
            } catch (SafeBrowsingApiException e) {
                log.error("Safe Browsing API check failed for URL: {}", url, e);
                threats.add("Safe Browsing check failed for URL: " + url);
            }

            try {
                if (checkUrlWithUrlScan(url)) {
                    threats.add("URL flagged by URLScan.io: " + url);
                    riskScore += 30;
                }
            } catch (UrlScanApiException e) {
                log.error("URLScan.io check failed for URL: {}", url, e);
                threats.add("URLScan check failed for URL: " + url);
            }
        }

        String riskLevel = calculateRiskLevel(riskScore);
        return new PhishingScanResult(riskScore, riskLevel, threats);
    }

    private boolean checkUrlWithSafeBrowsing(String url) {
        try {
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


            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new SafeBrowsingApiException("Safe Browsing API request failed with status: " + response.statusCode());
            }

            boolean hasMatches = response.body().contains("matches");
            log.info("URL {} is {}", url, hasMatches ? "unsafe" : "safe");
            return hasMatches;
        } catch (Exception e) {
            throw new SafeBrowsingApiException("Safe Browsing API error for URL: " + url, e);
        }
    }

    private boolean checkUrlWithUrlScan(String url) {
        try {
            log.info("Checking URL with URLScan.io: {}", url);

            String fullUrl = url.startsWith("http") ? url : "https://" + url;

            String submitRequestBody = """
            {
                "url": "%s",
                "visibility": "private"
            }
            """.formatted(fullUrl);

            HttpRequest submitRequest = HttpRequest.newBuilder()
                    .uri(URI.create(URLSCAN_SUBMIT_URL))
                    .header("Content-Type", "application/json")
                    .header("API-Key", urlscanApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(submitRequestBody))
                    .build();

            HttpResponse<String> submitResponse = httpClient.send(submitRequest, HttpResponse.BodyHandlers.ofString());

            if (submitResponse.statusCode() != 200) {
                throw new UrlScanApiException("URLScan.io submission failed with status: " + submitResponse.statusCode());
            }

            JsonObject submitJsonResponse = parseJsonResponse(submitResponse.body());
            String resultUrl = submitJsonResponse.get("api").getAsString();

            Thread.sleep(11000);

            HttpRequest resultRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resultUrl))
                    .header("API-Key", urlscanApiKey)
                    .GET()
                    .build();

            HttpResponse<String> resultResponse = httpClient.send(resultRequest, HttpResponse.BodyHandlers.ofString());

            if (resultResponse.statusCode() != 200) {
                throw new UrlScanApiException("URLScan.io result fetch failed with status: " + resultResponse.statusCode());
            }

            JsonObject jsonResponse = parseJsonResponse(resultResponse.body());
            JsonObject verdicts = jsonResponse.getAsJsonObject("verdicts").getAsJsonObject("urlscan");
            if (verdicts.has("categories") && !verdicts.getAsJsonArray("categories").isEmpty()) {
                log.info("URL {} has threat categories: {}", url, verdicts.getAsJsonArray("categories"));
                return true;
            }

            log.info("No threats detected for URL: {}", url);
            return false;

        } catch (Exception e) {
            log.error("Error in URLScan.io check for URL {}: {}", url, e.getMessage());
            throw new UrlScanApiException("URLScan.io API error for URL: " + url, e);
        }
    }

    private float checkEmailContent(String content) {
        try {
            log.info("Checking email content with AI model");
            String requestBody = String.format("{\"email_text\": \"%s\"}", content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URLSCAN_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return -1;
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.get("probability").getAsFloat();
        } catch (Exception e) {
            throw new AiModelException("AI model analysis error", e);
        }
    }

    private JsonObject parseJsonResponse(String response) {
        JsonReader reader = new JsonReader(new StringReader(response));
        reader.setLenient(true);
        return JsonParser.parseReader(reader).getAsJsonObject();
    }

    private String calculateRiskLevel(int riskScore) {
        if (riskScore >= 60) return "High";
        if (riskScore >= 30) return "Medium";
        if (riskScore > 0) return "Low";
        return "No risk detected";
    }
}