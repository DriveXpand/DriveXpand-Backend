package com.example.drivebackend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.drivebackend.dto.TripResponse;
import com.example.drivebackend.dto.TripUpdateRequest;
import com.example.drivebackend.entities.TripEntity;
import com.example.drivebackend.repository.TripRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripRepository tripRepository;

    @Operation(summary = "Update trip", description = "Update start/end location of a trip")
    @ApiResponse(responseCode = "200", description = "Trip updated successfully")
    @ApiResponse(responseCode = "404", description = "Trip not found")
    @PatchMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @Parameter(description = "Trip ID", required = true) @PathVariable UUID tripId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Trip update data", required = true) @RequestBody TripUpdateRequest request) {
        return tripRepository.findById(tripId)
                .map(trip -> {
                    if (request.startLocation() != null) {
                        trip.setStartLocation(request.startLocation());
                    }
                    if (request.endLocation() != null) {
                        trip.setEndLocation(request.endLocation());
                    }
                    TripEntity saved = tripRepository.save(trip);
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private TripResponse toResponse(TripEntity trip) {
        return new TripResponse(
                trip.getId(),
                trip.getDevice().getDeviceId(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getStartLocation(),
                trip.getEndLocation()
        );
    }
}
