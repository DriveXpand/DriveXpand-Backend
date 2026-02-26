package com.example.drivebackend.repository;

import java.time.Instant;
import java.util.UUID;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.drivebackend.entities.TripEntity;

public interface TripRepository extends JpaRepository<TripEntity, UUID> {
Page<TripEntity> findByDevice_DeviceId(String deviceId, Pageable pageable);

Page<TripEntity> findByDevice_DeviceIdAndStartTimeBetween(
        String deviceId,
        Instant since,
        Instant end,
        Pageable pageable
);

Page<TripEntity> findByDevice_DeviceIdAndStartTimeAfter(
        String deviceId,
        Instant since,
        Pageable pageable
);

Page<TripEntity> findByDevice_DeviceIdAndStartTimeBefore(
        String deviceId,
        Instant end,
        Pageable pageable
);
List<TripEntity> findAllByDevice_DeviceIdAndStartTimeBetween(String deviceId, Instant since, Instant end);
List<TripEntity> findAllByDevice_DeviceId(String deviceId);
}
