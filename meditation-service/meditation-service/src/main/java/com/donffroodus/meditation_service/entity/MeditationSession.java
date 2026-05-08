package com.donffroodus.meditation_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "meditation_session")
@Data
public class MeditationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    private Long userId;
    private LocalDateTime startTime;
    private Integer targetDuration;
    private Integer musicId;
    private Integer imageId;
}
