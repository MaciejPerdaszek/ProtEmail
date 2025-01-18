package com.example.api.config;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.TokenRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Auth0Config {

    @Value("${okta.oauth2.issuer}")
    private String domain;

    @Value("${oauth2.management.client-id}")
    private String clientId;

    @Value("${oauth2.management.client-secret}")
    private String clientSecret;

    @Bean
    public ManagementAPI managementAPI() throws Auth0Exception {
        AuthAPI authAPI = AuthAPI.newBuilder(domain, clientId, clientSecret)
                .build();

        TokenRequest tokenRequest = authAPI.requestToken( domain + "api/v2/");
        TokenHolder holder = tokenRequest.execute().getBody();
        String accessToken = holder.getAccessToken();

        return ManagementAPI.newBuilder(domain, accessToken)
                .build();
    }
}