package com.donffroodus.music_service.web;

import java.util.Comparator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.transaction.annotation.Transactional;

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

	@GetMapping("/me/music-preferences")
	public List<UserPreference> listUserPreferences(@RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return userPreferenceRepository.findByUserId(userId);
	}

	@PostMapping("/me/music-preferences")
	public ResponseEntity<UserPreference> upsertMusicPreference(
			@RequestHeader("X-User-Id") String xUserId,
			@RequestBody MusicPreferenceRequest request) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (request == null || request.musicId() == null || request.preferenceType() == null) {
			return ResponseEntity.badRequest().build();
		}
		if (request.preferenceType() != 1 && request.preferenceType() != -1) {
			return ResponseEntity.badRequest().build();
		}

		Optional<UserPreference> existing = userPreferenceRepository.findByUserIdAndMusicId(userId, request.musicId());
		UserPreference pref = existing.orElseGet(UserPreference::new);
		pref.setUserId(userId);
		pref.setMusicId(request.musicId());
		pref.setPreferenceType(request.preferenceType());

		return ResponseEntity.ok(userPreferenceRepository.save(pref));
	}

	@DeleteMapping("/me/music-preferences/{musicId}")
	public ResponseEntity<Void> deleteMusicPreference(
			@RequestHeader("X-User-Id") String xUserId,
			@PathVariable("musicId") Long musicId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		Optional<UserPreference> existing = userPreferenceRepository.findByUserIdAndMusicId(userId, musicId);
		if (existing.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		// 通过删除实体本身，避免派生删除方法在某些场景下触发异常
		userPreferenceRepository.delete(existing.get());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/me/music-recommendations/by-emotion")
	public ResponseEntity<List<MusicResource>> recommendByEmotion(
			@RequestHeader("X-User-Id") String xUserId,
			@RequestBody MusicRecommendationRequest request) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (request == null || request.emotionTagId() == null) {
			return ResponseEntity.badRequest().build();
		}

		int limit = request.limit() != null && request.limit() > 0 ? request.limit() : 10;

		// 1) 根据情绪标签找候选音乐
		List<MusicTagMapping> mappings = musicTagMappingRepository.findByTagId(request.emotionTagId());
		if (mappings.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		List<Long> candidateMusicIds = mappings.stream()
				.map(MusicTagMapping::getMusicId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();

		List<MusicResource> candidates = musicResourceRepository.findAllById(candidateMusicIds);

		// 2) 读取用户偏好：-1(黑名单) 过滤；1(喜欢) 提升排序靠前
		Map<Long, Integer> preferenceTypeByMusicId = userPreferenceRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(
						UserPreference::getMusicId,
						UserPreference::getPreferenceType,
						(a, b) -> a));

		List<MusicResource> result = candidates.stream()
				.filter(m -> {
					Integer t = preferenceTypeByMusicId.get(m.getId());
					return t == null || t != -1;
				})
				.sorted(Comparator
						.comparingInt((MusicResource m) -> {
							Integer t = preferenceTypeByMusicId.get(m.getId());
							return (t != null && t == 1) ? 0 : 1;
						})
						.thenComparing(MusicResource::getId))
				.limit(limit)
				.toList();

		return ResponseEntity.ok(result);
	}

	@PostMapping("/me/music-recommendations/next")
	public ResponseEntity<List<MusicResource>> recommendNext(
			@RequestHeader("X-User-Id") String xUserId,
			@RequestBody NextMusicRequest request) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (request == null || request.currentMusicId() == null) {
			return ResponseEntity.badRequest().build();
		}

		int limit = request.limit() != null && request.limit() > 0 ? request.limit() : 1;

		// 1) 如果前端传了 emotionTagId，就直接用它；否则根据当前音乐反推它有哪些情绪标签
		List<Long> emotionTagIds;
		if (request.emotionTagId() != null) {
			emotionTagIds = List.of(request.emotionTagId());
		} else {
			List<MusicTagMapping> currentMappings = musicTagMappingRepository.findByMusicId(request.currentMusicId());
			emotionTagIds = currentMappings.stream()
					.map(MusicTagMapping::getTagId)
					.filter(Objects::nonNull)
					.distinct()
					.toList();
		}

		if (emotionTagIds.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		// 2) 找候选音乐：这些情绪标签 => music_tag_mapping => music_id
		List<Long> candidateMusicIds = emotionTagIds.stream()
				.flatMap(tagId -> musicTagMappingRepository.findByTagId(tagId).stream())
				.map(MusicTagMapping::getMusicId)
				.filter(Objects::nonNull)
				.distinct()
				.filter(id -> !id.equals(request.currentMusicId()))
				.toList();

		if (candidateMusicIds.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		List<MusicResource> candidates = musicResourceRepository.findAllById(candidateMusicIds);

		// 3) 读取用户偏好：-1(黑名单) 过滤；1(喜欢) 排在前面
		Map<Long, Integer> preferenceTypeByMusicId = userPreferenceRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(
						UserPreference::getMusicId,
						UserPreference::getPreferenceType,
						(a, b) -> a));

		List<MusicResource> result = candidates.stream()
				.filter(m -> {
					Integer t = preferenceTypeByMusicId.get(m.getId());
					return t == null || t != -1;
				})
				.sorted(Comparator
						.comparingInt((MusicResource m) -> {
							Integer t = preferenceTypeByMusicId.get(m.getId());
							return (t != null && t == 1) ? 0 : 1;
						})
						.thenComparing(MusicResource::getId))
				.limit(limit)
				.toList();

		return ResponseEntity.ok(result);
	}

	@PostMapping("/music-resources")
	public ResponseEntity<MusicResource> createMusic(
			@RequestHeader("X-User-Id") String xUserId,
			@RequestBody MusicResourceWriteRequest request) {
		if (request == null || request.title() == null || request.fileUrl() == null) {
			return ResponseEntity.badRequest().build();
		}

		MusicResource music = new MusicResource();
		music.setTitle(request.title());
		music.setArtist(request.artist());
		music.setDuration(request.duration());
		music.setFileUrl(request.fileUrl());
		music.setCoverUrl(request.coverUrl());
		music.setSource(request.source());
		music.setUploaderId(GatewayAuthSupport.requireUserId(xUserId));
		return ResponseEntity.ok(musicResourceRepository.save(music));
	}

	@PutMapping("/music-resources/{musicId}")
	public ResponseEntity<MusicResource> updateMusic(
			@PathVariable("musicId") Long musicId,
			@RequestBody MusicResourceWriteRequest request) {
		if (request == null || request.title() == null || request.fileUrl() == null) {
			return ResponseEntity.badRequest().build();
		}

		return musicResourceRepository.findById(musicId)
				.map(existing -> {
					existing.setTitle(request.title());
					existing.setArtist(request.artist());
					existing.setDuration(request.duration());
					existing.setFileUrl(request.fileUrl());
					existing.setCoverUrl(request.coverUrl());
					existing.setSource(request.source());
					return ResponseEntity.ok(musicResourceRepository.save(existing));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/music-resources/{musicId}")
	public ResponseEntity<Void> deleteMusic(@PathVariable("musicId") Long musicId) {
		if (musicResourceRepository.findById(musicId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// 避免派生删除方法在某些场景触发运行时异常：先查询再 deleteAll
		List<MusicTagMapping> mappings = musicTagMappingRepository.findByMusicId(musicId);
		if (!mappings.isEmpty()) {
			musicTagMappingRepository.deleteAll(mappings);
		}
		musicResourceRepository.deleteById(musicId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 为某首歌批量绑定情绪标签（覆盖写入：先删旧 mapping，再写入新 tagIds）。
	 */
	@PostMapping("/music-resources/{musicId}/tags")
	@Transactional
	public ResponseEntity<List<MusicTagMapping>> replaceMusicTags(
			@PathVariable("musicId") Long musicId,
			@RequestBody MusicTagsWriteRequest request) {
		if (request == null || request.tagIds() == null || request.tagIds().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		if (musicResourceRepository.findById(musicId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Set<Long> distinctTagIds = new HashSet<>(request.tagIds());
		if (distinctTagIds.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		// 校验 tagIds 是否都存在
		List<EmotionTag> tags = emotionTagRepository.findAllById(distinctTagIds);
		Set<Long> existingTagIds = tags.stream().map(EmotionTag::getId).collect(Collectors.toSet());
		if (existingTagIds.size() != distinctTagIds.size()) {
			return ResponseEntity.badRequest().build();
		}

		String source = request.source();
		if (source == null || source.isBlank()) {
			source = "manual";
		}

		// 覆盖写入
		List<MusicTagMapping> oldMappings = musicTagMappingRepository.findByMusicId(musicId);
		if (!oldMappings.isEmpty()) {
			musicTagMappingRepository.deleteAll(oldMappings);
		}
		List<MusicTagMapping> saved = new ArrayList<>();
		for (Long tagId : distinctTagIds) {
			MusicTagMapping mapping = new MusicTagMapping();
			mapping.setMusicId(musicId);
			mapping.setTagId(tagId);
			mapping.setSource(source);
			saved.add(musicTagMappingRepository.save(mapping));
		}

		return ResponseEntity.ok(saved);
	}

	@PostMapping("/emotion-tags")
	public ResponseEntity<EmotionTag> createEmotionTag(@RequestBody EmotionTagWriteRequest request) {
		if (request == null || request.tagName() == null || request.tagName().isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		return emotionTagRepository.findByTagName(request.tagName())
				.map(ResponseEntity::ok)
				.orElseGet(() -> {
					EmotionTag tag = new EmotionTag();
					tag.setTagName(request.tagName());
					return ResponseEntity.ok(emotionTagRepository.save(tag));
				});
	}

	@PutMapping("/emotion-tags/{tagId}")
	public ResponseEntity<EmotionTag> updateEmotionTag(
			@PathVariable("tagId") Long tagId,
			@RequestBody EmotionTagWriteRequest request) {
		if (request == null || request.tagName() == null || request.tagName().isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		// 如果 tagName 已存在但不是自己，则拒绝，避免 unique 冲突
		Optional<EmotionTag> other = emotionTagRepository.findByTagName(request.tagName());
		if (other.isPresent() && !other.get().getId().equals(tagId)) {
			return ResponseEntity.badRequest().build();
		}

		return emotionTagRepository.findById(tagId)
				.map(existing -> {
					existing.setTagName(request.tagName());
					return ResponseEntity.ok(emotionTagRepository.save(existing));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/emotion-tags/{tagId}")
	public ResponseEntity<Void> deleteEmotionTag(@PathVariable("tagId") Long tagId) {
		if (emotionTagRepository.findById(tagId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		// tag 没有被实际外键约束时，需要同步清理 mapping
		List<MusicTagMapping> mappings = musicTagMappingRepository.findByTagId(tagId);
		if (!mappings.isEmpty()) {
			musicTagMappingRepository.deleteAll(mappings);
		}
		emotionTagRepository.deleteById(tagId);
		return ResponseEntity.ok().build();
	}

	public static record MusicPreferenceRequest(Long musicId, Integer preferenceType) {
	}

	public static record MusicRecommendationRequest(Long emotionTagId, Integer limit) {
	}

	public static record NextMusicRequest(
			Long currentMusicId,
			Long emotionTagId,
			Integer limit) {
	}

	public static record MusicResourceWriteRequest(
			String title,
			String artist,
			Integer duration,
			String fileUrl,
			String coverUrl,
			String source) {
	}

	public static record MusicTagsWriteRequest(
			List<Long> tagIds,
			String source) {
	}

	public static record EmotionTagWriteRequest(
			String tagName) {
	}
}
