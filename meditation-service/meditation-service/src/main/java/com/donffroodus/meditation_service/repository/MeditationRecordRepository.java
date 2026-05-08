package com.donffroodus.meditation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.meditation_service.entity.MeditationRecord;

public interface MeditationRecordRepository extends JpaRepository<MeditationRecord, Long> {
    
}
