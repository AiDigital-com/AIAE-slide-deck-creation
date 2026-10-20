package com.aidigital.strategyplanning.external.google.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Activates {@link GoogleWorkspaceClientProperties} binding.
 */
@Configuration
@EnableConfigurationProperties(GoogleWorkspaceClientProperties.class)
public class GoogleWorkspaceClientConfig {
}
