package com.donffroodus.meditation_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

enum MiniMissionStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

@Entity
@Table(name = "mini_mission_log")
@Data
public class MiniMissionLog {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long miniMissionId;
    private MiniMissionStatus status;
    private int earnedValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
