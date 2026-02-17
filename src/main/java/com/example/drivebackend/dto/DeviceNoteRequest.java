package com.example.drivebackend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DeviceNoteRequest(
        String noteText,
        Instant noteTimestamp,
        BigDecimal notePrice
) {
}

