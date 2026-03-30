package com.donffroodus.music_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.EmotionTag;

@Repository
public interface EmotionTagRepository extends JpaRepository<EmotionTag, Long> {

	Optional<EmotionTag> findByTagName(String tagName);
}
