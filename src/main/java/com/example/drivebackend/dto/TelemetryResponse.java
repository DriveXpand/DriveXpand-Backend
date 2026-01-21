package com.example.drivebackend.dto;

import java.util.Map;
import java.util.UUID;

public record TelemetryResponse(
        UUID id,
        String deviceId,
        Long startTime,
        Long endTime,
        Map<String, Object> aggregatedData,
        Map<String, Object> metrics
) {
}

