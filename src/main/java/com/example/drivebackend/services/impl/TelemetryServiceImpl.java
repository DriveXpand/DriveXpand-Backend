package com.example.drivebackend.services.impl;

import com.example.drivebackend.dto.TelemetryIngestRequest;
import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.entities.TelemetryEntity;
import com.example.drivebackend.mapper.TelemetryMapper;
import com.example.drivebackend.repository.TelemetrySampleRepository;
import com.example.drivebackend.services.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelemetryServiceImpl implements TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryServiceImpl.class);

    private final TelemetrySampleRepository telemetrySampleRepository;
    private final TelemetryMapper telemetryMapper;

    @Override
    @Transactional
    public TelemetryResponse ingestTelemetry(TelemetryIngestRequest request) {
        TelemetryEntity sample = telemetryMapper.toEntity(request);
        telemetrySampleRepository.save(sample);
        log.debug("Stored telemetry sample for device {}", request.deviceId());
        return telemetryMapper.toDto(sample);
    }

    @Override
    public Optional<TelemetryResponse> fetchLatestTelemetry(String deviceId) {
        return telemetrySampleRepository.findTopByDeviceIdOrderByStartTimeDesc(deviceId)
                .map(telemetryMapper::toDto);
    }
}

