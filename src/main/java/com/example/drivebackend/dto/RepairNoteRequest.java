package com.example.drivebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RepairNoteRequest(
        @NotNull
        @Size(max = 2000)
        String noteText,

        @NotNull
        LocalDate noteDate,

        @NotNull
        BigDecimal notePrice
) {
}

