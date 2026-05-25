package com.donffroodus.meditation_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "mini_mission_log")
@Data
public class MiniMissionLog {

    public enum MiniMissionStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long miniMissionId;

    @Enumerated(EnumType.STRING)
    private MiniMissionStatus status;
    private int earnedValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
