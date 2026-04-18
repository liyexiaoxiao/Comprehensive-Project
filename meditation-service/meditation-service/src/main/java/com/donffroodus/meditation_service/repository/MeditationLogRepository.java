package com.donffroodus.meditation_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.meditation_service.entity.MeditationLog;

@Repository
public interface MeditationLogRepository extends JpaRepository<MeditationLog, Long> {
    List<MeditationLog> findByUserIdOrderByStartTimeDesc(Long userId);
}