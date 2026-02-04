package com.example.drivebackend.dto;

public record TripUpdateRequest(
        String startLocation,
        String endLocation
) {
}
