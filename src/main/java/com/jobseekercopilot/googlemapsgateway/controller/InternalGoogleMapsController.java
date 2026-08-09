package com.jobseekercopilot.googlemapsgateway.controller;

import com.jobseekercopilot.googlemapsgateway.model.GatewayContracts;
import com.jobseekercopilot.googlemapsgateway.service.GoogleMapsGatewayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalGoogleMapsController {
    private final GoogleMapsGatewayService service;

    public InternalGoogleMapsController(GoogleMapsGatewayService service) {
        this.service = service;
    }

    @PostMapping("/places/autocomplete")
    public GatewayContracts.AutocompleteResponse autocomplete(
            @Valid @RequestBody GatewayContracts.AutocompleteRequest request) {
        return service.autocomplete(request);
    }

    @PostMapping("/places/resolve")
    public GatewayContracts.PlaceDetails resolve(@Valid @RequestBody GatewayContracts.ResolveRequest request) {
        return service.resolve(request);
    }

    @PostMapping("/routes/matrix")
    public GatewayContracts.CommuteMatrixResponse matrix(
            @Valid @RequestBody GatewayContracts.CommuteMatrixRequest request) {
        return service.matrix(request);
    }
}
