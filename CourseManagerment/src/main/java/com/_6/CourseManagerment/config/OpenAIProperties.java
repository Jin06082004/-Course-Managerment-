package com._6.CourseManagerment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIProperties {
    private String apiKey;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
