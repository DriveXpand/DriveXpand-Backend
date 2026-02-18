package com.example.drivebackend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.drivebackend.entities.RepairNoteEntity;

public interface RepairNoteRepository extends JpaRepository<RepairNoteEntity, UUID> {

    Page<RepairNoteEntity> findByDeviceIdOrderByNoteDateDesc(String deviceId, Pageable pageable);

    Optional<RepairNoteEntity> findByIdAndDeviceId(UUID id, String deviceId);
}

