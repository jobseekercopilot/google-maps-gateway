package com.jobseekercopilot.googlemapsgateway.service;

import com.jobseekercopilot.googlemapsgateway.config.GoogleMapsProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoogleSessionTokenStore {
    private final GoogleMapsProperties properties;
    private final Clock clock;
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();

    @Autowired
    public GoogleSessionTokenStore(GoogleMapsProperties properties) {
        this(properties, Clock.systemUTC());
    }

    GoogleSessionTokenStore(GoogleMapsProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String getOrCreate(String publicSessionId) {
        evictExpired();
        Entry existing = sessions.get(publicSessionId);
        if (existing != null && existing.expiresAt().isAfter(clock.instant())) {
            return existing.token();
        }
        if (sessions.size() >= properties.maximumSessions()) {
            throw new IllegalStateException("Google autocomplete session capacity exceeded");
        }
        Entry created = new Entry(UUID.randomUUID().toString(), clock.instant().plus(properties.sessionTtl()));
        sessions.put(publicSessionId, created);
        return created.token();
    }

    public String take(String publicSessionId) {
        Entry value = sessions.remove(publicSessionId);
        if (value == null || !value.expiresAt().isAfter(clock.instant())) {
            throw new IllegalArgumentException("Google autocomplete session expired or unknown");
        }
        return value.token();
    }

    private void evictExpired() {
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record Entry(String token, Instant expiresAt) { }
}
