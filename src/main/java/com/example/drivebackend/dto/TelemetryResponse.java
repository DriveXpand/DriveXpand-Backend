package com.example.drivebackend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TelemetryResponse(
        UUID id,
        String deviceId,
        Instant recordedAt,
        Long startTime,
        Long endTime,
        Map<String, Object> aggregatedData,
        Map<String, Object> metrics
) {
}

