package com.donffroodus.meditation_service.dto;

import lombok.Data;

@Data
public class MeditationCountDownStartRequest {
    private Integer duration; // 以秒为单位
    private Long musicId;
    private Long imageId;
}
