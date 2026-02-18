package com.example.drivebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RepairNoteResponse(
        UUID id,
        String noteText,
        LocalDate noteDate,
        BigDecimal notePrice
) {
}

