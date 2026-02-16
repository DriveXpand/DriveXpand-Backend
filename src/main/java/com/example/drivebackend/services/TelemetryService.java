package com.example.drivebackend.services;

import com.example.drivebackend.dto.TelemetryIngestRequest;
import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.dto.TripDetailsResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TelemetryService {

    TelemetryResponse ingestTelemetry(TelemetryIngestRequest request);

    Optional<TelemetryResponse> fetchLatestTelemetry(String deviceId);

    List<TelemetryResponse> fetchTelemetryInRange(String deviceId, Instant since, Instant end);

    List<TelemetryResponse> fetchTelemetryInRangeByTrip(String deviceId, UUID tripId, Instant since, Instant end);

    // Map für alle Fahrten in gegebenen Zeitraum. Der key ist die Fahrt-ID
    Map<UUID, List<TelemetryResponse>> fetchTelemetryGroupedByTrip(String deviceId, Instant since, Instant end);

    Map<UUID, TripDetailsResponse> fetchTripDetails(String deviceId, Instant since, Instant end);
}
