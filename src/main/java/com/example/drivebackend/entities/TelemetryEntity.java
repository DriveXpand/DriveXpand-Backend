package com.example.drivebackend.entities;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "telemetry")
@Getter
@Setter
@NoArgsConstructor
public class TelemetryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "device_id", referencedColumnName = "deviceId", nullable = false)
    private DeviceEntity device;

    @ManyToOne
    @JoinColumn(name = "trip_id", referencedColumnName = "id")
    private TripEntity trip;

    @Column(name="start_time", nullable = false)
    private Instant startTime;

    @Column(name="end_time")
    private Instant endTime;

    //@Column(columnDefinition = "jsonb")
    //@JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = TelemetryMetricsConverter.class)
    @Column(name = "timed_data", columnDefinition = "TEXT") // 'text' statt 'jsonb' für H2-Kompatibilität
    private Map<String, Object> timed_data;

    @Convert(converter = TelemetryMetricsConverter.class)
    @Column(name = "aggregated_data", columnDefinition = "TEXT") // 'text' statt 'jsonb' für H2-Kompatibilität
    private Map<String, Object> aggregated_data;
}