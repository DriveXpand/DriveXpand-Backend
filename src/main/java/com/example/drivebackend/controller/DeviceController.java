package com.example.drivebackend.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.drivebackend.entities.DeviceEntity;
import com.example.drivebackend.repository.DeviceRepository;
import com.example.drivebackend.services.TelemetryService;
import com.example.drivebackend.dto.TelemetryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final TelemetryService telemetryService;

    @Operation(summary = "Vehicle statistics", description = "Get aggregated vehicle statistics (distance, speed, drive time)")
    @ApiResponse(responseCode = "200", description = "Vehicle statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getVehicleStats(
        @Parameter(description = "Device ID") @RequestParam(value="deviceId", required = true) String deviceId,
        @Parameter(description = "Start time") @RequestParam(value="since", required = false) Instant since,
        @Parameter(description = "End time") @RequestParam(value="end", required = false) Instant end
    ) {
        Map<UUID, List<TelemetryResponse>> trips = telemetryService.fetchTelemetryGroupedByTrip(deviceId, since, end);

        double totalMeter = 0.0;
        double totalSpeed = 0.0;
        int speedCount = 0;
        long totalDriveTimeSeconds = 0;
        int tripCount = trips.size();

        for (List<TelemetryResponse> trip : trips.values()) {
            if (trip.isEmpty()) {
                continue;
            }

            for (TelemetryResponse resp : trip) {
                if (resp.aggregated_data() != null && resp.aggregated_data().get("distance") instanceof Number dist) {
                    totalMeter += dist.doubleValue();
                }
            }

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

             Instant first = trip.getFirst().start_time();
             Instant last = trip.getLast().end_time();
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

    @Operation(summary = "Get all devices", description = "Retrieve a list of all registered devices")
    @ApiResponse(responseCode = "200", description = "List of devices")
    @GetMapping
    public List<DeviceEntity> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Operation(summary = "Update device name", description = "Update the name of a specific device")
    @ApiResponse(responseCode = "200", description = "Device updated successfully")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PutMapping("/{deviceId}/name")
    public ResponseEntity<DeviceEntity> updateDeviceName(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New device name", required = true) @RequestBody String name) {
        return deviceRepository.findById(deviceId)
                .map(device -> {
                    device.setName(name);
                    deviceRepository.save(device);
                    return ResponseEntity.ok(device);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}