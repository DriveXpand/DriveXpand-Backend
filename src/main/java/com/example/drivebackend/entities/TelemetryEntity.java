package com.example.drivebackend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telemetry")
@Getter
@Setter
@NoArgsConstructor
public class TelemetryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    //TODO : Add relation to DeviceEntity
    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private Instant recordedAt;

    private Long startTime;
    private Long endTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode aggregatedData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metrics;
}