package com.example.api.service;

import com.example.api.exception.AuthException;
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

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean updatePassword(String userId, String email) {
        try {
            String baseUrl = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
            String url = baseUrl + "/dbconnections/change_password";

            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "client_id", clientId,
                    "email", email,
                    "connection", "Username-Password-Authentication"
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
                return response.isSuccessful();
            }
        } catch (Exception e) {
            throw new AuthException("Failed to initiate password reset", e);
        }
    }
}




