package com.example.drivebackend.entities;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "device_id", referencedColumnName = "deviceId")
    @JsonIgnore
    private List<RepairNoteEntity> repairNotes = new ArrayList<>();

    @JsonIgnore
    @Column(name = "note_photo", columnDefinition = "bytea")
    private byte[] notePhoto;

    @Column(name = "note_photo_content_type", length = 100)
    private String notePhotoContentType;
}
