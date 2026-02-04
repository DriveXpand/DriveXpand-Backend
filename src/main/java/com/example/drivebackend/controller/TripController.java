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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripRepository tripRepository;

    @PatchMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable UUID tripId,
            @RequestBody TripUpdateRequest request
    ) {
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
