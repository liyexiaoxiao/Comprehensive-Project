package com.donffroodus.meditation_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "unlocked_plant", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "plant_id"})
})
public class UnlockedPlant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "plant_id", nullable = false)
    private Integer plantId;
    
    @CreationTimestamp
    @Column(name = "unlocked_at", updatable = false)
    private LocalDateTime unlockedAt;
}
