package com.donffroodus.meditation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.meditation_service.entity.MiniMission;

@Repository
public interface MiniMissionRepository extends JpaRepository<MiniMission, Long> {
    boolean existsByTitle(String title);
}
