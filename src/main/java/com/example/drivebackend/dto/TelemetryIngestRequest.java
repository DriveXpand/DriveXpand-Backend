package com.example.drivebackend.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TelemetryIngestRequest(
        @NotBlank String deviceId,
        @NotNull Long start_time,
        Long end_time,
        Map<String, Object> aggregated_data,
        Map<String, Object> timed_data,
        Map<String, Object> errors
        ) {
}
