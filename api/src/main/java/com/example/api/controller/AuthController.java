package com.example.api.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.example.api.dto.AuthRequest;
import com.example.api.dto.AuthResponse;
import com.example.api.dto.LogoutResponse;
import com.example.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final ClientRegistration registration;

    private final AuthService authService;

    public AuthController(ClientRegistrationRepository registrations, AuthService authService) {
        this.registration = registrations.findByRegistrationId("okta");
        this.authService = authService;
    }

    @GetMapping("/user") public ResponseEntity<?> getUser(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return new ResponseEntity<>("", HttpStatus.OK);
        } else {
            return ResponseEntity.ok().body(user.getAttributes());
        }
    }

    @GetMapping("/token")
    public ResponseEntity<?> getToken(@AuthenticationPrincipal(expression = "idToken") OidcIdToken idToken) {
        if (idToken == null) {
            return new ResponseEntity<>("", HttpStatus.OK);
        } else {
            return ResponseEntity.ok().body(idToken);
        }
    }

    @PostMapping("/change-email")
    public ResponseEntity<AuthResponse> changeEmail(@AuthenticationPrincipal OAuth2User user,
                                                    @RequestBody AuthRequest request) {
        String newEmail = request.newEmail();
        if (newEmail == null || newEmail.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(false, "Email is required", null));
        }

        String userId = user.getAttribute("sub");
        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(false, "User ID not found", null));
        }
        boolean success = authService.updateEmail(userId, newEmail);

        if (success) {
            return ResponseEntity.ok()
                    .body(new AuthResponse(true, "Email updated successfully. " +
                            "Please check your inbox for verification.", Optional.of(true)));
        } else {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(false, "Failed to update email", null));
        }

    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    @AuthenticationPrincipal(expression = "idToken") OidcIdToken idToken) {
        // send logout URL to client so they can initiate logout
        String logoutUrl = this.registration.getProviderDetails()
                .getConfigurationMetadata().get("end_session_endpoint").toString();

        Map<String, String> logoutDetails = new HashMap<>();
        logoutDetails.put("logoutUrl", logoutUrl);
        logoutDetails.put("idToken", idToken.getTokenValue());
        request.getSession(false).invalidate();
        return ResponseEntity.ok().body(logoutDetails);
    }
}
