package com.donffroodus.meditation_service.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MeditationRequest {
    private LocalDateTime startTime; // 格式为 ISO 8601，例如 "2024-06-01T14:30:00"
    private Integer duration;
    private Long musicId;
    private Long imageId;
}
