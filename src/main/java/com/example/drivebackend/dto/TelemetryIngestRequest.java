package com.example.drivebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record TelemetryIngestRequest(
        @NotBlank String deviceId,
        @NotNull Instant recordedAt,
        @JsonProperty("start_time") Long startTime,
        @JsonProperty("end_time") Long endTime,
        @JsonProperty("aggregated_data") Map<String, Object> aggregatedData,
        Map<String, Object> metrics
) {
}

