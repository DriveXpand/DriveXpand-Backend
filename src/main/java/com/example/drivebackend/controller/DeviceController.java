package com.example.drivebackend.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.drivebackend.dto.RepairNoteRequest;
import com.example.drivebackend.dto.RepairNoteResponse;
import com.example.drivebackend.entities.DeviceEntity;
import com.example.drivebackend.entities.RepairNoteEntity;
import com.example.drivebackend.mapper.RepairNoteMapper;
import com.example.drivebackend.repository.DeviceRepository;
import com.example.drivebackend.repository.RepairNoteRepository;
import com.example.drivebackend.services.TelemetryService;
import com.example.drivebackend.dto.TelemetryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
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
    private final RepairNoteRepository repairNoteRepository;
    private final RepairNoteMapper repairNoteMapper;
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

    @Operation(summary = "Get all repair notes", description = "Fetch all repair notes for a device with optional pagination")
    @ApiResponse(responseCode = "200", description = "List of repair notes")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @GetMapping("/{deviceId}/notes")
    public ResponseEntity<List<RepairNoteResponse>> getRepairNotes(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(value = "pageSize", required = false) Integer pageSize) {

        // Check if device exists
        if (!deviceRepository.existsById(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable;
        if (page == null || pageSize == null) {
            pageable = Pageable.unpaged(Sort.by("noteDate").descending());
        } else {
            pageable = PageRequest.of(page, pageSize, Sort.by("noteDate").descending());
        }

        Page<RepairNoteEntity> notes = repairNoteRepository.findByDeviceIdOrderByNoteDateDesc(deviceId, pageable);
        List<RepairNoteResponse> responses = notes.getContent().stream()
                .map(repairNoteMapper::toDto)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Create repair note", description = "Create a new repair note for a device")
    @ApiResponse(responseCode = "201", description = "Repair note created")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PostMapping("/{deviceId}/notes")
    public ResponseEntity<RepairNoteResponse> createRepairNote(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Repair note details") @Valid @RequestBody RepairNoteRequest request) {

        // Check if device exists
        if (!deviceRepository.existsById(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        RepairNoteEntity note = repairNoteMapper.toEntity(request, deviceId);
        RepairNoteEntity saved = repairNoteRepository.save(note);

        return ResponseEntity.status(HttpStatus.CREATED).body(repairNoteMapper.toDto(saved));
    }

    @Operation(summary = "Get repair note by ID", description = "Fetch a specific repair note")
    @ApiResponse(responseCode = "200", description = "Repair note details")
    @ApiResponse(responseCode = "404", description = "Note or device not found")
    @GetMapping("/{deviceId}/notes/{noteId}")
    public ResponseEntity<RepairNoteResponse> getRepairNoteById(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @Parameter(description = "Note ID", required = true) @PathVariable UUID noteId) {

        return repairNoteRepository.findByIdAndDeviceId(noteId, deviceId)
                .map(repairNoteMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update repair note", description = "Update an existing repair note")
    @ApiResponse(responseCode = "200", description = "Repair note updated")
    @ApiResponse(responseCode = "404", description = "Note or device not found")
    @PatchMapping("/{deviceId}/notes/{noteId}")
    public ResponseEntity<RepairNoteResponse> updateRepairNote(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @Parameter(description = "Note ID", required = true) @PathVariable UUID noteId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated repair note fields") @Valid @RequestBody RepairNoteRequest request) {

        Optional<RepairNoteEntity> noteOpt = repairNoteRepository.findByIdAndDeviceId(noteId, deviceId);
        if (noteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RepairNoteEntity note = noteOpt.get();
        repairNoteMapper.updateEntityFromRequest(request, note);

        RepairNoteEntity saved = repairNoteRepository.save(note);

        return ResponseEntity.ok(repairNoteMapper.toDto(saved));
    }

    @Operation(summary = "Delete repair note", description = "Delete a specific repair note")
    @ApiResponse(responseCode = "204", description = "Repair note deleted")
    @ApiResponse(responseCode = "404", description = "Note or device not found")
    @DeleteMapping("/{deviceId}/notes/{noteId}")
    public ResponseEntity<Void> deleteRepairNote(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @Parameter(description = "Note ID", required = true) @PathVariable UUID noteId) {

        Optional<RepairNoteEntity> noteOpt = repairNoteRepository.findByIdAndDeviceId(noteId, deviceId);
        if (noteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        repairNoteRepository.delete(noteOpt.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload device photo", description = "Upload or replace the device photo (supports JPEG, PNG, GIF, WebP)")
    @ApiResponse(responseCode = "204", description = "Photo uploaded")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @ApiResponse(responseCode = "415", description = "Unsupported image format")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @PatchMapping(path = "/{deviceId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadDevicePhoto(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @Parameter(description = "Photo file (JPEG, PNG, GIF, WebP)", required = true) @RequestPart("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            String contentType = file.getContentType();
            if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
            }

            byte[] content = file.getBytes();

            Optional<DeviceEntity> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DeviceEntity device = deviceOpt.get();
            device.setNotePhoto(content);
            device.setNotePhotoContentType(contentType);
            deviceRepository.save(device);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @Operation(summary = "Download device photo", description = "Download the device photo")
    @ApiResponse(responseCode = "200", description = "Photo file")
    @ApiResponse(responseCode = "404", description = "Device or photo not found")
    @GetMapping("/{deviceId}/photo")
    public ResponseEntity<byte[]> downloadDevicePhoto(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId) {
        Optional<DeviceEntity> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty() || deviceOpt.get().getNotePhoto() == null) {
            return ResponseEntity.notFound().build();
        }

        DeviceEntity device = deviceOpt.get();
        String contentType = device.getNotePhotoContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(device.getNotePhoto());
    }
}
