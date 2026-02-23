package com.example.drivebackend.controller;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.dto.TripDetailsResponse;
import com.example.drivebackend.dto.TripResponse;
import com.example.drivebackend.dto.TripUpdateRequest;
import com.example.drivebackend.entities.TelemetryEntity;
import com.example.drivebackend.entities.TripEntity;
import com.example.drivebackend.repository.TelemetrySampleRepository;
import com.example.drivebackend.repository.TripRepository;
import com.example.drivebackend.services.TelemetryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripRepository tripRepository;
    private final TelemetrySampleRepository telemetrySampleRepository;
    private final TelemetryService telemetryService;

    @Operation(summary = "Trips per weekday", description = "Count trips grouped by day of week")
    @ApiResponse(responseCode = "200", description = "Trip counts by weekday")
    @GetMapping("/weekday")
    public ResponseEntity<Map<DayOfWeek, Integer>> getTripsPerWeekday(
            @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId,
            @Parameter(description = "Start time (optional)") @RequestParam(value = "since", required = false) Instant since,
            @Parameter(description = "End time (optional)") @RequestParam(value = "end", required = false) Instant end
    ) {
        Map<UUID, List<TelemetryResponse>> trips = telemetryService.fetchTelemetryGroupedByTrip(deviceId, since, end);

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

    @Operation(
        summary = "Get trips",
        description = "Show all existing trips, without telemetry data"
    )
    @ApiResponse(responseCode = "200", description = "Paged list of trips")
    @GetMapping("/list")
    public ResponseEntity<List<TripEntity>> getTrips(
            @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId,
            @Parameter(description = "Start time (optional)") @RequestParam(value = "since", required = false) Instant since,
            @Parameter(description = "End time (optional)") @RequestParam(value = "end", required = false) Instant end,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(value = "pageSize", required = false) Integer pageSize
    ) {
        // Paging is optional
        Pageable pageable;
        if (page == null || pageSize == null) {
            pageable = Pageable.unpaged(Sort.by("startTime").descending());
        } else {
            pageable = PageRequest.of(
                    page,
                    pageSize,
                    Sort.by("startTime").descending()
            );
        }

        Page<TripEntity> trips;

        if (since != null && end != null) {
            trips = tripRepository
                    .findByDevice_DeviceIdAndStartTimeBetween(deviceId, since, end, pageable);
        } else if (since != null) {
            trips = tripRepository
                    .findByDevice_DeviceIdAndStartTimeAfter(deviceId, since, pageable);
        } else if (end != null) {
            trips = tripRepository
                    .findByDevice_DeviceIdAndStartTimeBefore(deviceId, end, pageable);
        } else {
            trips = tripRepository
                    .findByDevice_DeviceId(deviceId, pageable);
        }

        return ResponseEntity.ok(trips.getContent());
    }

    @Operation(summary = "Get telemetry for a trip", description = "Fetch all telemetry samples for a given trip and device, use trips/list to gather the id")
    @ApiResponse(responseCode = "200", description = "List of telemetry samples")
    @GetMapping("/{tripId}")
    public ResponseEntity<TripDetailsResponse> getTelemetryByTrip(
            @Parameter(description = "Trip ID", required = true) @PathVariable("tripId") UUID tripId,
            @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId
    ) {
        List<TelemetryEntity> telemetry = telemetrySampleRepository.findByTrip_IdAndDevice_DeviceId(tripId, deviceId);
        if (telemetry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TripEntity trip = telemetry.getFirst()
                .getTrip();
        List<Map<String, Object>> aggregated_data = new ArrayList<>();
        List<Map<String, Object>> timed_data = new ArrayList<>();
        for (TelemetryEntity entity : telemetry){ 
            aggregated_data.add(entity.getAggregated_data());
            timed_data.add(entity.getTimed_data());
        }
        TripDetailsResponse tripDetails = new TripDetailsResponse(
            tripId,
            deviceId,
            trip.getStartTime(),
            trip.getEndTime(),
            trip.getStartLocation(),
            trip.getEndLocation(),
            trip.getNote(),
            timed_data,
            aggregated_data,
            trip.getTrip_distance_km()
        );

        return ResponseEntity.ok(tripDetails);
    }

    @Operation(summary = "Get trips with details", description = "Fetch telemetry grouped by trip with detailed information")
    @ApiResponse(responseCode = "200", description = "Trips with details")
    @GetMapping
    public ResponseEntity<Map<UUID, TripDetailsResponse>> fetchTelemetryGroupedByTrip(
            @Parameter(description = "Device ID", required = true) @RequestParam("deviceId") String deviceId,
            @Parameter(description = "Start time (optional)") @RequestParam(value = "since", required = false) Instant since,
            @Parameter(description = "End time (optional)") @RequestParam(value = "end", required = false) Instant end,
            @Parameter(description = "Page 0 based (0 is first page)") @RequestParam(value = "page", required = false) Integer page,
            @Parameter() @RequestParam(value = "page", required = false) Integer pageSize
    ) {
        Map<UUID, TripDetailsResponse> tripMap = telemetryService.fetchTripDetails(deviceId, since, end);
        return ResponseEntity.ok(tripMap);
    }

    @Operation(summary = "Update trip", description = "Update start/end location of a trip")
    @ApiResponse(responseCode = "200", description = "Trip updated successfully")
    @ApiResponse(responseCode = "404", description = "Trip not found")
    @PatchMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @Parameter(description = "Trip ID", required = true) @PathVariable UUID tripId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Trip update data", required = true) @RequestBody TripUpdateRequest request) {
        return tripRepository.findById(tripId)
                .map(trip -> {
                    if (request.startLocation() != null) {
                        trip.setStartLocation(request.startLocation());
                    }
                    if (request.endLocation() != null) {
                        trip.setEndLocation(request.endLocation());
                    }
                    if (request.note() != null) {
                        trip.setNote(request.note());
                    }
                    TripEntity saved = tripRepository.save(trip);
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private TripResponse toResponse(TripEntity trip) {
        return new TripResponse(
                trip.getId(),
                trip.getDevice().getDeviceId(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getStartLocation(),
                trip.getEndLocation(),
                trip.getNote(),
                trip.getTrip_distance_km()
        );
    }
}
