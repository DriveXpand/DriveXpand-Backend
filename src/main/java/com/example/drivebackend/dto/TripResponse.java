package com.example.drivebackend.dto;

import java.time.Instant;
import java.util.UUID;

public record TripResponse(
        UUID id,
        String deviceId,
        Instant startTime,
        Instant endTime,
        String startLocation,
        String endLocation
) {
}
