package com.trace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mcp.brave-search")
public class McpConfig {

    private boolean enabled;
    private String apiKey;
}
