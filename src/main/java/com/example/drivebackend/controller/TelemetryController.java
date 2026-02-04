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

    @GetMapping
    public ResponseEntity<List<TelemetryResponse>> fetchTelemetry(
            @RequestParam("deviceId") String deviceId,
            @RequestParam(value = "since", required = false) Instant since,
            @RequestParam(value = "end", required = false) Instant end,
            @RequestParam(value = "tripId", required = false) UUID tripId
    ) {
        List<TelemetryResponse> result = tripId == null
                ? telemetryService.fetchTelemetryInRange(deviceId, since, end)
                : telemetryService.fetchTelemetryInRangeByTrip(deviceId, tripId, since, end);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trips-per-weekday")
    public ResponseEntity<Map<DayOfWeek, Integer>> getTripsPerWeekday(
        @RequestParam("deviceId") String deviceId,
        @RequestParam("since") Instant since,
        @RequestParam("end") Instant end,
        @RequestParam(value = "timeBetweenTripsInSeconds", defaultValue = "1800") int timeBetweenTripsInSeconds
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

    @GetMapping("/vehicle-stats")
    public ResponseEntity<Map<String, Object>> getVehicleStats(
        @RequestParam("deviceId") String deviceId,
        @RequestParam("since") Instant since,
        @RequestParam("end") Instant end,
        @RequestParam(value = "timeBetweenTripsInSeconds", defaultValue = "1800") int timeBetweenTripsInSeconds
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

    @GetMapping("/trips")
    public ResponseEntity<Map<UUID, List<TelemetryResponse>>> fetchTelemetryGroupedByTrip(
            @RequestParam("deviceId") String deviceId,
            @RequestParam(value = "since", required = false) Instant since,
            @RequestParam(value = "end", required = false) Instant end,
            @RequestParam(value = "timeBetweenTripsInSeconds", defaultValue = "1800") int timeBetweenTripsInSeconds
    ) {
        Map<UUID, List<TelemetryResponse>> tripMap = telemetryService.fetchTelemetryGroupedByTrip(deviceId, since, end, timeBetweenTripsInSeconds);
        return ResponseEntity.ok(tripMap);
    }
}
