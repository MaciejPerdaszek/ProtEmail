package com.example.api.controller;

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

    @PostMapping("/change-password")
    public ResponseEntity<?> requestPasswordChange(@AuthenticationPrincipal OAuth2User user) {
        try {
            String userId = user.getAttribute("sub");
            String email = user.getAttribute("email");

            if (userId == null || email == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            authService.updatePassword(userId, email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to initiate password change for user: {}", user.getAttribute("email"), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    @AuthenticationPrincipal(expression = "idToken") OidcIdToken idToken) {
        // send logout URL to client so they can initiate logout
        String logoutUrl = this.registration.getProviderDetails()
                .getConfigurationMetadata().get("end_session_endpoint").toString();

        request.getSession(false).invalidate();
        return ResponseEntity.ok().body(new LogoutResponse(logoutUrl, idToken.getTokenValue()));
    }
}
