package com.jobseekercopilot.googlemapsgateway.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

final class GoogleProviderDtos {
    private GoogleProviderDtos() { }

    record AutocompleteProviderRequest(
            String input,
            String sessionToken,
            List<String> includedRegionCodes,
            List<String> includedPrimaryTypes,
            String languageCode,
            String regionCode) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AutocompleteProviderResponse(List<ProviderSuggestion> suggestions) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProviderSuggestion(PlacePrediction placePrediction) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlacePrediction(
            String place,
            String placeId,
            TextValue text,
            StructuredFormat structuredFormat,
            List<String> types) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StructuredFormat(TextValue mainText, TextValue secondaryText) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TextValue(String text) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlaceDetailsProviderResponse(
            String id,
            List<AddressComponent> addressComponents,
            LatLng location,
            List<String> types) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AddressComponent(String longText, String shortText, List<String> types) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record MatrixProviderRequest(
            List<RouteMatrixOrigin> origins,
            List<RouteMatrixDestination> destinations,
            String travelMode,
            String routingPreference,
            String departureTime) { }

    record RouteMatrixOrigin(Waypoint waypoint) { }
    record RouteMatrixDestination(Waypoint waypoint) { }
    record Waypoint(Location location) { }
    record Location(LatLng latLng) { }
    record LatLng(double latitude, double longitude) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RouteMatrixElement(
            Integer originIndex,
            Integer destinationIndex,
            Map<String, Object> status,
            String condition,
            Integer distanceMeters,
            String duration) { }
}
