package com.aboff.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    
    private JwtProperties jwt;
    
    @Getter
    @Setter
    public static class JwtProperties {
        private String secret;
        private String accessTokenExpiry = "15m";
        private String refreshTokenExpiry = "7d";
    }
}
