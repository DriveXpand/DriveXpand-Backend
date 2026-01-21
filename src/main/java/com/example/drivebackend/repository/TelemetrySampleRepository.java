package com.example.drivebackend.repository;

import com.example.drivebackend.entities.TelemetryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TelemetrySampleRepository extends JpaRepository<TelemetryEntity, UUID> {

    Optional<TelemetryEntity> findTopByDeviceIdOrderByStartTimeDesc(String deviceId);
}

