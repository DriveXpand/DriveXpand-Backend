package com.example.drivebackend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repair_note")
@Getter
@Setter
@NoArgsConstructor
public class RepairNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "note_text", length = 2000, nullable = false)
    private String noteText;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(name = "note_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal notePrice;
}

