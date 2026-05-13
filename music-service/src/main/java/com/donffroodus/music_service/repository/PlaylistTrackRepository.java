package com.donffroodus.music_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.PlaylistTrack;

@Repository
public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {

	List<PlaylistTrack> findByPlaylistIdOrderBySortOrderAscIdAsc(Long playlistId);

	List<PlaylistTrack> findByPlaylistIdInOrderBySortOrderAscIdAsc(List<Long> playlistIds);

	Optional<PlaylistTrack> findByPlaylistIdAndMusicId(Long playlistId, String musicId);

	boolean existsByPlaylistIdAndMusicId(Long playlistId, String musicId);

	void deleteByPlaylistIdAndMusicId(Long playlistId, String musicId);

	void deleteByPlaylistId(Long playlistId);

	List<PlaylistTrack> findByPlaylistId(Long playlistId);

	@Query("SELECT COALESCE(MAX(t.sortOrder), 0) FROM PlaylistTrack t WHERE t.playlistId = :playlistId")
	int findMaxSortOrderByPlaylistId(@Param("playlistId") Long playlistId);
}
