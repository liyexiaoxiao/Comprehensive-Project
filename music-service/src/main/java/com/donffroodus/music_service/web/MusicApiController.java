package com.donffroodus.music_service.web;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.donffroodus.music_service.entity.EmotionTag;
import com.donffroodus.music_service.entity.MusicResource;
import com.donffroodus.music_service.entity.MusicTagMapping;
import com.donffroodus.music_service.entity.OfficialPlaylistConfig;
import com.donffroodus.music_service.entity.Playlist;
import com.donffroodus.music_service.entity.PlaylistTrack;
import com.donffroodus.music_service.entity.UserPreference;
import com.donffroodus.music_service.repository.EmotionTagRepository;
import com.donffroodus.music_service.repository.MusicResourceRepository;
import com.donffroodus.music_service.repository.MusicTagMappingRepository;
import com.donffroodus.music_service.repository.OfficialPlaylistConfigRepository;
import com.donffroodus.music_service.repository.PlaylistRepository;
import com.donffroodus.music_service.repository.PlaylistTrackRepository;
import com.donffroodus.music_service.repository.UserPreferenceRepository;
import com.donffroodus.music_service.service.ai.AiEmotionTaggingService;
import com.donffroodus.music_service.service.ai.AiEmotionTaggingService.AiTaggingOptions;
import com.donffroodus.music_service.service.ai.AiEmotionTaggingService.AiTaggingResult;

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
	private final OfficialPlaylistConfigRepository officialPlaylistConfigRepository;
	private final PlaylistRepository playlistRepository;
	private final PlaylistTrackRepository playlistTrackRepository;
	private final AiEmotionTaggingService aiEmotionTaggingService;

	public MusicApiController(
			MusicResourceRepository musicResourceRepository,
			EmotionTagRepository emotionTagRepository,
			MusicTagMappingRepository musicTagMappingRepository,
			UserPreferenceRepository userPreferenceRepository,
			OfficialPlaylistConfigRepository officialPlaylistConfigRepository,
			PlaylistRepository playlistRepository,
			PlaylistTrackRepository playlistTrackRepository,
			AiEmotionTaggingService aiEmotionTaggingService) {
		this.musicResourceRepository = musicResourceRepository;
		this.emotionTagRepository = emotionTagRepository;
		this.musicTagMappingRepository = musicTagMappingRepository;
		this.userPreferenceRepository = userPreferenceRepository;
		this.officialPlaylistConfigRepository = officialPlaylistConfigRepository;
		this.playlistRepository = playlistRepository;
		this.playlistTrackRepository = playlistTrackRepository;
		this.aiEmotionTaggingService = aiEmotionTaggingService;
	}

	/** preference_type：1=喜欢，2=收藏（推荐排序与喜欢同等加权），-1=黑名单。 */
	private static boolean isValidPreferenceType(Integer t) {
		return t != null && (t == 1 || t == 2 || t == -1);
	}

	/** 喜欢或收藏：推荐时均视为正向偏好（收藏默认享受“喜欢”级排序）。 */
	private static boolean isBoostPreference(Integer t) {
		return t != null && (t == 1 || t == 2);
	}

	private static boolean isBlocked(Integer t) {
		return t != null && t == -1;
	}

	/** 解析逗号分隔的 Long id；去重且保持首次出现顺序；最多 max 个。非法片段跳过。 */
	private static List<Long> parseCommaSeparatedIds(String raw, int max) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : raw.split(",")) {
			String s = part.strip();
			if (s.isEmpty()) {
				continue;
			}
			try {
				long id = Long.parseLong(s);
				if (out.contains(id)) {
					continue;
				}
				out.add(id);
				if (out.size() >= max) {
					break;
				}
			} catch (NumberFormatException ex) {
				// skip invalid token
			}
		}
		return out;
	}

	private static final String OFFICIAL_PLAYLIST_LIST_SEPARATOR = "\n";

	private static final List<OfficialPlaylistConfigResponse> DEFAULT_OFFICIAL_PLAYLISTS = List.of(
			new OfficialPlaylistConfigResponse(
					"official-neutral",
					"官方·中性歌单",
					"适合情绪平稳、不过度起伏，想安静陪伴自己的时刻。",
					"中性",
					"neutral",
					List.of("neutral"),
					List.of("中性", "neutral"),
					0),
			new OfficialPlaylistConfigResponse(
					"official-joy",
					"官方·高兴歌单",
					"适合开心、轻快、想把好心情继续延长的时刻。",
					"高兴",
					"joy",
					List.of("joy"),
					List.of("高兴", "喜悦", "快乐", "开心", "joy", "happy"),
					1),
			new OfficialPlaylistConfigResponse(
					"official-sadness",
					"官方·悲伤歌单",
					"允许情绪被温柔接住，先陪你慢慢消化低落。",
					"悲伤",
					"sadness",
					List.of("sadness"),
					List.of("悲伤", "sad", "sadness", "孤独"),
					2),
			new OfficialPlaylistConfigResponse(
					"official-fear",
					"官方·恐惧歌单",
					"给焦虑和不安留出缓冲区，慢慢找回安全感。",
					"恐惧",
					"fear",
					List.of("fear"),
					List.of("恐惧", "fear", "焦虑", "anxious", "anxiety"),
					3),
			new OfficialPlaylistConfigResponse(
					"official-disgust",
					"官方·厌恶歌单",
					"适合需要与排斥感、烦躁感拉开距离，慢慢整理情绪的时刻。",
					"厌恶",
					"disgust",
					List.of("disgust"),
					List.of("厌恶", "disgust"),
					4),
			new OfficialPlaylistConfigResponse(
					"official-calm",
					"官方·平静歌单",
					"适合放慢呼吸、慢慢松下来，让身心重新沉静的时刻。",
					"平静",
					"calm",
					List.of("calm"),
					List.of("平静", "calm", "放松", "舒缓"),
					5),
			new OfficialPlaylistConfigResponse(
					"official-surprise",
					"官方·惊讶歌单",
					"适合感受新鲜、意外和被触动时的跳跃心情。",
					"惊讶",
					"surprise",
					List.of("surprise"),
					List.of("惊讶", "惊喜", "surprise"),
					6),
			new OfficialPlaylistConfigResponse(
					"official-anger",
					"官方·愤怒歌单",
					"帮助释放紧绷与躁动，让身体和思绪重新稳定。",
					"愤怒",
					"anger",
					List.of("anger"),
					List.of("愤怒", "anger", "angry"),
					7));

	private static List<String> splitOfficialList(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(raw.split("\\R"))
				.map(String::strip)
				.filter(s -> !s.isEmpty())
				.distinct()
				.toList();
	}

	private static String joinOfficialList(List<String> values) {
		if (values == null || values.isEmpty()) {
			return "";
		}
		return values.stream()
				.filter(Objects::nonNull)
				.map(String::strip)
				.filter(s -> !s.isEmpty())
				.distinct()
				.collect(Collectors.joining(OFFICIAL_PLAYLIST_LIST_SEPARATOR));
	}

	private static OfficialPlaylistConfig toOfficialPlaylistEntity(OfficialPlaylistConfigResponse response) {
		OfficialPlaylistConfig entity = new OfficialPlaylistConfig();
		entity.setPlaylistKey(response.playlistKey());
		entity.setName(response.name());
		entity.setDescription(response.description());
		entity.setTagName(response.tagName());
		entity.setCoverEmotion(response.coverEmotion());
		entity.setEmotionKeys(joinOfficialList(response.emotions()));
		entity.setKeywords(joinOfficialList(response.keywords()));
		entity.setSortOrder(response.sortOrder());
		return entity;
	}

	private static OfficialPlaylistConfigResponse toOfficialPlaylistResponse(OfficialPlaylistConfig entity) {
		return new OfficialPlaylistConfigResponse(
				entity.getPlaylistKey(),
				entity.getName(),
				entity.getDescription(),
				entity.getTagName(),
				entity.getCoverEmotion(),
				splitOfficialList(entity.getEmotionKeys()),
				splitOfficialList(entity.getKeywords()),
				entity.getSortOrder() == null ? 0 : entity.getSortOrder());
	}

	private static Optional<OfficialPlaylistConfigResponse> findDefaultOfficialPlaylist(String playlistKey) {
		return DEFAULT_OFFICIAL_PLAYLISTS.stream()
				.filter(item -> Objects.equals(item.playlistKey(), playlistKey))
				.findFirst();
	}

	private static void copyOfficialPlaylistDefaults(OfficialPlaylistConfig entity, OfficialPlaylistConfigResponse defaults) {
		entity.setPlaylistKey(defaults.playlistKey());
		entity.setName(defaults.name());
		entity.setDescription(defaults.description());
		entity.setTagName(defaults.tagName());
		entity.setCoverEmotion(defaults.coverEmotion());
		entity.setEmotionKeys(joinOfficialList(defaults.emotions()));
		entity.setKeywords(joinOfficialList(defaults.keywords()));
		entity.setSortOrder(defaults.sortOrder());
	}

	private static boolean isLegacyOfficialPlaylistConfig(OfficialPlaylistConfig entity, OfficialPlaylistConfigResponse defaults) {
		if (entity == null) {
			return false;
		}
		if (Objects.equals(entity.getTagName(), defaults.tagName())) {
			return false;
		}
		return switch (entity.getPlaylistKey()) {
			case "official-neutral" -> Objects.equals(entity.getTagName(), "平静");
			case "official-joy" -> Objects.equals(entity.getTagName(), "喜悦");
			case "official-surprise" -> Objects.equals(entity.getTagName(), "惊喜");
			default -> false;
		};
	}

	private List<OfficialPlaylistConfig> ensureOfficialPlaylistConfigs() {
		Map<String, OfficialPlaylistConfig> existingByKey = officialPlaylistConfigRepository.findAllByOrderBySortOrderAscPlaylistKeyAsc().stream()
				.collect(Collectors.toMap(OfficialPlaylistConfig::getPlaylistKey, item -> item, (a, b) -> a, LinkedHashMap::new));

		List<OfficialPlaylistConfig> ordered = new ArrayList<>();
		for (OfficialPlaylistConfigResponse defaults : DEFAULT_OFFICIAL_PLAYLISTS) {
			OfficialPlaylistConfig entity = existingByKey.get(defaults.playlistKey());
			if (entity == null) {
				entity = officialPlaylistConfigRepository.save(toOfficialPlaylistEntity(defaults));
			} else if (isLegacyOfficialPlaylistConfig(entity, defaults)) {
				copyOfficialPlaylistDefaults(entity, defaults);
				entity = officialPlaylistConfigRepository.save(entity);
			}
			ordered.add(entity);
		}
		return ordered;
	}

	@Operation(summary = "列出官方情绪歌单配置", description = "首次访问会自动初始化默认八个官方歌单配置")
	@GetMapping("/official-playlists")
	public List<OfficialPlaylistConfigResponse> listOfficialPlaylists() {
		return ensureOfficialPlaylistConfigs().stream()
				.map(MusicApiController::toOfficialPlaylistResponse)
				.toList();
	}

	@Operation(summary = "更新单个官方情绪歌单配置", description = "若配置不存在则按默认项初始化后再覆盖保存")
	@PutMapping("/official-playlists/{playlistKey}")
	public ResponseEntity<OfficialPlaylistConfigResponse> updateOfficialPlaylist(
			@PathVariable("playlistKey") String playlistKey,
			@RequestBody OfficialPlaylistConfigWriteRequest request) {
		if (request == null
				|| request.name() == null || request.name().isBlank()
				|| request.tagName() == null || request.tagName().isBlank()
				|| request.coverEmotion() == null || request.coverEmotion().isBlank()
				|| request.emotions() == null || request.emotions().isEmpty()
				|| request.keywords() == null || request.keywords().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		Optional<OfficialPlaylistConfigResponse> defaults = findDefaultOfficialPlaylist(playlistKey);
		if (defaults.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		OfficialPlaylistConfig entity = officialPlaylistConfigRepository.findByPlaylistKey(playlistKey)
				.orElseGet(() -> toOfficialPlaylistEntity(defaults.get()));

		entity.setPlaylistKey(playlistKey);
		entity.setName(request.name().strip());
		entity.setDescription(request.description());
		entity.setTagName(request.tagName().strip());
		entity.setCoverEmotion(request.coverEmotion().strip());
		entity.setEmotionKeys(joinOfficialList(request.emotions()));
		entity.setKeywords(joinOfficialList(request.keywords()));
		entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : defaults.get().sortOrder());

		OfficialPlaylistConfig saved = officialPlaylistConfigRepository.save(entity);
		return ResponseEntity.ok(toOfficialPlaylistResponse(saved));
	}

	/** 列出音乐资源；可选 q 按标题或艺人模糊匹配（不区分大小写）。 */
	@Operation(summary = "列出音乐资源", description = "无 q 时返回全部；有 q 时在标题或 artist 中模糊匹配")
	@GetMapping("/music-resources")
	public List<MusicResource> listMusic(
			@Parameter(name = "q", description = "关键词，匹配 title 或 artist", in = ParameterIn.QUERY) @RequestParam(value = "q", required = false) String q) {
		if (q == null || q.isBlank()) {
		return musicResourceRepository.findAll();
		}
		String keyword = q.strip();
		return musicResourceRepository.searchByTitleOrArtistIgnoreCase(keyword);
	}

	/** 当前用户上传的音乐列表（按 music_id 降序）。 */
	@Operation(summary = "列出当前用户上传的音乐", description = "uploaderId 与 X-User-Id 一致")
	@GetMapping("/me/music-resources")
	public List<MusicResource> listMyUploadedMusic(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return musicResourceRepository.findByUploaderIdOrderByIdDesc(userId);
	}

	/**
	 * 按多个主键批量查询音乐；顺序与请求 ids 中首次出现的 id 一致；不存在的 id 跳过。
	 * 路径放在 /music-resources/{id} 之前，避免 "by-ids" 被当成 id。
	 */
	@Operation(summary = "按 id 批量查询音乐", description = "ids 为逗号分隔，如 1,2,3；最多 200 个；非法片段忽略")
	@GetMapping("/music-resources/by-ids")
	public ResponseEntity<List<MusicResource>> listMusicByIds(
			@Parameter(name = "ids", description = "逗号分隔的 music_id", in = ParameterIn.QUERY, required = true) @RequestParam("ids") String ids) {
		List<Long> idList = parseCommaSeparatedIds(ids, 200);
		if (idList.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		Map<Long, MusicResource> byId = musicResourceRepository.findAllById(idList).stream()
				.collect(Collectors.toMap(MusicResource::getId, m -> m, (a, b) -> a, HashMap::new));
		List<MusicResource> ordered = new ArrayList<>();
		for (Long id : idList) {
			MusicResource m = byId.get(id);
			if (m != null) {
				ordered.add(m);
			}
		}
		return ResponseEntity.ok(ordered);
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

	/** 删除某首音乐的一条标签映射（须 mapping 属于该 musicId）。 */
	@Operation(summary = "删除单条音乐-标签映射", description = "mapping 不属于该曲目时返回 404，避免误删")
	@DeleteMapping("/music-resources/{musicId}/tags/{mappingId}")
	@Transactional
	public ResponseEntity<Void> deleteOneMusicTagMapping(
			@PathVariable("musicId") Long musicId,
			@PathVariable("mappingId") Long mappingId) {
		Optional<MusicTagMapping> row = musicTagMappingRepository.findById(mappingId);
		if (row.isEmpty() || !Objects.equals(row.get().getMusicId(), musicId)) {
			return ResponseEntity.notFound().build();
		}
		musicTagMappingRepository.delete(row.get());
		return ResponseEntity.ok().build();
	}

	/** 列出全部情绪标签字典。 */
	@Operation(summary = "列出全部情绪标签")
	@GetMapping("/emotion-tags")
	public List<EmotionTag> listEmotionTags() {
		return emotionTagRepository.findAll();
	}

	/** 按精确标签名查询一条情绪标签（唯一约束）。 */
	@Operation(summary = "按名称查询情绪标签", description = "精确匹配 tag_name；不存在返回 404")
	@GetMapping("/emotion-tags/by-name")
	public ResponseEntity<EmotionTag> getEmotionTagByName(
			@Parameter(name = "name", description = "标签名，精确匹配", in = ParameterIn.QUERY, required = true) @RequestParam("name") String name) {
		if (name == null || name.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		return emotionTagRepository.findByTagName(name.strip())
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/** 查询当前用户对各音乐的偏好；可选按类型筛选。 */
	@Operation(summary = "列出当前用户的音乐偏好", description = "preferenceType：1 喜欢，2 收藏（推荐与喜欢同等加权），-1 黑名单；可选 query preferenceType 筛选")
	@GetMapping("/me/music-preferences")
	public ResponseEntity<List<UserPreference>> listUserPreferences(
			@Parameter(name = "X-User-Id", description = "用户 ID（网关从 JWT 注入）", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@Parameter(name = "preferenceType", description = "可选：1、2、-1", in = ParameterIn.QUERY) @RequestParam(value = "preferenceType", required = false) Integer preferenceType) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (preferenceType != null) {
			if (!isValidPreferenceType(preferenceType)) {
				return ResponseEntity.badRequest().build();
			}
			return ResponseEntity.ok(userPreferenceRepository.findByUserIdAndPreferenceType(userId, preferenceType));
		}
		return ResponseEntity.ok(userPreferenceRepository.findByUserId(userId));
	}

	/** 创建或更新当前用户对某首音乐的偏好。 */
	@Operation(summary = "创建或更新音乐偏好", description = "preferenceType：1 喜欢，2 收藏，-1 黑名单；收藏在推荐排序中与喜欢同等优先")
	@PostMapping("/me/music-preferences")
	public ResponseEntity<UserPreference> upsertMusicPreference(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestBody MusicPreferenceRequest request) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (request == null || request.musicId() == null || request.preferenceType() == null) {
			return ResponseEntity.badRequest().build();
		}
		if (!isValidPreferenceType(request.preferenceType())) {
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

	/** 查询当前用户对指定曲目的偏好（一行或不存在）。 */
	@Operation(summary = "查询当前用户对某首音乐的偏好")
	@GetMapping("/me/music-preferences/by-music/{musicId}")
	public ResponseEntity<UserPreference> getMusicPreferenceForMusic(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("musicId") String musicId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return userPreferenceRepository.findByUserIdAndMusicId(userId, musicId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/** 按情绪标签推荐候选曲目，并结合用户偏好排序与过滤黑名单。 */
	@Operation(summary = "按情绪标签推荐", description = "结合用户偏好：喜欢/收藏优先，黑名单排除；limit 默认 10")
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

		// 2) 读取用户偏好：-1(黑名单) 过滤；1(喜欢)/2(收藏) 提升排序靠前
		Map<String, Integer> preferenceTypeByMusicId = userPreferenceRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(
						UserPreference::getMusicId,
						UserPreference::getPreferenceType,
						(a, b) -> a));

		List<MusicResource> result = candidates.stream()
				.filter(m -> !isBlocked(preferenceTypeByMusicId.get(String.valueOf(m.getId()))))
				.sorted(Comparator
						.comparingInt((MusicResource m) -> isBoostPreference(preferenceTypeByMusicId.get(String.valueOf(m.getId()))) ? 0 : 1)
						.thenComparingInt((MusicResource m) -> {
							Integer t = preferenceTypeByMusicId.get(String.valueOf(m.getId()));
							if (t != null && t == 1) {
								return 0;
							}
							if (t != null && t == 2) {
								return 1;
							}
							return 2;
						})
						.thenComparing(MusicResource::getId))
				.limit(limit)
				.toList();

		return ResponseEntity.ok(result);
	}

	/** 基于当前播放曲目（及可选情绪标签）推荐下一首，排除当前曲并考虑偏好。 */
	@Operation(summary = "推荐下一首", description = "可传 emotionTagId，否则根据当前曲目标签推断；limit 默认 1；喜欢/收藏优先，黑名单排除")
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

		// 3) 读取用户偏好：-1(黑名单) 过滤；1(喜欢)/2(收藏) 排在前面
		Map<String, Integer> preferenceTypeByMusicId = userPreferenceRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(
						UserPreference::getMusicId,
						UserPreference::getPreferenceType,
						(a, b) -> a));

		List<MusicResource> result = candidates.stream()
				.filter(m -> !isBlocked(preferenceTypeByMusicId.get(String.valueOf(m.getId()))))
				.sorted(Comparator
						.comparingInt((MusicResource m) -> isBoostPreference(preferenceTypeByMusicId.get(String.valueOf(m.getId()))) ? 0 : 1)
						.thenComparingInt((MusicResource m) -> {
							Integer t = preferenceTypeByMusicId.get(String.valueOf(m.getId()));
							if (t != null && t == 1) {
								return 0;
							}
							if (t != null && t == 2) {
								return 1;
							}
							return 2;
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

	private static final int MAX_BATCH_TAG_ITEMS = 100;

	/** 覆盖写入：先删该曲全部映射，再写入；调用方须已校验 music 存在且 tagIds 非空且均在字典中。 */
	private List<MusicTagMapping> applyReplaceTagsForMusic(Long musicId, Set<Long> distinctTagIds, String source) {
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
		return saved;
	}

	/**
	 * 使用 Qwen3-Omni-Captioner 分析音频并推断情绪标签，默认写入 {@code source=ai} 的映射（保留人工标签）。
	 */
	@Operation(summary = "AI 自动打情绪标签",
			description = "调用阿里云 Qwen3-Omni-Captioner 生成英文描述，再映射为情绪标签；需配置 DASHSCOPE_API_KEY；"
					+ "默认仅替换 source=ai 的映射")
	@PostMapping("/music-resources/{musicId}/tags/ai")
	public ResponseEntity<AiTaggingResponse> aiTagMusicEmotions(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("musicId") Long musicId,
			@RequestBody(required = false) AiTaggingRequest request) {
		GatewayAuthSupport.requireUserId(xUserId);
		if (musicResourceRepository.findById(musicId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		boolean persist = request == null || request.persist() == null || request.persist();
		boolean replaceAiOnly = request == null || request.replaceAiOnly() == null || request.replaceAiOnly();
		Integer maxTags = request != null ? request.maxTags() : null;

		AiTaggingResult result = aiEmotionTaggingService.tagMusicByAi(
				musicId,
				new AiTaggingOptions(persist, replaceAiOnly, maxTags));

		Map<Long, String> tagNames = emotionTagRepository.findAllById(result.tagIds()).stream()
				.collect(Collectors.toMap(EmotionTag::getId, EmotionTag::getTagName));

		List<Map<String, Object>> mappingBodies = result.mappings().stream()
				.map(m -> Map.<String, Object>of(
						"mappingId", m.getId(),
						"tagId", m.getTagId(),
						"tagName", tagNames.getOrDefault(m.getTagId(), ""),
						"source", m.getSource() != null ? m.getSource() : ""))
				.toList();

		return ResponseEntity.ok(new AiTaggingResponse(
				result.musicId(),
				result.caption(),
				result.inferredEmotionKeys(),
				result.tagIds(),
				result.tagNames(),
				mappingBodies,
				result.persisted()));
	}

	@Operation(summary = "预览 AI 自动打情绪标签",
			description = "直接分析待上传音频文件并返回建议标签，不创建 music_resource，也不写入标签映射")
	@PostMapping(path = "/music-resources/tags/ai-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AiTaggingResponse> previewAiTagMusicEmotions(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestPart("file") MultipartFile file,
			@RequestParam(value = "maxTags", required = false) Integer maxTags,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "artist", required = false) String artist) throws Exception {
		GatewayAuthSupport.requireUserId(xUserId);
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		AiTaggingResult result = aiEmotionTaggingService.previewTagging(
				file.getOriginalFilename(),
				file.getContentType(),
				file.getBytes(),
				maxTags,
				title,
				artist);

		return ResponseEntity.ok(new AiTaggingResponse(
				null,
				result.caption(),
				result.inferredEmotionKeys(),
				result.tagIds(),
				result.tagNames(),
				List.of(),
				false));
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

		return ResponseEntity.ok(applyReplaceTagsForMusic(musicId, distinctTagIds, source));
	}

	/**
	 * 批量覆盖写入多首音乐的情绪标签（语义与单首 POST /music-resources/{musicId}/tags 一致）。
	 * 同一请求内若出现重复 musicId，以后出现的条目为准。全部校验通过后再一次性提交事务。
	 */
	@Operation(summary = "批量覆盖写入音乐情绪标签", description = "最多 100 条；每项须 musicId + 非空 tagIds；tagIds 须全部存在于字典；source 可逐项省略，默认 manual")
	@PostMapping("/music-resources/tags/batch")
	@Transactional
	public ResponseEntity<BatchReplaceMusicTagsResponse> batchReplaceMusicTags(
			@RequestBody BatchReplaceMusicTagsRequest body) {
		if (body == null || body.items() == null || body.items().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		if (body.items().size() > MAX_BATCH_TAG_ITEMS) {
			return ResponseEntity.badRequest().build();
		}

		// 同 batch 内重复 musicId：保留最后一次
		List<BatchReplaceMusicTagsItem> normalized = new ArrayList<>();
		Map<Long, BatchReplaceMusicTagsItem> byMusic = new LinkedHashMap<>();
		for (BatchReplaceMusicTagsItem item : body.items()) {
			if (item == null || item.musicId() == null || item.tagIds() == null || item.tagIds().isEmpty()) {
				return ResponseEntity.badRequest().build();
			}
			byMusic.put(item.musicId(), item);
		}
		normalized.addAll(byMusic.values());

		Set<Long> allMusicIds = new HashSet<>();
		Set<Long> allTagIds = new HashSet<>();
		for (BatchReplaceMusicTagsItem item : normalized) {
			allMusicIds.add(item.musicId());
			allTagIds.addAll(new HashSet<>(item.tagIds()));
		}

		List<MusicResource> musics = musicResourceRepository.findAllById(allMusicIds);
		if (musics.size() != allMusicIds.size()) {
			return ResponseEntity.badRequest().build();
		}

		List<EmotionTag> tags = emotionTagRepository.findAllById(allTagIds);
		Set<Long> existingTagIds = tags.stream().map(EmotionTag::getId).collect(Collectors.toSet());
		if (existingTagIds.size() != allTagIds.size()) {
			return ResponseEntity.badRequest().build();
		}

		for (BatchReplaceMusicTagsItem item : normalized) {
			Set<Long> distinct = new HashSet<>(item.tagIds());
			if (distinct.isEmpty()) {
				return ResponseEntity.badRequest().build();
			}
			if (!existingTagIds.containsAll(distinct)) {
				return ResponseEntity.badRequest().build();
			}
		}

		List<BatchReplaceMusicTagsResultItem> results = new ArrayList<>();
		for (BatchReplaceMusicTagsItem item : normalized) {
			Set<Long> distinct = new HashSet<>(item.tagIds());
			String source = item.source();
			if (source == null || source.isBlank()) {
				source = "manual";
			}
			List<MusicTagMapping> saved = applyReplaceTagsForMusic(item.musicId(), distinct, source);
			results.add(new BatchReplaceMusicTagsResultItem(item.musicId(), saved.size()));
		}
		return ResponseEntity.ok(new BatchReplaceMusicTagsResponse(results));
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

	// =============== 自建歌单 ===============

	/** 列出当前用户的全部歌单，并附带每个歌单的曲目（按 sort_order 升序）。 */
	@Operation(summary = "列出当前用户的歌单", description = "每个歌单嵌入曲目数组，曲目按 sort_order 升序")
	@GetMapping("/me/playlists")
	public List<PlaylistResponse> listMyPlaylists(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		List<Playlist> playlists = playlistRepository.findByUserIdOrderByIdDesc(userId);
		if (playlists.isEmpty()) {
			return List.of();
		}

		List<Long> playlistIds = playlists.stream().map(Playlist::getId).toList();
		List<PlaylistTrack> allTracks = playlistTrackRepository
				.findByPlaylistIdInOrderBySortOrderAscIdAsc(playlistIds);

		Set<Long> referencedMusicIds = allTracks.stream()
				.map(PlaylistTrack::getMusicId)
				.filter(id -> id != null && id.matches("\\d+"))
				.map(Long::valueOf)
				.collect(Collectors.toCollection(HashSet::new));
		Map<String, MusicResource> musicById = musicResourceRepository.findAllById(referencedMusicIds).stream()
				.collect(Collectors.toMap(m -> String.valueOf(m.getId()), m -> m, (a, b) -> a, HashMap::new));

		Map<Long, List<PlaylistTrackResponse>> tracksByPlaylist = new HashMap<>();
		for (PlaylistTrack t : allTracks) {
			tracksByPlaylist
					.computeIfAbsent(t.getPlaylistId(), k -> new ArrayList<>())
					.add(PlaylistTrackResponse.of(t, musicById.get(t.getMusicId())));
		}

		List<PlaylistResponse> result = new ArrayList<>();
		for (Playlist p : playlists) {
			result.add(PlaylistResponse.of(p, tracksByPlaylist.getOrDefault(p.getId(), List.of())));
		}
		return result;
	}

	/** 查询当前用户的单个歌单详情。 */
	@Operation(summary = "查看歌单详情", description = "返回歌单元信息与曲目数组（按 sort_order 升序）")
	@GetMapping("/me/playlists/{playlistId}")
	public ResponseEntity<PlaylistResponse> getMyPlaylist(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("playlistId") Long playlistId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		Optional<Playlist> playlist = playlistRepository.findByIdAndUserId(playlistId, userId);
		if (playlist.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		List<PlaylistTrack> tracks = playlistTrackRepository
				.findByPlaylistIdOrderBySortOrderAscIdAsc(playlistId);
		Set<Long> referencedMusicIds = tracks.stream()
				.map(PlaylistTrack::getMusicId)
				.filter(id -> id != null && id.matches("\\d+"))
				.map(Long::valueOf)
				.collect(Collectors.toCollection(HashSet::new));
		Map<String, MusicResource> musicById = musicResourceRepository.findAllById(referencedMusicIds).stream()
				.collect(Collectors.toMap(m -> String.valueOf(m.getId()), m -> m, (a, b) -> a, HashMap::new));

		List<PlaylistTrackResponse> trackBodies = tracks.stream()
				.map(t -> PlaylistTrackResponse.of(t, musicById.get(t.getMusicId())))
				.toList();
		return ResponseEntity.ok(PlaylistResponse.of(playlist.get(), trackBodies));
	}

	/** 新建当前用户的歌单。 */
	@Operation(summary = "新建歌单", description = "name 必填；description、coverUrl 可空")
	@PostMapping("/me/playlists")
	public ResponseEntity<PlaylistResponse> createMyPlaylist(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestBody PlaylistWriteRequest request) {
		if (request == null || request.name() == null || request.name().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);

		Playlist playlist = new Playlist();
		playlist.setUserId(userId);
		playlist.setName(request.name().strip());
		playlist.setDescription(request.description());
		playlist.setCoverUrl(request.coverUrl());
		Playlist saved = playlistRepository.save(playlist);
		return ResponseEntity.ok(PlaylistResponse.of(saved, List.of()));
	}

	/** 更新当前用户的歌单元信息。 */
	@Operation(summary = "更新歌单元信息", description = "可更新 name / description / coverUrl；name 必填")
	@PutMapping("/me/playlists/{playlistId}")
	public ResponseEntity<PlaylistResponse> updateMyPlaylist(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("playlistId") Long playlistId,
			@RequestBody PlaylistWriteRequest request) {
		if (request == null || request.name() == null || request.name().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return playlistRepository.findByIdAndUserId(playlistId, userId)
				.map(existing -> {
					existing.setName(request.name().strip());
					existing.setDescription(request.description());
					existing.setCoverUrl(request.coverUrl());
					Playlist saved = playlistRepository.save(existing);
					List<PlaylistTrack> tracks = playlistTrackRepository
							.findByPlaylistIdOrderBySortOrderAscIdAsc(playlistId);
					Set<Long> ids = tracks.stream().map(PlaylistTrack::getMusicId)
							.filter(id -> id != null && id.matches("\\d+"))
							.map(Long::valueOf)
							.collect(Collectors.toCollection(HashSet::new));
					Map<String, MusicResource> musicById = musicResourceRepository.findAllById(ids).stream()
							.collect(Collectors.toMap(m -> String.valueOf(m.getId()), m -> m, (a, b) -> a, HashMap::new));
					List<PlaylistTrackResponse> body = tracks.stream()
							.map(t -> PlaylistTrackResponse.of(t, musicById.get(t.getMusicId())))
							.toList();
					return ResponseEntity.ok(PlaylistResponse.of(saved, body));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 删除当前用户的歌单并清空其曲目。 */
	@Operation(summary = "删除歌单", description = "同时删除该歌单下全部曲目记录")
	@DeleteMapping("/me/playlists/{playlistId}")
	@Transactional
	public ResponseEntity<Void> deleteMyPlaylist(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("playlistId") Long playlistId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return playlistRepository.findByIdAndUserId(playlistId, userId)
				.map(playlist -> {
					playlistTrackRepository.deleteByPlaylistId(playlistId);
					playlistRepository.delete(playlist);
					return ResponseEntity.ok().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 向当前用户的歌单追加一首曲目（sort_order 取已有最大值 +1）。 */
	@Operation(summary = "向歌单追加曲目", description = "musicId 必填；曲目必须存在；同一歌单同一首歌不可重复，重复时返回 409")
	@PostMapping("/me/playlists/{playlistId}/tracks")
	@Transactional
	public ResponseEntity<PlaylistTrackResponse> addTrackToMyPlaylist(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("playlistId") Long playlistId,
			@RequestBody PlaylistTrackWriteRequest request) {
		if (request == null || request.musicId() == null) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);

		Optional<Playlist> playlist = playlistRepository.findByIdAndUserId(playlistId, userId);
		if (playlist.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		MusicResource music = null;
		if (request.musicId() != null && request.musicId().matches("\\d+")) {
			music = musicResourceRepository.findById(Long.valueOf(request.musicId())).orElse(null);
		}
		if (playlistTrackRepository.existsByPlaylistIdAndMusicId(playlistId, request.musicId())) {
			return ResponseEntity.status(409).build();
		}

		int nextOrder = playlistTrackRepository.findMaxSortOrderByPlaylistId(playlistId) + 1;
		PlaylistTrack track = new PlaylistTrack();
		track.setPlaylistId(playlistId);
		track.setMusicId(request.musicId());
		track.setSortOrder(nextOrder);
		PlaylistTrack saved = playlistTrackRepository.save(track);
		return ResponseEntity.ok(PlaylistTrackResponse.of(saved, music));
	}

	/** 从当前用户的歌单移除某首曲目（按 musicId）。 */
	@Operation(summary = "从歌单移除曲目")
	@DeleteMapping("/me/playlists/{playlistId}/tracks/{musicId}")
	@Transactional
	public ResponseEntity<Void> removeTrackFromMyPlaylist(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("playlistId") Long playlistId,
			@PathVariable("musicId") String musicId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		Optional<Playlist> playlist = playlistRepository.findByIdAndUserId(playlistId, userId);
		if (playlist.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return playlistTrackRepository.findByPlaylistIdAndMusicId(playlistId, musicId)
				.map(row -> {
					playlistTrackRepository.delete(row);
					return ResponseEntity.ok().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	public static record PlaylistWriteRequest(
			String name,
			String description,
			String coverUrl) {
	}

	public static record PlaylistTrackWriteRequest(
			String musicId) {
	}

	public static record PlaylistTrackResponse(
			Long playlistTrackId,
			Long playlistId,
			String musicId,
			Integer sortOrder,
			LocalDateTime createdAt,
			MusicResource music) {

		static PlaylistTrackResponse of(PlaylistTrack t, MusicResource music) {
			return new PlaylistTrackResponse(
					t.getId(),
					t.getPlaylistId(),
					t.getMusicId(),
					t.getSortOrder(),
					t.getCreatedAt(),
					music);
		}
	}

	public static record PlaylistResponse(
			Long playlistId,
			Long userId,
			String name,
			String description,
			String coverUrl,
			LocalDateTime createdAt,
			LocalDateTime updatedAt,
			Integer trackCount,
			List<PlaylistTrackResponse> tracks) {

		static PlaylistResponse of(Playlist p, List<PlaylistTrackResponse> tracks) {
			return new PlaylistResponse(
					p.getId(),
					p.getUserId(),
					p.getName(),
					p.getDescription(),
					p.getCoverUrl(),
					p.getCreatedAt(),
					p.getUpdatedAt(),
					tracks == null ? 0 : tracks.size(),
					tracks == null ? List.of() : tracks);
		}
	}

	public static record MusicPreferenceRequest(String musicId, Integer preferenceType) {
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

	public static record BatchReplaceMusicTagsItem(
			Long musicId,
			List<Long> tagIds,
			String source) {
	}

	public static record BatchReplaceMusicTagsRequest(
			List<BatchReplaceMusicTagsItem> items) {
	}

	public static record BatchReplaceMusicTagsResultItem(
			Long musicId,
			int mappingCount) {
	}

	public static record BatchReplaceMusicTagsResponse(
			List<BatchReplaceMusicTagsResultItem> results) {
	}

	public static record EmotionTagWriteRequest(
			String tagName) {
	}

	/**
	 * @param persist 是否写入数据库，默认 true
	 * @param replaceAiOnly 为 true 时仅替换 source=ai 的映射并保留人工标签；为 false 时清空全部再写入 AI 标签
	 * @param maxTags 最多推断几种情绪，默认 3
	 */
	public static record AiTaggingRequest(
			Boolean persist,
			Boolean replaceAiOnly,
			Integer maxTags) {
	}

	public static record AiTaggingResponse(
			Long musicId,
			String caption,
			List<String> inferredEmotionKeys,
			List<Long> tagIds,
			List<String> tagNames,
			List<Map<String, Object>> mappings,
			boolean persisted) {
  }
  
	public static record OfficialPlaylistConfigWriteRequest(
			String name,
			String description,
			String tagName,
			String coverEmotion,
			List<String> emotions,
			List<String> keywords,
			Integer sortOrder) {
	}

	public static record OfficialPlaylistConfigResponse(
			String playlistKey,
			String name,
			String description,
			String tagName,
			String coverEmotion,
			List<String> emotions,
			List<String> keywords,
			Integer sortOrder) {
	}
}
