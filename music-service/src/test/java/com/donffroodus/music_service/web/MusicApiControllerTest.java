package com.donffroodus.music_service.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.donffroodus.music_service.entity.EmotionTag;
import com.donffroodus.music_service.entity.MusicResource;
import com.donffroodus.music_service.entity.MusicTagMapping;
import com.donffroodus.music_service.entity.Playlist;
import com.donffroodus.music_service.entity.PlaylistTrack;
import com.donffroodus.music_service.entity.UserPreference;
import com.donffroodus.music_service.repository.EmotionTagRepository;
import com.donffroodus.music_service.repository.MusicResourceRepository;
import com.donffroodus.music_service.repository.MusicTagMappingRepository;
import com.donffroodus.music_service.repository.PlaylistRepository;
import com.donffroodus.music_service.repository.PlaylistTrackRepository;
import com.donffroodus.music_service.repository.UserPreferenceRepository;

@ExtendWith(MockitoExtension.class)
class MusicApiControllerTest {

    @Mock
    private MusicResourceRepository musicResourceRepository;

    @Mock
    private EmotionTagRepository emotionTagRepository;

    @Mock
    private MusicTagMappingRepository musicTagMappingRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;

    private MusicApiController controller;

    @BeforeEach
    void setUp() {
        controller = new MusicApiController(
                musicResourceRepository,
                emotionTagRepository,
                musicTagMappingRepository,
                userPreferenceRepository,
                playlistRepository,
                playlistTrackRepository);
    }

    @Test
    void listMusicShouldReturnAllWhenKeywordIsBlank() {
        List<MusicResource> allMusic = List.of(music(1L, "Calm Sea"), music(2L, "Morning Light"));
        when(musicResourceRepository.findAll()).thenReturn(allMusic);

        List<MusicResource> result = controller.listMusic("   ");

        assertEquals(allMusic, result);
        verify(musicResourceRepository).findAll();
        verify(musicResourceRepository, never()).searchByTitleOrArtistIgnoreCase(any());
    }

    @Test
    void listMusicShouldSearchWhenKeywordIsPresent() {
        List<MusicResource> matches = List.of(music(1L, "Calm Sea"));
        when(musicResourceRepository.searchByTitleOrArtistIgnoreCase("calm")).thenReturn(matches);

        List<MusicResource> result = controller.listMusic(" calm ");

        assertEquals(matches, result);
        verify(musicResourceRepository).searchByTitleOrArtistIgnoreCase("calm");
    }

    @Test
    void listMusicByIdsShouldRejectEmptyOrInvalidIds() {
        ResponseEntity<List<MusicResource>> response = controller.listMusicByIds("bad, ,x");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(musicResourceRepository, never()).findAllById(any());
    }

    @Test
    void listMusicByIdsShouldDeduplicateAndKeepRequestOrder() {
        MusicResource first = music(1L, "First");
        MusicResource second = music(2L, "Second");
        when(musicResourceRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(first, second));

        ResponseEntity<List<MusicResource>> response = controller.listMusicByIds("2,bad,1,2");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(List.of(2L, 1L), response.getBody().stream().map(MusicResource::getId).toList());
    }

    @Test
    void listUserPreferencesShouldRejectInvalidPreferenceType() {
        ResponseEntity<List<UserPreference>> response = controller.listUserPreferences("7", 99);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userPreferenceRepository, never()).findByUserIdAndPreferenceType(any(), any());
    }

    @Test
    void upsertMusicPreferenceShouldUpdateExistingPreference() {
        UserPreference existing = preference(7L, "3", 1);
        when(userPreferenceRepository.findByUserIdAndMusicIdAndPreferenceType(7L, "3", 1))
                .thenReturn(Optional.of(existing));
        when(userPreferenceRepository.save(existing)).thenReturn(existing);

        ResponseEntity<UserPreference> response = controller.upsertMusicPreference(
                "7",
                new MusicApiController.MusicPreferenceRequest("3", 1));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userPreferenceRepository).save(existing);
        assertEquals(7L, existing.getUserId());
        assertEquals("3", existing.getMusicId());
        assertEquals(1, existing.getPreferenceType());
    }

    @Test
    void recommendByEmotionShouldBoostLikedMusicAndFilterBlockedMusic() {
        when(musicTagMappingRepository.findByTagId(10L)).thenReturn(List.of(
                mapping(1L, 10L),
                mapping(2L, 10L),
                mapping(3L, 10L)));
        when(musicResourceRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(
                music(1L, "Neutral"),
                music(2L, "Blocked"),
                music(3L, "Liked")));
        when(userPreferenceRepository.findByUserId(7L)).thenReturn(List.of(
                preference(7L, "3", 1),
                preference(7L, "2", -1)));

        ResponseEntity<List<MusicResource>> response = controller.recommendByEmotion(
                "7",
                new MusicApiController.MusicRecommendationRequest(10L, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(List.of(3L, 1L), response.getBody().stream().map(MusicResource::getId).toList());
    }

    @Test
    void recommendByEmotionShouldReturnEmptyListWhenTagHasNoMappings() {
        when(musicTagMappingRepository.findByTagId(10L)).thenReturn(List.of());

        ResponseEntity<List<MusicResource>> response = controller.recommendByEmotion(
                "7",
                new MusicApiController.MusicRecommendationRequest(10L, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(musicResourceRepository, never()).findAllById(any());
    }

    @Test
    void createMyPlaylistShouldRejectBlankName() {
        ResponseEntity<MusicApiController.PlaylistResponse> response = controller.createMyPlaylist(
                "7",
                new MusicApiController.PlaylistWriteRequest("   ", "desc", "cover.jpg"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(playlistRepository, never()).save(any());
    }

    @Test
    void addTrackToMyPlaylistShouldRejectDuplicateTrack() {
        when(playlistRepository.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(playlist(5L, 7L, "Sleep")));
        when(musicResourceRepository.findById(3L)).thenReturn(Optional.of(music(3L, "Rain")));
        when(playlistTrackRepository.existsByPlaylistIdAndMusicId(5L, "3")).thenReturn(true);

        ResponseEntity<MusicApiController.PlaylistTrackResponse> response = controller.addTrackToMyPlaylist(
                "7",
                5L,
                new MusicApiController.PlaylistTrackWriteRequest("3"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(playlistTrackRepository, never()).save(any());
    }

    @Test
    void addTrackToMyPlaylistShouldSaveTrackWithNextSortOrder() {
        when(playlistRepository.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(playlist(5L, 7L, "Sleep")));
        when(musicResourceRepository.findById(3L)).thenReturn(Optional.of(music(3L, "Rain")));
        when(playlistTrackRepository.existsByPlaylistIdAndMusicId(5L, "3")).thenReturn(false);
        when(playlistTrackRepository.findMaxSortOrderByPlaylistId(5L)).thenReturn(4);
        when(playlistTrackRepository.save(any(PlaylistTrack.class))).thenAnswer(invocation -> {
            PlaylistTrack saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        ResponseEntity<MusicApiController.PlaylistTrackResponse> response = controller.addTrackToMyPlaylist(
                "7",
                5L,
                new MusicApiController.PlaylistTrackWriteRequest("3"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<PlaylistTrack> trackCaptor = ArgumentCaptor.forClass(PlaylistTrack.class);
        verify(playlistTrackRepository).save(trackCaptor.capture());
        assertEquals(5L, trackCaptor.getValue().getPlaylistId());
        assertEquals("3", trackCaptor.getValue().getMusicId());
        assertEquals(5, trackCaptor.getValue().getSortOrder());
    }

    private static MusicResource music(Long id, String title) {
        MusicResource music = new MusicResource();
        music.setId(id);
        music.setTitle(title);
        music.setFileUrl("/music/" + id + ".mp3");
        return music;
    }

    private static MusicTagMapping mapping(Long musicId, Long tagId) {
        MusicTagMapping mapping = new MusicTagMapping();
        mapping.setMusicId(musicId);
        mapping.setTagId(tagId);
        return mapping;
    }

    private static UserPreference preference(Long userId, String musicId, Integer preferenceType) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setMusicId(musicId);
        preference.setPreferenceType(preferenceType);
        return preference;
    }

    private static Playlist playlist(Long id, Long userId, String name) {
        Playlist playlist = new Playlist();
        playlist.setId(id);
        playlist.setUserId(userId);
        playlist.setName(name);
        return playlist;
    }
}
