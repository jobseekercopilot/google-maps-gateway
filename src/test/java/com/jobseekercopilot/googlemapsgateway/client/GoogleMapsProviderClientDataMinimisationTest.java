package com.jobseekercopilot.googlemapsgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobseekercopilot.googlemapsgateway.config.GoogleMapsProperties;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GoogleMapsProviderClientDataMinimisationTest {

    @Test
    void placeResolutionRequestsOnlyTheIdentifierAndPostcodeComponents() throws Exception {
        AtomicReference<String> fieldMask = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/places/ChIJ-test", exchange -> {
            fieldMask.set(exchange.getRequestHeaders().getFirst("X-Goog-FieldMask"));
            byte[] response = """
                    {"id":"ChIJ-test","addressComponents":[
                      {"longText":"SW1A 2AA","shortText":"SW1A 2AA","types":["postal_code"]},
                      {"longText":"United Kingdom","shortText":"GB","types":["country"]}
                    ]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            GoogleMapsProperties properties = new GoogleMapsProperties(
                    true, "test-key", baseUrl, baseUrl,
                    Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofMinutes(10), 10);
            GoogleMapsProviderClient client = new GoogleMapsProviderClient(
                    RestClient.builder().baseUrl(baseUrl).build(),
                    RestClient.builder().baseUrl(baseUrl).build(),
                    properties,
                    new SimpleMeterRegistry());

            var result = client.resolve("ChIJ-test", "session-token");

            assertThat(fieldMask.get()).isEqualTo("id,addressComponents");
            assertThat(result.providerReference()).isEqualTo("ChIJ-test");
            assertThat(result.postcode()).isEqualTo("SW1A 2AA");
            assertThat(result.latitude()).isNull();
            assertThat(result.longitude()).isNull();
        } finally {
            server.stop(0);
        }
    }
}
