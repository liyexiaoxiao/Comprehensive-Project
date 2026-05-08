package com.donffroodus.meditation_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "meditation_record")
@Data
public class MeditationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    private Long userId;
    private LocalDateTime startTime;
    private Integer actualDuration;
    
    // 我们定义一个简单的 String 或 Enum 来存储状态
    private String status; 
    
    private Long musicId;
    private Long imageId;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
