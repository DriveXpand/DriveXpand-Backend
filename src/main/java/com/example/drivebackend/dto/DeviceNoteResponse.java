package com.example.drivebackend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DeviceNoteResponse(
        String noteText,
        Instant noteTimestamp,
        BigDecimal notePrice,
        boolean hasPhoto,
        String notePhotoContentType
) {
}

