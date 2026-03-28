package com.donffroodus.music_service.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.music_service.entity.EmotionTag;
import com.donffroodus.music_service.entity.MusicResource;
import com.donffroodus.music_service.entity.MusicTagMapping;
import com.donffroodus.music_service.entity.UserPreference;
import com.donffroodus.music_service.repository.EmotionTagRepository;
import com.donffroodus.music_service.repository.MusicResourceRepository;
import com.donffroodus.music_service.repository.MusicTagMappingRepository;
import com.donffroodus.music_service.repository.UserPreferenceRepository;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class MusicApiController {

	private final MusicResourceRepository musicResourceRepository;
	private final EmotionTagRepository emotionTagRepository;
	private final MusicTagMappingRepository musicTagMappingRepository;
	private final UserPreferenceRepository userPreferenceRepository;

	public MusicApiController(
			MusicResourceRepository musicResourceRepository,
			EmotionTagRepository emotionTagRepository,
			MusicTagMappingRepository musicTagMappingRepository,
			UserPreferenceRepository userPreferenceRepository) {
		this.musicResourceRepository = musicResourceRepository;
		this.emotionTagRepository = emotionTagRepository;
		this.musicTagMappingRepository = musicTagMappingRepository;
		this.userPreferenceRepository = userPreferenceRepository;
	}

	@GetMapping("/music-resources")
	public List<MusicResource> listMusic() {
		return musicResourceRepository.findAll();
	}

	@GetMapping("/music-resources/{id}")
	public ResponseEntity<MusicResource> getMusic(@PathVariable("id") Long id) {
		return musicResourceRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/music-resources/{id}/tags")
	public ResponseEntity<List<Map<String, Object>>> getMusicTags(@PathVariable("id") Long id) {
		if (musicResourceRepository.findById(id).isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		List<MusicTagMapping> mappings = musicTagMappingRepository.findByMusicId(id);
		Map<Long, String> tagNames = emotionTagRepository.findAll().stream()
				.collect(Collectors.toMap(EmotionTag::getId, EmotionTag::getTagName));
		List<Map<String, Object>> body = mappings.stream()
				.map(m -> Map.<String, Object>of(
						"mappingId", m.getId(),
						"tagId", m.getTagId(),
						"tagName", tagNames.getOrDefault(m.getTagId(), ""),
						"source", m.getSource() != null ? m.getSource() : ""))
				.toList();
		return ResponseEntity.ok(body);
	}

	@GetMapping("/emotion-tags")
	public List<EmotionTag> listEmotionTags() {
		return emotionTagRepository.findAll();
	}

	@GetMapping("/users/{userId}/music-preferences")
	public List<UserPreference> listUserPreferences(@PathVariable("userId") Long userId) {
		return userPreferenceRepository.findByUserId(userId);
	}
}
