package com.aboff.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.defaults")
public class DefaultsProperties {
    
    private String defaultAvatarUrl;
}
