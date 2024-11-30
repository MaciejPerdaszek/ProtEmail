package com.example.api.service;

import com.example.api.exception.AuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final String clientId;
    private final String clientSecret;
    private final String domain;

    @Autowired
    public AuthServiceImpl(
            @Value("${oauth2.management.client-id}") String clientId,
            @Value("${oauth2.management.client-secret}") String clientSecret,
            @Value("${okta.oauth2.issuer}") String domain) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.domain = domain;
    }

    private String managementApiToken;
    private long tokenExpirationTime;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public synchronized String getManagementApiToken() {
        if (managementApiToken == null || System.currentTimeMillis() >= tokenExpirationTime) {
            refreshManagementApiToken();
        }
        return managementApiToken;
    }

    private void refreshManagementApiToken() {
        try {
            String baseUrl = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
            String url = baseUrl + "/oauth/token";

            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "audience", baseUrl + "/api/v2/",
                    "grant_type", "client_credentials"
            ));

            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new AuthException("Failed to refresh token. Status: " + response.code());
                }

                String responseBody = response.body().string();
                JsonNode node = objectMapper.readTree(responseBody);

                if (!node.has("access_token") || !node.has("expires_in")) {
                    throw new AuthException("Invalid token response format");
                }

                managementApiToken = node.get("access_token").asText();
                int expiresIn = node.get("expires_in").asInt();
                tokenExpirationTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;
            }
        } catch (Exception e) {
            throw new AuthException("Failed to refresh Management API token", e);
        }
    }

    @Override
    public boolean updateEmail(String userId, String email) {
        try {
            String baseUrl = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
            String url = baseUrl + "/api/v2/users/" + userId;

            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "email", email,
                    "email_verified", false
            ));

            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json")
            );

            String token = "Bearer " + getManagementApiToken();

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", token)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
}




