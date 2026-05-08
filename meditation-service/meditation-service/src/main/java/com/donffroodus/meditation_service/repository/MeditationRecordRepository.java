package com.donffroodus.meditation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.donffroodus.meditation_service.entity.MeditationRecord;

@Repository
public interface MeditationRecordRepository extends JpaRepository<MeditationRecord, Long> {
    @Transactional
    void deleteByUserId(Long userId);
}
