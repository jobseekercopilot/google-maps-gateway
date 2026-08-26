FROM eclipse-temurin:17-jre-alpine@sha256:90b7615cb81e3a75f69124fb480e48981c7d56dbc9f32c614d789d3a1c3e32fe

# Refresh runtime OpenSSL to the fixed CVE-2026-14456 build.
RUN apk add --no-cache --upgrade \
    libcrypto3=3.5.8-r0 \
    libssl3=3.5.8-r0 \
    openssl=3.5.8-r0

WORKDIR /app
RUN addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app
COPY --chown=10001:10001 target/google-maps-gateway-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8105
USER 10001:10001
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD ["wget", "--quiet", "--timeout=3", "--tries=1", "--spider", "http://127.0.0.1:8105/actuator/health/readiness"]
ENTRYPOINT ["java", "-jar", "app.jar"]
