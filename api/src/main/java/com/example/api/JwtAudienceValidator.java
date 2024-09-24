package com.example.api;

import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String allowedAudiences;

    public JwtAudienceValidator(String allowedAudiences) {
        this.allowedAudiences = allowedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (audience.stream().anyMatch(allowedAudiences::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
