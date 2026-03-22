package com._6.CourseManagerment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration - CORS and resource handlers
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * Configure CORS settings to allow frontend applications
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
    
    /**
     * Configure static resource handlers
     *
     * FIX: Spring Boot's default already serves:
     *   /static/**  → classpath:/static/
     *   /css/**     → classpath:/static/css/
     *   /js/**      → classpath:/static/js/
     *   /images/**  → classpath:/static/images/
     *
     * The old config registered /js/** → classpath:/js/ (WRONG — directory doesn't exist),
     * which overrode Spring Boot's default and broke ALL JS loading.
     * Remove redundant / conflicting handlers; let Spring Boot defaults handle everything.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // No-op: rely on Spring Boot auto-configured static resource handlers.
        // Explicit handlers are only needed for non-standard paths.
    }
}
