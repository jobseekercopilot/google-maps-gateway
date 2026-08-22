package com.jobseekercopilot.googlemapsgateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobseekercopilot.googlemapsgateway.config.GoogleMapsProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ContractAndDisabledStartupTest {
    @Autowired
    private GoogleMapsProperties properties;

    @Test
    void startsDisabledWithoutAGoogleCredential() {
        assertThat(properties.enabled()).isFalse();
        assertThat(properties.apiKey()).isBlank();
    }

    @Test
    void producerContractMatchesItsReviewedChecksum() throws Exception {
        byte[] contract = Files.readAllBytes(Path.of("api/openapi.yaml"));
        String expected = Files.readString(Path.of("api/SHA256SUMS")).split("\\s+")[0];
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contract)))
                .isEqualTo(expected);
        assertThat(new String(contract, java.nio.charset.StandardCharsets.UTF_8))
                .contains("version: 1.0.0");
    }
}
