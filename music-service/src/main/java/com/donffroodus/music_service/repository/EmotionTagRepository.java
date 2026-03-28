package com.donffroodus.music_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.EmotionTag;

@Repository
public interface EmotionTagRepository extends JpaRepository<EmotionTag, Long> {
}
