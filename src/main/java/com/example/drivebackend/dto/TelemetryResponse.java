package com.example.drivebackend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TelemetryResponse(
        UUID id,
        String deviceId,
        UUID tripId,
        Instant start_time,
        Instant end_time,
        Map<String, Object> timed_data,
        Map<String, Object> aggregated_data
) {
}
