package com.pulse.pass.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.dto.request.CreateVenueRequest;
import jakarta.validation.Valid;
import com.pulse.pass.service.VenueService;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody CreateVenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.create(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<VenueResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(venueService.findByCode(code));
    }

    @GetMapping("/active")
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {
        return ResponseEntity.ok(venueService.findActiveVenues());
    }
}
