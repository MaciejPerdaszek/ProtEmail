package com.example.api;

import com.example.api.exception.AuthException;
import com.example.api.service.AuthServiceImpl;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    @InjectMocks
    private AuthServiceImpl authService;

    private final String userId = "test-user-id";
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                "test-client-id",
                "https://test.auth0.com"
        );
        ReflectionTestUtils.setField(authService, "httpClient", httpClient);
    }

    @Test
    void updatePassword_Success() throws IOException {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(mockResponse);

        boolean result = authService.updatePassword(userId, email);

        assertTrue(result);
        verify(call).execute();
        verify(mockResponse).close();
    }

    @Test
    void updatePassword_Failure() throws IOException {
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenThrow(new IOException("Network error"));

        AuthException exception = assertThrows(AuthException.class, () ->
                authService.updatePassword(userId, email)
        );
        assertEquals("Failed to initiate password reset", exception.getMessage());
        verify(call).execute();
    }

    @Test
    void updatePassword_UnsuccessfulResponse() throws IOException {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(mockResponse);

        boolean result = authService.updatePassword(userId, email);

        assertFalse(result);
        verify(call).execute();
        verify(mockResponse).close();
    }
}