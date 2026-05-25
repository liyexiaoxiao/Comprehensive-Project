package com.donffroodus.meditation_service.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MiniMissionAdminResponse {
    private Long id;
    private String title;
    private String description;
    private int rewardValue;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
