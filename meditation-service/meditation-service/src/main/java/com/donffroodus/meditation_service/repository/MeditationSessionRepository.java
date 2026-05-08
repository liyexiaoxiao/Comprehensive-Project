package com.donffroodus.meditation_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.donffroodus.meditation_service.entity.MeditationSession;

@Repository
public interface MeditationSessionRepository extends JpaRepository<MeditationSession, Long> {
    Optional<MeditationSession> findByUserId(Long userId);
    @Transactional
    void deleteByUserId(Long userId);
}
