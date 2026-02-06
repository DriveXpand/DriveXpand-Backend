package com.example.drivebackend.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.drivebackend.dto.TelemetryIngestRequest;
import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.services.TelemetryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@Validated
public class TelemetryController {

    private final TelemetryService telemetryService;

    @Operation(summary = "Ingest telemetry", description = "Submit telemetry data for a device")
    @ApiResponse(responseCode = "201", description = "Telemetry ingested")
    @PostMapping
    public ResponseEntity<TelemetryResponse> ingestTelemetry(@Valid @RequestBody TelemetryIngestRequest request) {
        TelemetryResponse response = telemetryService.ingestTelemetry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get latest telemetry", description = "Fetch the latest telemetry record for a device")
    @ApiResponse(responseCode = "200", description = "Latest telemetry data")
    @ApiResponse(responseCode = "404", description = "No telemetry found")
    @GetMapping("/latest")
    public ResponseEntity<TelemetryResponse> fetchLatestTelemetry(
            @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId) {
        Optional<TelemetryResponse> latest = telemetryService.fetchLatestTelemetry(deviceId);
        return latest.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get telemetry data", description = "Fetch telemetry records with optional time range and trip filtering")
    @ApiResponse(responseCode = "200", description = "Telemetry records")
    @GetMapping
    public ResponseEntity<List<TelemetryResponse>> fetchTelemetry(
            @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId,
            @Parameter(description = "Start time (optional)") @RequestParam(value = "since", required = false) Instant since,
            @Parameter(description = "End time (optional)") @RequestParam(value = "end", required = false) Instant end,
            @Parameter(description = "Trip ID (optional)") @RequestParam(value = "tripId", required = false) UUID tripId
    ) {
        List<TelemetryResponse> result = tripId == null
                ? telemetryService.fetchTelemetryInRange(deviceId, since, end)
                : telemetryService.fetchTelemetryInRangeByTrip(deviceId, tripId, since, end);
        return ResponseEntity.ok(result);
    }
}
