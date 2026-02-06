package com.example.drivebackend.controller;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    @Operation(summary = "Trips per weekday", description = "Count trips grouped by day of week")
    @ApiResponse(responseCode = "200", description = "Trip counts by weekday")
    @GetMapping("/trips-per-weekday")
    public ResponseEntity<Map<DayOfWeek, Integer>> getTripsPerWeekday(
        @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId,
        @Parameter(description = "Start time (optional)") @RequestParam(value = "since", required = false) Instant since,
        @Parameter(description = "End time (optional)") @RequestParam(value = "end", required = false) Instant end,
        @Parameter(description = "Min seconds between trips") @RequestParam(value = "timeBetweenTripsInSeconds", defaultValue = "1800") int timeBetweenTripsInSeconds
        ) {
    Map<UUID, List<TelemetryResponse>> trips = telemetryService.fetchTelemetryGroupedByTrip(deviceId, since, end, timeBetweenTripsInSeconds);

    Map<DayOfWeek, Integer> result = new EnumMap<>(DayOfWeek.class);
    for (List<TelemetryResponse> trip : trips.values()) {
        if (trip.isEmpty()) {
            continue;
        }
        DayOfWeek day = trip.getFirst().start_time().atZone(ZoneOffset.UTC).getDayOfWeek();
        result.put(day, result.getOrDefault(day, 0) + 1);
    }
    return ResponseEntity.ok(result);
    }

    @Operation(summary = "Vehicle statistics", description = "Get aggregated vehicle statistics (distance, speed, drive time)")
    @ApiResponse(responseCode = "200", description = "Vehicle statistics")
    @GetMapping("/vehicle-stats")
    public ResponseEntity<Map<String, Object>> getVehicleStats(
        @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId,
        @Parameter(description = "Start time", required = true) @RequestParam("since") Instant since,
        @Parameter(description = "End time", required = true) @RequestParam("end") Instant end,
        @Parameter(description = "Min seconds between trips") @RequestParam(value = "timeBetweenTripsInSeconds", defaultValue = "1800") int timeBetweenTripsInSeconds
    ) {
    Map<UUID, List<TelemetryResponse>> trips = telemetryService.fetchTelemetryGroupedByTrip(deviceId, since, end, timeBetweenTripsInSeconds);

    double totalMeter = 0.0;
    double totalSpeed = 0.0;
    int speedCount = 0;
    long totalDriveTimeSeconds = 0;
    int tripCount = trips.size();

    for (List<TelemetryResponse> trip : trips.values()) {
        if (trip.isEmpty()) continue;

        // Aggregierte Distanz aufsummieren (falls vorhanden)
        for (TelemetryResponse resp : trip) {
            if (resp.aggregated_data() != null && resp.aggregated_data().get("distance") instanceof Number dist) {
                totalMeter += dist.doubleValue();
            }
        }

        // Durchschnittsgeschwindigkeit berechnen
        for (TelemetryResponse resp : trip) {
            if (resp.timed_data() != null) {
                for (Object timedEntry : resp.timed_data().values()) {
                    if (timedEntry instanceof Map<?, ?> map && map.get("speed") instanceof Number speed) {
                        totalSpeed += speed.doubleValue();
                        speedCount++;
                    }
                }
            }
        }

        // Fahrtdauer berechnen (Differenz zwischen erstem und letztem start_time)
        Instant first = trip.getFirst().start_time();
        Instant last = trip.getLast().start_time();
        totalDriveTimeSeconds += Math.abs(last.getEpochSecond() - first.getEpochSecond());
    }

    double avgSpeed = speedCount > 0 ? totalSpeed / speedCount : 0.0;
    int totalDriveTimeMinutes = (int) (totalDriveTimeSeconds / 60);

    Map<String, Object> result = Map.of(
            "total_km", totalMeter / 1000.0,
            "avg_speed", avgSpeed,
            "total_drive_time_minutes", totalDriveTimeMinutes,
            "trip_count", tripCount
    );

    return ResponseEntity.ok(result);
    }
}
