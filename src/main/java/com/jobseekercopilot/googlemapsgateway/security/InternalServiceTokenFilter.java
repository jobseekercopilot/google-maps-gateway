package com.jobseekercopilot.googlemapsgateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Service-Token";
    private final byte[] expected;

    public InternalServiceTokenFilter(@Value("${google.maps.service-token}") String token) {
        if (token == null || token.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Google Maps gateway token must contain at least 32 bytes");
        }
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/")
                || request.getRequestURI().equals("/actuator")
                || request.getRequestURI().equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String candidate = request.getHeader(HEADER);
        if (candidate == null || !MessageDigest.isEqual(
                expected, candidate.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
