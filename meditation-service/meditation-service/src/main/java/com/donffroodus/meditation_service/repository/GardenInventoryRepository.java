package com.donffroodus.meditation_service.repository;

import com.donffroodus.meditation_service.entity.GardenInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GardenInventoryRepository extends JpaRepository<GardenInventory, Long> {
    List<GardenInventory> findByUserId(Long userId);
    List<GardenInventory> findByUserIdAndItemType(Long userId, String itemType);
    Optional<GardenInventory> findByUserIdAndItemTypeAndItemId(Long userId, String itemType, Integer itemId);
}
