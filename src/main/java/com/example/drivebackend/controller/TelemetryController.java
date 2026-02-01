package com.example.drivebackend.controller;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

    @GetMapping("/trips-per-weekday")
    public ResponseEntity<Map<DayOfWeek, Integer>> getTripsPerWeekday(
        @RequestParam("deviceId") String deviceId,
        @RequestParam("since") Instant since,
        @RequestParam("end") Instant end,
        @RequestParam(value = "timeBetweenDrivesInSeconds", defaultValue = "600") int timeBetweenDrivesInSeconds
        ) {
    Map<Instant, ?> trips = telemetryService.fetchTelemetryGroupedByDrive(deviceId, since, end, timeBetweenDrivesInSeconds);

    Map<DayOfWeek, Integer> result = new EnumMap<>(DayOfWeek.class);
    for (Instant tripStart : trips.keySet()) {
        DayOfWeek day = tripStart.atZone(ZoneOffset.UTC).getDayOfWeek();
        result.put(day, result.getOrDefault(day, 0) + 1);
    }
    return ResponseEntity.ok(result);
    }
}

