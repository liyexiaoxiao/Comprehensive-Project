package com.donffroodus.music_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.donffroodus.music_service.entity.PlaylistTrack;

@Repository
public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
    List<PlaylistTrack> findByPlaylistId(Long playlistId);
    void deleteByPlaylistIdAndMusicId(Long playlistId, String musicId);
    void deleteByPlaylistId(Long playlistId);
}
