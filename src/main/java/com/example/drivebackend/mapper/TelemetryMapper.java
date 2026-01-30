package com.example.drivebackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.drivebackend.dto.TelemetryIngestRequest;
import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.entities.TelemetryEntity;

@Mapper(componentModel = "spring")
public interface TelemetryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "device", ignore = true)
    @Mapping(source = "start_time", target = "startTime")
    TelemetryEntity toEntity(TelemetryIngestRequest request);
    
    @Mapping(source = "device.deviceId", target = "deviceId")
    @Mapping(source = "startTime", target = "start_time")
    TelemetryResponse toDto(TelemetryEntity sample);
}
