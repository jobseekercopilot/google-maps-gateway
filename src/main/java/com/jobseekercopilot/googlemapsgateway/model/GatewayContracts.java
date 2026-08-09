package com.jobseekercopilot.googlemapsgateway.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class GatewayContracts {
    private GatewayContracts() { }

    public enum Precision { EXACT_ADDRESS, POSTCODE_CENTROID, LOCALITY_CENTROID, PROVIDER_COORDINATE, NONE }
    public enum TravelMode { DRIVE, TRANSIT }
    public enum EstimateStatus { ESTIMATED, APPROXIMATE, UNAVAILABLE }

    public record AutocompleteRequest(
            @NotBlank @Size(min = 3, max = 200) String input,
            @NotBlank String sessionId,
            @NotEmpty List<String> countryCodes) { }

    public record AutocompleteResponse(String sessionId, List<Suggestion> suggestions) { }

    public record Suggestion(
            String providerReference,
            String primaryText,
            String secondaryText,
            Precision precisionHint) { }

    public record ResolveRequest(@NotBlank String providerReference, @NotBlank String sessionId) { }

    public record PlaceDetails(
            String providerReference,
            String displayText,
            String postcode,
            String locality,
            String region,
            String countryCode,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant observedAt) { }

    public record CommuteMatrixRequest(
            @NotNull @Valid RoutePoint origin,
            @NotEmpty @Size(max = 5) List<@Valid RoutePoint> destinations,
            @NotEmpty List<TravelMode> modes,
            @NotNull Instant departureTime) { }

    public record RoutePoint(
            @NotNull String referenceId,
            @NotNull BigDecimal latitude,
            @NotNull BigDecimal longitude,
            @NotNull Precision precision) { }

    public record CommuteMatrixResponse(List<CommuteEstimate> estimates) { }

    public record CommuteEstimate(
            String destinationReferenceId,
            TravelMode mode,
            EstimateStatus status,
            Integer durationMinutes,
            BigDecimal distanceMiles,
            Instant calculatedFor,
            String reasonCode,
            String providerAttribution) { }
}
