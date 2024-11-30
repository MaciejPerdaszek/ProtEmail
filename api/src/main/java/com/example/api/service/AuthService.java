package com.example.api.service;

public interface AuthService {

    boolean updateEmail(String userId, String email);

    String getManagementApiToken();
}
