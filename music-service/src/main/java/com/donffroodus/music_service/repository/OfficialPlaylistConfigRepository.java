package com.donffroodus.music_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.OfficialPlaylistConfig;

@Repository
public interface OfficialPlaylistConfigRepository extends JpaRepository<OfficialPlaylistConfig, String> {

	List<OfficialPlaylistConfig> findAllByOrderBySortOrderAscPlaylistKeyAsc();

	Optional<OfficialPlaylistConfig> findByPlaylistKey(String playlistKey);
}
