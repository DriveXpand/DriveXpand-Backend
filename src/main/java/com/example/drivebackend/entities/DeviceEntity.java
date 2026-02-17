package com.example.drivebackend.entities;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "device")
@Getter
@Setter
@NoArgsConstructor
public class DeviceEntity {

    @Id
    private String deviceId; // gleiche deviceId wie in TelemetryEntity

    @Column(nullable = false)
    private String name = "Porsche 911 Carrera 4 GTS"; // Default-Wert

    @Column(name = "note_text", length = 2000)
    private String noteText;

    @Column(name = "note_timestamp")
    private Instant noteTimestamp;

    @Column(name = "note_price", precision = 12, scale = 2)
    private BigDecimal notePrice;

    @JsonIgnore
    @Column(name = "note_photo", columnDefinition = "bytea")
    private byte[] notePhoto;

    @Column(name = "note_photo_content_type", length = 100)
    private String notePhotoContentType;
}
