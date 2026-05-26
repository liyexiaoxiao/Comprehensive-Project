package com.donffroodus.meditation_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.meditation_service.entity.MiniMissionLog;

public interface MiniMissionLogRepository extends JpaRepository<MiniMissionLog, Long> {
    List<MiniMissionLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<MiniMissionLog> findByUserIdAndStatus(Long userId, MiniMissionLog.MiniMissionStatus status);
    List<MiniMissionLog> findByUserId(Long userId);
    boolean existsByStatusAndUserId(MiniMissionLog.MiniMissionStatus status, Long userId);
}
