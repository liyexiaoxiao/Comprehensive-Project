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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.donffroodus.music_service.entity.EmotionTag;
import com.donffroodus.music_service.entity.MusicResource;
import com.donffroodus.music_service.entity.MusicTagMapping;
import com.donffroodus.music_service.entity.UserPreference;
import com.donffroodus.music_service.repository.EmotionTagRepository;
import com.donffroodus.music_service.repository.MusicResourceRepository;
import com.donffroodus.music_service.repository.MusicTagMappingRepository;
import com.donffroodus.music_service.repository.UserPreferenceRepository;

/**
 * 音乐服务 HTTP API：曲库与情绪标签、用户偏好，以及基于情绪/上下文的推荐。
 * <p>Swagger 展示说明需使用 {@link Operation} 等注解。</p>
 */
@Tag(name = "音乐服务", description = "曲库、情绪标签、用户偏好与推荐。经网关访问时路径前缀为 /api/music")
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

	/** 列出全部音乐资源。 */
	@Operation(summary = "列出全部音乐资源")
	@GetMapping("/music-resources")
	public List<MusicResource> listMusic() {
		return musicResourceRepository.findAll();
	}

	/** 按主键获取单首音乐详情。 */
	@Operation(summary = "获取单首音乐详情")
	@GetMapping("/music-resources/{id}")
	public ResponseEntity<MusicResource> getMusic(@PathVariable("id") Long id) {
		return musicResourceRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/** 查询某首音乐已绑定的情绪标签及来源。 */
	@Operation(summary = "查询音乐已绑定的情绪标签", description = "返回 mappingId、tagId、tagName、source")
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

	/** 列出全部情绪标签字典。 */
	@Operation(summary = "列出全部情绪标签")
	@GetMapping("/emotion-tags")
	public List<EmotionTag> listEmotionTags() {
		return emotionTagRepository.findAll();
	}

	/** 查询当前用户对各音乐的偏好（喜欢 / 不喜欢）。 */
	@Operation(summary = "列出当前用户的音乐偏好", description = "preferenceType：1 喜欢，-1 不喜欢（黑名单），2 收藏")
	@GetMapping("/me/music-preferences")
	public List<UserPreference> listUserPreferences(
			@Parameter(name = "X-User-Id", description = "用户 ID（网关从 JWT 注入）", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return userPreferenceRepository.findByUserId(userId);
	}

	/** 创建或更新当前用户对某首音乐的偏好。 */
	@Operation(summary = "创建或更新音乐偏好", description = "preferenceType 允许 1(喜欢), -1(不喜欢), 2(收藏)")
	@PostMapping("/me/music-preferences")
	public ResponseEntity<UserPreference> upsertMusicPreference(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestBody MusicPreferenceRequest request) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (request == null || request.musicId() == null || request.preferenceType() == null) {
			return ResponseEntity.badRequest().build();
		}
		if (request.preferenceType() != 1 && request.preferenceType() != -1 && request.preferenceType() != 2) {
			return ResponseEntity.badRequest().build();
		}

		Optional<UserPreference> existing = userPreferenceRepository.findByUserIdAndMusicIdAndPreferenceType(userId, request.musicId(), request.preferenceType());
		UserPreference pref = existing.orElseGet(UserPreference::new);
		pref.setUserId(userId);
		pref.setMusicId(request.musicId());
		pref.setPreferenceType(request.preferenceType());

		return ResponseEntity.ok(userPreferenceRepository.save(pref));
	}

	/** 删除当前用户对某首音乐的偏好记录。 */
	@Operation(summary = "删除某首音乐的偏好记录")
	@DeleteMapping("/me/music-preferences/{musicId}/{preferenceType}")
	public ResponseEntity<Void> deleteMusicPreference(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("musicId") String musicId,
			@PathVariable("preferenceType") Integer preferenceType) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		Optional<UserPreference> existing = userPreferenceRepository.findByUserIdAndMusicIdAndPreferenceType(userId, musicId, preferenceType);
		if (existing.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		// 通过删除实体本身，避免派生删除方法在某些场景下触发异常
		userPreferenceRepository.delete(existing.get());
		return ResponseEntity.ok().build();
	}

	/** 按情绪标签推荐候选曲目，并结合用户偏好排序与过滤黑名单。 */
	@Operation(summary = "按情绪标签推荐", description = "结合用户偏好：喜欢优先，黑名单排除；limit 默认 10")
	@PostMapping("/me/music-recommendations/by-emotion")
	public ResponseEntity<List<MusicResource>> recommendByEmotion(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
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
		Map<String, Integer> preferenceTypeByMusicId = userPreferenceRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(
						UserPreference::getMusicId,
						UserPreference::getPreferenceType,
						(a, b) -> a));

		List<MusicResource> result = candidates.stream()
				.filter(m -> {
					Integer t = preferenceTypeByMusicId.get(String.valueOf(m.getId()));
					return t == null || t != -1;
				})
				.sorted(Comparator
						.comparingInt((MusicResource m) -> {
							Integer t = preferenceTypeByMusicId.get(String.valueOf(m.getId()));
							return (t != null && t == 1) ? 0 : 1;
						})
						.thenComparing(MusicResource::getId))
				.limit(limit)
				.toList();

		return ResponseEntity.ok(result);
	}

	/** 基于当前播放曲目（及可选情绪标签）推荐下一首，排除当前曲并考虑偏好。 */
	@Operation(summary = "推荐下一首", description = "可传 emotionTagId，否则根据当前曲目标签推断；limit 默认 1")
	@PostMapping("/me/music-recommendations/next")
	public ResponseEntity<List<MusicResource>> recommendNext(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
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
		Map<String, Integer> preferenceTypeByMusicId = userPreferenceRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(
						UserPreference::getMusicId,
						UserPreference::getPreferenceType,
						(a, b) -> a));

		List<MusicResource> result = candidates.stream()
				.filter(m -> {
					Integer t = preferenceTypeByMusicId.get(String.valueOf(m.getId()));
					return t == null || t != -1;
				})
				.sorted(Comparator
						.comparingInt((MusicResource m) -> {
							Integer t = preferenceTypeByMusicId.get(String.valueOf(m.getId()));
							return (t != null && t == 1) ? 0 : 1;
						})
						.thenComparing(MusicResource::getId))
				.limit(limit)
				.toList();

		return ResponseEntity.ok(result);
	}

	/** 新建音乐资源（上传者取自 X-User-Id）。 */
	@Operation(summary = "新建音乐资源", description = "上传者 uploaderId 取自 X-User-Id；title、fileUrl 必填")
	@PostMapping("/music-resources")
	public ResponseEntity<MusicResource> createMusic(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
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

	/** 更新已有音乐资源的元数据与文件地址等。 */
	@Operation(summary = "更新音乐资源元数据", description = "title、fileUrl 必填")
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

	/** 删除音乐及其标签映射。 */
	@Operation(summary = "删除音乐资源", description = "同时删除该曲目的标签映射")
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

	/** 覆盖写入某首音乐的情绪标签绑定（先清空旧映射再保存新标签集）。 */
	@Operation(summary = "覆盖写入音乐的情绪标签", description = "先清空旧映射；tagIds 须全部存在于字典；source 默认 manual")
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

	/** 按名称创建情绪标签；已存在则返回已有记录。 */
	@Operation(summary = "创建情绪标签", description = "同名已存在则返回已有记录，不重复插入")
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

	/** 更新情绪标签名称（禁止与其他标签重名）。 */
	@Operation(summary = "更新情绪标签名称", description = "新名称不得与其他标签重名")
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

	/** 删除情绪标签并清理相关音乐映射。 */
	@Operation(summary = "删除情绪标签", description = "同步删除 music_tag_mapping 中相关映射")
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

	public record MusicPreferenceRequest(String musicId, Integer preferenceType) {
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
