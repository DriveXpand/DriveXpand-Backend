package com.example.drivebackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.example.drivebackend.dto.TelemetryIngestRequest;
import com.example.drivebackend.dto.TelemetryResponse;
import com.example.drivebackend.entities.TelemetryEntity;

@Mapper(componentModel = "spring")
public interface TelemetryMapper {

    long EPOCH_2000_OFFSET_SECONDS = 946684800L;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "device", ignore = true)
    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "startTime", source = "start_time", qualifiedByName = "epoch2000ToInstant")
    @Mapping(target = "endTime", source = "end_time", qualifiedByName = "epoch2000ToInstant")
    TelemetryEntity toEntity(TelemetryIngestRequest request);

    @Mapping(source = "device.deviceId", target = "deviceId")
    @Mapping(source = "trip.id", target = "tripId")
    @Mapping(source = "startTime", target = "start_time")
    @Mapping(source = "endTime", target = "end_time")
    TelemetryResponse toDto(TelemetryEntity sample);

    @Named("epoch2000ToInstant")
    default java.time.Instant epoch2000ToInstant(Long seconds) {
        if (seconds == null) {
            return null;
        }
        return java.time.Instant.ofEpochSecond(seconds + EPOCH_2000_OFFSET_SECONDS);
    }
}
