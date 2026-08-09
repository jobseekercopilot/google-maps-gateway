package com.jobseekercopilot.googlemapsgateway.client;

import com.jobseekercopilot.googlemapsgateway.config.GoogleMapsProperties;
import com.jobseekercopilot.googlemapsgateway.model.GatewayContracts;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleMapsProviderClient {
    private static final String AUTOCOMPLETE_FIELD_MASK =
            "suggestions.placePrediction.placeId,suggestions.placePrediction.text," +
            "suggestions.placePrediction.structuredFormat,suggestions.placePrediction.types";
    private static final String DETAILS_FIELD_MASK = "id,addressComponents";
    private static final String MATRIX_FIELD_MASK =
            "originIndex,destinationIndex,status,condition,distanceMeters,duration";
    private static final BigDecimal METERS_PER_MILE = new BigDecimal("1609.344");

    private final RestClient places;
    private final RestClient routes;
    private final GoogleMapsProperties properties;
    private final MeterRegistry meters;

    public GoogleMapsProviderClient(
            @Qualifier("placesRestClient") RestClient places,
            @Qualifier("routesRestClient") RestClient routes,
            GoogleMapsProperties properties,
            MeterRegistry meters) {
        this.places = places;
        this.routes = routes;
        this.properties = properties;
        this.meters = meters;
    }

    public List<GatewayContracts.Suggestion> autocomplete(String input, String token, List<String> countries) {
        properties.validateForUse();
        long started = System.nanoTime();
        try {
            var response = places.post()
                    .uri("/v1/places:autocomplete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", AUTOCOMPLETE_FIELD_MASK)
                    .body(new GoogleProviderDtos.AutocompleteProviderRequest(
                            input, token, countries.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList(),
                            List.of("(regions)"), "en", "gb"))
                    .retrieve()
                    .body(GoogleProviderDtos.AutocompleteProviderResponse.class);
            List<GatewayContracts.Suggestion> suggestions = response == null || response.suggestions() == null
                    ? List.of()
                    : response.suggestions().stream()
                            .map(GoogleProviderDtos.ProviderSuggestion::placePrediction)
                            .filter(Objects::nonNull)
                            .map(this::suggestion)
                            .filter(Objects::nonNull)
                            .limit(5)
                            .toList();
            record("places_autocomplete", "success", started, suggestions.size());
            return suggestions;
        } catch (RuntimeException exception) {
            record("places_autocomplete", "failure", started, 0);
            throw exception;
        }
    }

    public GatewayContracts.PlaceDetails resolve(String placeId, String token) {
        properties.validateForUse();
        long started = System.nanoTime();
        try {
            var response = places.get()
                    .uri(uri -> uri.path("/v1/places/{placeId}")
                            .queryParam("languageCode", "en").queryParam("regionCode", "gb")
                            .queryParam("sessionToken", token).build(placeId))
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
                    .retrieve()
                    .body(GoogleProviderDtos.PlaceDetailsProviderResponse.class);
            if (response == null || response.id() == null) {
                throw new IllegalStateException("Google returned an invalid place response");
            }
            record("place_details_essentials", "success", started, 1);
            return new GatewayContracts.PlaceDetails(
                    response.id(), null,
                    component(response.addressComponents(), "postal_code", false),
                    firstComponent(response.addressComponents(), List.of("postal_town", "locality"), false),
                    component(response.addressComponents(), "administrative_area_level_1", false),
                    component(response.addressComponents(), "country", true),
                    response.location() == null ? null : BigDecimal.valueOf(response.location().latitude()),
                    response.location() == null ? null : BigDecimal.valueOf(response.location().longitude()),
                    Instant.now());
        } catch (RuntimeException exception) {
            record("place_details_essentials", "failure", started, 0);
            throw exception;
        }
    }

    public GatewayContracts.CommuteMatrixResponse matrix(GatewayContracts.CommuteMatrixRequest request) {
        properties.validateForUse();
        List<GatewayContracts.CommuteEstimate> estimates = new ArrayList<>();
        for (GatewayContracts.TravelMode mode : request.modes()) {
            estimates.addAll(matrixForMode(request, mode));
        }
        return new GatewayContracts.CommuteMatrixResponse(List.copyOf(estimates));
    }

    private List<GatewayContracts.CommuteEstimate> matrixForMode(
            GatewayContracts.CommuteMatrixRequest request, GatewayContracts.TravelMode mode) {
        long started = System.nanoTime();
        GoogleProviderDtos.MatrixProviderRequest providerRequest = new GoogleProviderDtos.MatrixProviderRequest(
                List.of(new GoogleProviderDtos.RouteMatrixOrigin(waypoint(request.origin()))),
                request.destinations().stream()
                        .map(value -> new GoogleProviderDtos.RouteMatrixDestination(waypoint(value))).toList(),
                mode.name(), mode == GatewayContracts.TravelMode.DRIVE ? "TRAFFIC_AWARE" : null,
                request.departureTime().toString());
        try {
            List<GoogleProviderDtos.RouteMatrixElement> elements = routes.post()
                    .uri("/distanceMatrix/v2:computeRouteMatrix")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", MATRIX_FIELD_MASK)
                    .body(providerRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            List<GoogleProviderDtos.RouteMatrixElement> safe = elements == null ? List.of() : elements;
            List<GatewayContracts.CommuteEstimate> mapped = safe.stream()
                    .map(element -> estimate(request, mode, element))
                    .toList();
            record(mode == GatewayContracts.TravelMode.DRIVE ? "route_matrix_pro" : "route_matrix_essentials",
                    "success", started, request.destinations().size());
            return mapped;
        } catch (RuntimeException exception) {
            record(mode == GatewayContracts.TravelMode.DRIVE ? "route_matrix_pro" : "route_matrix_essentials",
                    "failure", started, request.destinations().size());
            throw exception;
        }
    }

    private GatewayContracts.CommuteEstimate estimate(
            GatewayContracts.CommuteMatrixRequest request,
            GatewayContracts.TravelMode mode,
            GoogleProviderDtos.RouteMatrixElement element) {
        int index = element.destinationIndex() == null ? -1 : element.destinationIndex();
        if (index < 0 || index >= request.destinations().size() || !successful(element)) {
            String reference = index >= 0 && index < request.destinations().size()
                    ? request.destinations().get(index).referenceId() : "unknown";
            return new GatewayContracts.CommuteEstimate(
                    reference, mode, GatewayContracts.EstimateStatus.UNAVAILABLE,
                    null, null, request.departureTime(), "NO_ROUTE", "GOOGLE_MAPS");
        }
        GatewayContracts.RoutePoint destination = request.destinations().get(index);
        GatewayContracts.EstimateStatus status = approximate(request.origin(), destination)
                ? GatewayContracts.EstimateStatus.APPROXIMATE : GatewayContracts.EstimateStatus.ESTIMATED;
        return new GatewayContracts.CommuteEstimate(
                destination.referenceId(), mode, status, durationMinutes(element.duration()),
                miles(element.distanceMeters()), request.departureTime(), null, "GOOGLE_MAPS");
    }

    private boolean successful(GoogleProviderDtos.RouteMatrixElement element) {
        if (element.condition() != null && !"ROUTE_EXISTS".equals(element.condition())) {
            return false;
        }
        Object code = element.status() == null ? null : element.status().get("code");
        return code == null || "0".equals(String.valueOf(code));
    }

    private boolean approximate(GatewayContracts.RoutePoint origin, GatewayContracts.RoutePoint destination) {
        return origin.precision() != GatewayContracts.Precision.EXACT_ADDRESS
                || destination.precision() != GatewayContracts.Precision.EXACT_ADDRESS;
    }

    private GoogleProviderDtos.Waypoint waypoint(GatewayContracts.RoutePoint point) {
        return new GoogleProviderDtos.Waypoint(new GoogleProviderDtos.Location(
                new GoogleProviderDtos.LatLng(point.latitude().doubleValue(), point.longitude().doubleValue())));
    }

    private Integer durationMinutes(String duration) {
        if (duration == null || !duration.endsWith("s")) {
            return null;
        }
        BigDecimal seconds = new BigDecimal(duration.substring(0, duration.length() - 1));
        return seconds.divide(new BigDecimal("60"), 0, RoundingMode.CEILING).intValueExact();
    }

    private BigDecimal miles(Integer meters) {
        return meters == null ? null : BigDecimal.valueOf(meters).divide(METERS_PER_MILE, 1, RoundingMode.HALF_UP);
    }

    private GatewayContracts.Suggestion suggestion(GoogleProviderDtos.PlacePrediction prediction) {
        String id = prediction.placeId();
        if (id == null && prediction.place() != null && prediction.place().startsWith("places/")) {
            id = prediction.place().substring("places/".length());
        }
        String primary = prediction.structuredFormat() != null && prediction.structuredFormat().mainText() != null
                ? prediction.structuredFormat().mainText().text()
                : prediction.text() == null ? null : prediction.text().text();
        String secondary = prediction.structuredFormat() == null || prediction.structuredFormat().secondaryText() == null
                ? null : prediction.structuredFormat().secondaryText().text();
        if (id == null || primary == null || primary.isBlank()) {
            return null;
        }
        return new GatewayContracts.Suggestion(id, primary, secondary, precision(prediction.types()));
    }

    private GatewayContracts.Precision precision(List<String> types) {
        if (types == null) return GatewayContracts.Precision.NONE;
        if (types.contains("postal_code")) return GatewayContracts.Precision.POSTCODE_CENTROID;
        if (types.contains("locality") || types.contains("postal_town") || types.contains("administrative_area_level_2")) {
            return GatewayContracts.Precision.LOCALITY_CENTROID;
        }
        return GatewayContracts.Precision.NONE;
    }

    private String firstComponent(List<GoogleProviderDtos.AddressComponent> values, List<String> types, boolean shortText) {
        for (String type : types) {
            String value = component(values, type, shortText);
            if (value != null) return value;
        }
        return null;
    }

    private String component(List<GoogleProviderDtos.AddressComponent> values, String type, boolean shortText) {
        if (values == null) return null;
        return values.stream().filter(value -> value.types() != null && value.types().contains(type))
                .map(value -> shortText ? value.shortText() : value.longText())
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    private void record(String operation, String outcome, long started, int billableEvents) {
        meters.timer("google.maps.provider.requests", "operation", operation, "outcome", outcome)
                .record(Duration.ofNanos(System.nanoTime() - started));
        if (billableEvents > 0) {
            meters.counter("google.maps.billable.events", "sku", operation).increment(billableEvents);
        }
    }
}
