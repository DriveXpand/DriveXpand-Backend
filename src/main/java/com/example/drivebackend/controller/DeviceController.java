package com.example.drivebackend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.drivebackend.entities.DeviceEntity;
import com.example.drivebackend.repository.DeviceRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceRepository deviceRepository;

    @Operation(summary = "Get all devices", description = "Retrieve a list of all registered devices")
    @ApiResponse(responseCode = "200", description = "List of devices")
    @GetMapping
    public List<DeviceEntity> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Operation(summary = "Update device name", description = "Update the name of a specific device")
    @ApiResponse(responseCode = "200", description = "Device updated successfully")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PutMapping("/{deviceId}/name")
    public ResponseEntity<DeviceEntity> updateDeviceName(
            @Parameter(description = "Device ID", required = true) @PathVariable String deviceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New device name", required = true) @RequestBody String name) {
        return deviceRepository.findById(deviceId)
                .map(device -> {
                    device.setName(name);
                    deviceRepository.save(device);
                    return ResponseEntity.ok(device);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}