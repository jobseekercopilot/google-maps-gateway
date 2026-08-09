package com.jobseekercopilot.googlemapsgateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.maps")
public record GoogleMapsProperties(
        boolean enabled,
        String apiKey,
        String placesBaseUrl,
        String routesBaseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        Duration sessionTtl,
        int maximumSessions) {

    public void validateForUse() {
        if (!enabled) {
            throw new IllegalStateException("Google Maps provider is disabled");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Google Maps credential is not configured");
        }
    }
}
