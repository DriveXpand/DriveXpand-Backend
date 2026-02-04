package com.example.drivebackend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.drivebackend.entities.TelemetryEntity;

public interface TelemetrySampleRepository extends JpaRepository<TelemetryEntity, UUID> {

    Optional<TelemetryEntity> findTopByDevice_DeviceIdOrderByStartTimeDesc(String deviceId);

    @Query("SELECT t FROM TelemetryEntity t WHERE t.device.deviceId = :deviceId AND t.startTime >= :since AND t.startTime <= :end ORDER BY t.startTime ASC")
    List<TelemetryEntity> findAllByDeviceIdInRange(
        @Param("deviceId") String deviceId,
        @Param("since") Instant since,
        @Param("end") Instant end
    );

    @Query("SELECT t FROM TelemetryEntity t WHERE t.device.deviceId = :deviceId AND t.trip.id = :tripId AND t.startTime >= :since AND t.startTime <= :end ORDER BY t.startTime ASC")
    List<TelemetryEntity> findAllByDeviceIdAndTripIdInRange(
        @Param("deviceId") String deviceId,
        @Param("tripId") UUID tripId,
        @Param("since") Instant since,
        @Param("end") Instant end
    );

    List<TelemetryEntity> findAllByDevice_DeviceIdOrderByStartTimeAsc(String deviceId);

    List<TelemetryEntity> findAllByDevice_DeviceIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(String deviceId, Instant since);

    List<TelemetryEntity> findAllByDevice_DeviceIdAndStartTimeLessThanEqualOrderByStartTimeAsc(String deviceId, Instant end);

    List<TelemetryEntity> findAllByDevice_DeviceIdAndTrip_IdOrderByStartTimeAsc(String deviceId, UUID tripId);

    List<TelemetryEntity> findAllByDevice_DeviceIdAndTrip_IdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(String deviceId, UUID tripId, Instant since);

    List<TelemetryEntity> findAllByDevice_DeviceIdAndTrip_IdAndStartTimeLessThanEqualOrderByStartTimeAsc(String deviceId, UUID tripId, Instant end);
}
