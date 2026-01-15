package com.example.drivebackend.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.drivebackend.dto.TelemetryIngestRequest;
import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.services.TelemetryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@Validated
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<TelemetryResponse> ingestTelemetry(@Valid @RequestBody TelemetryIngestRequest request) {
        TelemetryResponse response = telemetryService.ingestTelemetry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<TelemetryResponse> fetchLatestTelemetry(@RequestParam("deviceId") String deviceId) {
        Optional<TelemetryResponse> latest = telemetryService.fetchLatestTelemetry(deviceId);
        return latest.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/range")
    public ResponseEntity<Map<Instant, List<TelemetryResponse>>> fetchTelemetryGroupedByDrive(
            @RequestParam("deviceId") String deviceId,
            @RequestParam("since") Instant since,
            @RequestParam("end") Instant end,
            @RequestParam(value = "timeBetweenDrivesInSeconds", defaultValue = "600") int timeBetweenDrivesInSeconds
    ) {
        Map<Instant, List<TelemetryResponse>> telemetryMap = telemetryService.fetchTelemetryGroupedByDrive(deviceId, since, end, timeBetweenDrivesInSeconds);
        return ResponseEntity.ok(telemetryMap);
    }
}

