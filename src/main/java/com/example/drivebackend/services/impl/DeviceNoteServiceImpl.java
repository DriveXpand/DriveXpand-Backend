package com.example.drivebackend.services.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.drivebackend.dto.DeviceNoteRequest;
import com.example.drivebackend.dto.DeviceNoteResponse;
import com.example.drivebackend.entities.DeviceEntity;
import com.example.drivebackend.repository.DeviceRepository;
import com.example.drivebackend.services.DeviceNoteService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceNoteServiceImpl implements DeviceNoteService {

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceNoteResponse> getNote(String deviceId) {
        return deviceRepository.findById(deviceId).map(this::toNoteResponse);
    }

    @Override
    public Optional<DeviceNoteResponse> updateNote(String deviceId, DeviceNoteRequest request) {
        return deviceRepository.findById(deviceId)
                .map(device -> {
                    if (request.noteText() != null) {
                        device.setNoteText(request.noteText());
                    }
                    if (request.noteTimestamp() != null) {
                        device.setNoteTimestamp(request.noteTimestamp());
                    }
                    if (request.notePrice() != null) {
                        device.setNotePrice(request.notePrice());
                    }
                    DeviceEntity saved = deviceRepository.save(device);
                    return toNoteResponse(saved);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceNotePhoto> getNotePhoto(String deviceId) {
        return deviceRepository.findById(deviceId)
                .filter(device -> device.getNotePhoto() != null)
                .map(device -> new DeviceNotePhoto(device.getNotePhoto(), device.getNotePhotoContentType()));
    }

    @Override
    public Optional<DeviceNotePhoto> updateNotePhoto(String deviceId, byte[] content, String contentType) {
        return deviceRepository.findById(deviceId)
                .map(device -> {
                    device.setNotePhoto(content);
                    device.setNotePhotoContentType(contentType);
                    DeviceEntity saved = deviceRepository.save(device);
                    return new DeviceNotePhoto(saved.getNotePhoto(), saved.getNotePhotoContentType());
                });
    }

    private DeviceNoteResponse toNoteResponse(DeviceEntity device) {
        boolean hasPhoto = device.getNotePhoto() != null && device.getNotePhoto().length > 0;
        return new DeviceNoteResponse(
                device.getNoteText(),
                device.getNoteTimestamp(),
                device.getNotePrice(),
                hasPhoto,
                device.getNotePhotoContentType()
        );
    }
}

