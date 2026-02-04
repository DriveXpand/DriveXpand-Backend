package com.example.drivebackend.services.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.drivebackend.dto.TelemetryIngestRequest;
import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.entities.DeviceEntity;
import com.example.drivebackend.entities.TelemetryEntity;
import com.example.drivebackend.entities.TripEntity;
import com.example.drivebackend.mapper.TelemetryMapper;
import com.example.drivebackend.repository.DeviceRepository;
import com.example.drivebackend.repository.TelemetrySampleRepository;
import com.example.drivebackend.repository.TripRepository;
import com.example.drivebackend.services.TelemetryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelemetryServiceImpl implements TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryServiceImpl.class);
    private static final Duration TRIP_GAP = Duration.ofMinutes(30);

    private final TelemetrySampleRepository telemetrySampleRepository;
    private final DeviceRepository deviceRepository;
    private final TripRepository tripRepository;
    private final TelemetryMapper telemetryMapper;

    @Override
    @Transactional
    public TelemetryResponse ingestTelemetry(TelemetryIngestRequest request) {
        DeviceEntity device = deviceRepository.findById(request.deviceId())
            .orElseGet(() -> {
                DeviceEntity newDevice = new DeviceEntity();
                newDevice.setDeviceId(request.deviceId());
                return deviceRepository.save(newDevice);
            });

        TripEntity trip = resolveTrip(device, request.start_time());

        TelemetryEntity sample = telemetryMapper.toEntity(request);
        sample.setDevice(device);
        sample.setTrip(trip);
        telemetrySampleRepository.save(sample);

        updateTripEndTime(trip, request.start_time());

        log.debug("Stored telemetry sample for device {}", request.deviceId());
        return telemetryMapper.toDto(sample);
    }

    @Override
    public Optional<TelemetryResponse> fetchLatestTelemetry(String deviceId) {
        return telemetrySampleRepository.findTopByDevice_DeviceIdOrderByStartTimeDesc(deviceId)
                .map(telemetryMapper::toDto);
    }

    @Override
    public List<TelemetryResponse> fetchTelemetryInRange(String deviceId, Instant since, Instant end) {
        List<TelemetryEntity> response;
        if (since != null && end != null) {
            response = telemetrySampleRepository.findAllByDeviceIdInRange(deviceId, since, end);
        } else if (since != null) {
            response = telemetrySampleRepository.findAllByDevice_DeviceIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(deviceId, since);
        } else if (end != null) {
            response = telemetrySampleRepository.findAllByDevice_DeviceIdAndStartTimeLessThanEqualOrderByStartTimeAsc(deviceId, end);
        } else {
            response = telemetrySampleRepository.findAllByDevice_DeviceIdOrderByStartTimeAsc(deviceId);
        }
        return response.stream().map(telemetryMapper::toDto).toList();
    }

    @Override
    public List<TelemetryResponse> fetchTelemetryInRangeByTrip(String deviceId, UUID tripId, Instant since, Instant end) {
        List<TelemetryEntity> response;
        if (since != null && end != null) {
            response = telemetrySampleRepository.findAllByDeviceIdAndTripIdInRange(deviceId, tripId, since, end);
        } else if (since != null) {
            response = telemetrySampleRepository.findAllByDevice_DeviceIdAndTrip_IdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(deviceId, tripId, since);
        } else if (end != null) {
            response = telemetrySampleRepository.findAllByDevice_DeviceIdAndTrip_IdAndStartTimeLessThanEqualOrderByStartTimeAsc(deviceId, tripId, end);
        } else {
            response = telemetrySampleRepository.findAllByDevice_DeviceIdAndTrip_IdOrderByStartTimeAsc(deviceId, tripId);
        }
        return response.stream().map(telemetryMapper::toDto).toList();
    }

    @Override
    public Map<UUID, List<TelemetryResponse>> fetchTelemetryGroupedByTrip(String deviceId, Instant since, Instant end, int timeBetweenTripsInSeconds) {
        List<TelemetryEntity> response = telemetrySampleRepository.findAllByDeviceIdInRange(deviceId, since, end);
        List<TelemetryResponse> dtoResponses = response.stream().map(telemetryMapper::toDto).toList();
        Map<UUID, List<TelemetryResponse>> aggregatedDrivesMap = new LinkedHashMap<>();

        for (TelemetryResponse entity : dtoResponses) {
            UUID tripId = entity.tripId();
            aggregatedDrivesMap.computeIfAbsent(tripId, ignored -> new ArrayList<>()).add(entity);
        }

        return aggregatedDrivesMap;
    }

    private TripEntity resolveTrip(DeviceEntity device, Instant currentStartTime) {
        return telemetrySampleRepository.findTopByDevice_DeviceIdOrderByStartTimeDesc(device.getDeviceId())
            .map(TelemetryEntity::getTrip)
            .filter(existingTrip -> !isNewTrip(existingTrip, currentStartTime))
            .orElseGet(() -> createTrip(device, currentStartTime));
    }

    private boolean isNewTrip(TripEntity existingTrip, Instant currentStartTime) {
        Instant lastEnd = existingTrip.getEndTime();
        return lastEnd == null || Duration.between(lastEnd, currentStartTime).compareTo(TRIP_GAP) > 0;
    }

    private TripEntity createTrip(DeviceEntity device, Instant startTime) {
        TripEntity trip = new TripEntity();
        trip.setDevice(device);
        trip.setStartTime(startTime);
        trip.setEndTime(startTime);
        return tripRepository.save(trip);
    }

    private void updateTripEndTime(TripEntity trip, Instant currentStartTime) {
        Instant endTime = trip.getEndTime();
        if (endTime == null || currentStartTime.isAfter(endTime)) {
            trip.setEndTime(currentStartTime);
            tripRepository.save(trip);
        }
    }
}
