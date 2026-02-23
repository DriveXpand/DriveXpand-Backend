package com.example.drivebackend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TripDetailsResponse(
        UUID id,
        String deviceId,
        Instant startTime,
        Instant endTime,
        String startLocation,
        String endLocation,
        String note,
        List<Map<String, Object>> timed_data,
        List<Map<String, Object>> aggregated_data,
        Float trip_distance_km
) {
}
