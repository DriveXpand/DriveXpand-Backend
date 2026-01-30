package com.example.drivebackend.dto;

import java.util.Map;
import java.util.UUID;

public record TelemetryResponse(
        UUID id,
        String deviceId,
        Instant start_time,
        Map<String, Object> timed_data
) {
}

