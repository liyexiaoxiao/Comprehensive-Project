package com.donffroodus.meditation_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "garden_inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "item_type", "item_id"})
})
public class GardenInventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "item_type", nullable = false)
    private String itemType; // "SEED" or "FRUIT"
    
    @Column(name = "item_id", nullable = false)
    private Integer itemId;
    
    @Column(nullable = false)
    private Integer count = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
