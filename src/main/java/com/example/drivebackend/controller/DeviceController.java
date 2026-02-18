package com.example.drivebackend.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.drivebackend.dto.DeviceNoteRequest;
import com.example.drivebackend.dto.DeviceNoteResponse;
import com.example.drivebackend.entities.DeviceEntity;
import com.example.drivebackend.repository.DeviceRepository;
import com.example.drivebackend.services.DeviceNoteService;
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

    private static final List<String> SUPPORTED_IMAGE_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp"
    );

    private final DeviceRepository deviceRepository;
    private final TelemetryService telemetryService;
    private final DeviceNoteService deviceNoteService;

    @Operation(summary = "Vehicle statistics", description = "Get aggregated vehicle statistics (distance, speed, drive time)")
    @ApiResponse(responseCode = "200", description = "Vehicle statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getVehicleStats(
        @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId,
        @Parameter(description = "Start time") @RequestParam(value = "since", required = false) Instant since,
        @Parameter(description = "End time") @RequestParam(value = "end", required = false) Instant end,
        @Parameter(description = "Min seconds between trips") @RequestParam(value = "timeBetweenTripsInSeconds", defaultValue = "1800") int timeBetweenTripsInSeconds
    ) {
        Map<UUID, List<TelemetryResponse>> trips = telemetryService.fetchTelemetryGroupedByTrip(deviceId, since, end, timeBetweenTripsInSeconds);

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

    @Operation(summary = "Get device note", description = "Fetch the repair note for a device")
    @ApiResponse(responseCode = "200", description = "Device note")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @GetMapping("/{deviceId}/note")
    public ResponseEntity<DeviceNoteResponse> getDeviceNote(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId) {
        return deviceNoteService.getNote(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update device note", description = "Update repair note details for a device")
    @ApiResponse(responseCode = "200", description = "Device note updated")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PatchMapping("/{deviceId}/note")
    public ResponseEntity<DeviceNoteResponse> updateDeviceNote(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Repair note fields") @RequestBody DeviceNoteRequest request) {
        return deviceNoteService.updateNote(deviceId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Upload device note photo", description = "Upload or replace the repair note photo (supports JPEG, PNG, GIF, WebP)")
    @ApiResponse(responseCode = "204", description = "Photo uploaded")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @ApiResponse(responseCode = "415", description = "Unsupported image format")
    @PatchMapping(path = "/{deviceId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadDeviceNotePhoto(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @Parameter(description = "Photo file (JPEG, PNG, GIF, WebP)", required = true) @RequestPart("file") MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            String contentType = file.getContentType();
            Optional<DeviceNoteService.DeviceNotePhoto> result = deviceNoteService.updateNotePhoto(deviceId, content, contentType);
            if (result.isPresent()) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Download device note photo", description = "Download the repair note photo")
    @ApiResponse(responseCode = "200", description = "Photo file")
    @ApiResponse(responseCode = "404", description = "Device or photo not found")
    @GetMapping("/{deviceId}/photo")
    public ResponseEntity<byte[]> downloadDeviceNotePhoto(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId) {
        Optional<DeviceNoteService.DeviceNotePhoto> photoOpt = deviceNoteService.getNotePhoto(deviceId);
        if (photoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DeviceNoteService.DeviceNotePhoto photo = photoOpt.get();
        String contentType = photo.contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(photo.content());
    }
}
