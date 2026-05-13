package com.donffroodus.music_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.UserPreference;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

	List<UserPreference> findByUserId(Long userId);

	List<UserPreference> findByUserIdAndPreferenceType(Long userId, Integer preferenceType);

	Optional<UserPreference> findByUserIdAndMusicId(Long userId, Long musicId);

	void deleteByUserIdAndMusicIdAndPreferenceType(Long userId, String musicId, Integer preferenceType);
}
