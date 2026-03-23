package com._6.CourseManagerment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT Configuration Properties
 * Maps jwt.secret and jwt.expiration from application.properties
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expiration;
}
