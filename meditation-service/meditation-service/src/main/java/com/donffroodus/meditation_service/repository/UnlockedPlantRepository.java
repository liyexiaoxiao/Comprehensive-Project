package com.donffroodus.meditation_service.repository;

import com.donffroodus.meditation_service.entity.UnlockedPlant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnlockedPlantRepository extends JpaRepository<UnlockedPlant, Long> {
    List<UnlockedPlant> findByUserId(Long userId);
    boolean existsByUserIdAndPlantId(Long userId, Integer plantId);
}
