package com.donffroodus.music_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.Playlist;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

	List<Playlist> findByUserIdOrderByIdDesc(Long userId);

	List<Playlist> findByUserId(Long userId);

	Optional<Playlist> findByIdAndUserId(Long id, Long userId);
}
