package com.donffroodus.social_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.social_service.entity.MoodDiary;

public interface MoodDiaryRepository extends JpaRepository<MoodDiary, Long> {

	List<MoodDiary> findByUserIdOrderByDateDescCreatedAtDesc(Long userId);

	Optional<MoodDiary> findByIdAndUserId(Long id, Long userId);

	List<MoodDiary> findByUserIdAndDate(Long userId, LocalDate date);
}
