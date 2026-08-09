package com.jobseekercopilot.googlemapsgateway.service;

import com.jobseekercopilot.googlemapsgateway.client.GoogleMapsProviderClient;
import com.jobseekercopilot.googlemapsgateway.config.GoogleMapsProperties;
import com.jobseekercopilot.googlemapsgateway.model.GatewayContracts;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GoogleMapsGatewayService {
    private final GoogleMapsProperties properties;
    private final GoogleSessionTokenStore sessions;
    private final GoogleMapsProviderClient provider;

    public GoogleMapsGatewayService(
            GoogleMapsProperties properties,
            GoogleSessionTokenStore sessions,
            GoogleMapsProviderClient provider) {
        this.properties = properties;
        this.sessions = sessions;
        this.provider = provider;
    }

    public GatewayContracts.AutocompleteResponse autocomplete(GatewayContracts.AutocompleteRequest request) {
        properties.validateForUse();
        List<String> countries = request.countryCodes().stream().map(String::toUpperCase).distinct().toList();
        if (!countries.equals(List.of("GB"))) {
            throw new IllegalArgumentException("Only GB locations are supported");
        }
        String googleToken = sessions.getOrCreate(request.sessionId());
        return new GatewayContracts.AutocompleteResponse(
                request.sessionId(), provider.autocomplete(request.input(), googleToken, countries));
    }

    public GatewayContracts.PlaceDetails resolve(GatewayContracts.ResolveRequest request) {
        properties.validateForUse();
        return provider.resolve(request.providerReference(), sessions.take(request.sessionId()));
    }

    public GatewayContracts.CommuteMatrixResponse matrix(GatewayContracts.CommuteMatrixRequest request) {
        properties.validateForUse();
        return provider.matrix(request);
    }
}
