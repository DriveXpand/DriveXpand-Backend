package com.example.drivebackend.services;

import java.util.Optional;

import com.example.drivebackend.dto.DeviceNoteRequest;
import com.example.drivebackend.dto.DeviceNoteResponse;

public interface DeviceNoteService {
    Optional<DeviceNoteResponse> getNote(String deviceId);

    Optional<DeviceNoteResponse> updateNote(String deviceId, DeviceNoteRequest request);

    Optional<DeviceNotePhoto> getNotePhoto(String deviceId);

    Optional<DeviceNotePhoto> updateNotePhoto(String deviceId, byte[] content, String contentType);

    record DeviceNotePhoto(byte[] content, String contentType) {
    }
}

