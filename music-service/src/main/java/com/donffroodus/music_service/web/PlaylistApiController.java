package com.donffroodus.music_service.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.donffroodus.music_service.entity.MusicResource;
import com.donffroodus.music_service.entity.Playlist;
import com.donffroodus.music_service.entity.PlaylistTrack;
import com.donffroodus.music_service.repository.MusicResourceRepository;
import com.donffroodus.music_service.repository.PlaylistRepository;
import com.donffroodus.music_service.repository.PlaylistTrackRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "歌单服务", description = "用户自建歌单与歌单内音乐管理")
@RestController
@RequestMapping("/api/v1/playlists")
@CrossOrigin(origins = "*")
public class PlaylistApiController {

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final MusicResourceRepository musicResourceRepository;

    public PlaylistApiController(
            PlaylistRepository playlistRepository,
            PlaylistTrackRepository playlistTrackRepository,
            MusicResourceRepository musicResourceRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.musicResourceRepository = musicResourceRepository;
    }

    public record PlaylistCreateRequest(String name, String description, String coverUrl) {}

    @Operation(summary = "列出当前用户的歌单")
    @GetMapping
    public List<Playlist> listMyPlaylists(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
        Long userId = GatewayAuthSupport.requireUserId(xUserId);
        return playlistRepository.findByUserId(userId);
    }

    @Operation(summary = "创建歌单")
    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
            @RequestBody PlaylistCreateRequest request) {
        Long userId = GatewayAuthSupport.requireUserId(xUserId);
        if (request == null || request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Playlist playlist = new Playlist();
        playlist.setUserId(userId);
        playlist.setName(request.name());
        playlist.setDescription(request.description());
        playlist.setCoverUrl(request.coverUrl());
        return ResponseEntity.ok(playlistRepository.save(playlist));
    }

    @Operation(summary = "删除歌单")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePlaylist(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
            @PathVariable("id") Long id) {
        Long userId = GatewayAuthSupport.requireUserId(xUserId);
        Optional<Playlist> existing = playlistRepository.findById(id);
        if (existing.isEmpty() || !existing.get().getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        playlistTrackRepository.deleteByPlaylistId(id);
        playlistRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "获取歌单中的音乐列表(返回 ID 列表)")
    @GetMapping("/{id}/tracks")
    public ResponseEntity<List<String>> getPlaylistTracks(@PathVariable("id") Long id) {
        if (playlistRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<PlaylistTrack> tracks = playlistTrackRepository.findByPlaylistId(id);
        List<String> musicIds = tracks.stream().map(PlaylistTrack::getMusicId).toList();
        return ResponseEntity.ok(musicIds);
    }

    public record PlaylistTrackAddRequest(String musicId) {}

    @Operation(summary = "向歌单添加音乐")
    @PostMapping("/{id}/tracks")
    public ResponseEntity<PlaylistTrack> addTrackToPlaylist(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
            @PathVariable("id") Long id,
            @RequestBody PlaylistTrackAddRequest request) {
        Long userId = GatewayAuthSupport.requireUserId(xUserId);
        Optional<Playlist> playlist = playlistRepository.findById(id);
        if (playlist.isEmpty() || !playlist.get().getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        if (request == null || request.musicId() == null) {
            return ResponseEntity.badRequest().build();
        }
        PlaylistTrack track = new PlaylistTrack();
        track.setPlaylistId(id);
        track.setMusicId(request.musicId());
        return ResponseEntity.ok(playlistTrackRepository.save(track));
    }

    @Operation(summary = "从歌单移除音乐")
    @DeleteMapping("/{id}/tracks/{musicId}")
    @Transactional
    public ResponseEntity<Void> removeTrackFromPlaylist(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
            @PathVariable("id") Long id,
            @PathVariable("musicId") String musicId) {
        Long userId = GatewayAuthSupport.requireUserId(xUserId);
        Optional<Playlist> playlist = playlistRepository.findById(id);
        if (playlist.isEmpty() || !playlist.get().getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        playlistTrackRepository.deleteByPlaylistIdAndMusicId(id, musicId);
        return ResponseEntity.ok().build();
    }
}
