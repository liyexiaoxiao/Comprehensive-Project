package com.donffroodus.meditation_service.dto;

import lombok.Data;

@Data
public class MiniMissionRequest {
    private String title;
    private String description;
    private int rewardValue;
    private boolean isActive;
}
