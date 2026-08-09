package com.jobseekercopilot.googlemapsgateway.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.jobseekercopilot.googlemapsgateway.client.GoogleMapsProviderClient;
import com.jobseekercopilot.googlemapsgateway.config.GoogleMapsProperties;
import com.jobseekercopilot.googlemapsgateway.model.GatewayContracts;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoogleMapsGatewayServiceTest {
    @Test
    void providerIsDisabledByDefaultAndCannotBeCalledAccidentally() {
        GoogleMapsProperties properties = new GoogleMapsProperties(
                false, "", "https://places.invalid", "https://routes.invalid",
                Duration.ofMillis(100), Duration.ofMillis(100), Duration.ofMinutes(10), 10);
        GoogleMapsGatewayService service = new GoogleMapsGatewayService(
                properties, new GoogleSessionTokenStore(properties), mock(GoogleMapsProviderClient.class));

        assertThatThrownBy(() -> service.autocomplete(
                new GatewayContracts.AutocompleteRequest("Hayes", "session", List.of("GB"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }
}
