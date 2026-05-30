package com.donffroodus.social_service.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.donffroodus.social_service.config.AiServiceProperties;
import com.donffroodus.social_service.entity.FriendRequest;
import com.donffroodus.social_service.entity.Friendship;
import com.donffroodus.social_service.entity.MoodDiary;
import com.donffroodus.social_service.entity.SocialInteraction;
import com.donffroodus.social_service.entity.SocialPost;
import com.donffroodus.social_service.repository.FriendRequestRepository;
import com.donffroodus.social_service.repository.FriendshipRepository;
import com.donffroodus.social_service.repository.MoodDiaryRepository;
import com.donffroodus.social_service.repository.SocialInteractionRepository;
import com.donffroodus.social_service.repository.SocialPostRepository;
import com.donffroodus.social_service.service.CensorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 社交服务 HTTP API：情绪日记、动态、点赞与评论。
 */
@Tag(name = "社交服务", description = "情绪日记、动态、点赞与评论。经网关访问时路径前缀为 /api/social")
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SocialApiController {
	@Autowired
	private CensorService censorService;

	private static final int MAX_PAGE_SIZE = 100;
	private static final int FRIENDSHIP_FLOWER_STEP = 50;
	private static final int MAX_FRIENDSHIP_FLOWERS = 3;
	private static final int POST_LIKE_FRIENDSHIP_POINTS = 3;
	private static final int POST_COMMENT_FRIENDSHIP_POINTS = 5;
	private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final MoodDiaryRepository moodDiaryRepository;
	private final SocialPostRepository socialPostRepository;
	private final SocialInteractionRepository socialInteractionRepository;
	private final FriendshipRepository friendshipRepository;
	private final FriendRequestRepository friendRequestRepository;
	private final RestTemplate restTemplate;
	private final AiServiceProperties aiServiceProperties;

	public SocialApiController(
			MoodDiaryRepository moodDiaryRepository,
			SocialPostRepository socialPostRepository,
			SocialInteractionRepository socialInteractionRepository,
			FriendshipRepository friendshipRepository,
			FriendRequestRepository friendRequestRepository,
			RestTemplate restTemplate,
			AiServiceProperties aiServiceProperties) {
		this.moodDiaryRepository = moodDiaryRepository;
		this.socialPostRepository = socialPostRepository;
		this.socialInteractionRepository = socialInteractionRepository;
		this.friendshipRepository = friendshipRepository;
		this.friendRequestRepository = friendRequestRepository;
		this.restTemplate = restTemplate;
		this.aiServiceProperties = aiServiceProperties;
	}

	private static List<String> parseMoodTags(String moodTag) {
		if (moodTag == null || moodTag.isBlank()) {
			return List.of();
		}
		return Arrays.stream(moodTag.split("[,，、\\s]+"))
				.map(String::trim)
				.filter(tag -> !tag.isEmpty())
				.collect(java.util.stream.Collectors.collectingAndThen(
						java.util.stream.Collectors.toCollection(LinkedHashSet::new),
						List::copyOf));
	}

	private static String normalizeMoodTag(String moodTag) {
		List<String> tags = parseMoodTags(moodTag);
		return tags.isEmpty() ? null : String.join(",", tags);
	}

	private HttpHeaders buildAuthorizationHeaders(String authorization) {
		HttpHeaders headers = new HttpHeaders();
		if (authorization != null && !authorization.isBlank()) {
			headers.set(HttpHeaders.AUTHORIZATION, authorization);
		}
		return headers;
	}

	private boolean areFriends(Long userId, Long otherUserId) {
		if (userId == null || otherUserId == null) {
			return false;
		}
		return friendshipRepository.existsByUserIdAndFriendUserId(userId, otherUserId);
	}

	private boolean canViewPost(Long viewerId, SocialPost post) {
		if (viewerId == null || post == null || post.getUserId() == null) {
			return false;
		}
		return viewerId.equals(post.getUserId()) || areFriends(viewerId, post.getUserId());
	}

	private List<Long> resolveVisibleAuthorIds(Long viewerId) {
		if (viewerId == null) {
			return List.of();
		}
		List<Long> friendIds = friendshipRepository.findByUserIdOrderByCreatedAtDesc(viewerId)
				.stream()
				.map(Friendship::getFriendUserId)
					.filter(java.util.Objects::nonNull)
				.toList();
		LinkedHashSet<Long> visibleIds = new LinkedHashSet<>();
		visibleIds.add(viewerId);
		visibleIds.addAll(friendIds);
		return List.copyOf(visibleIds);
	}

	private boolean canInteractWithComment(Long userId, SocialPost post, SocialInteraction targetComment) {
		if (userId == null || post == null || targetComment == null) {
			return false;
		}
		if (!canViewPost(userId, post)) {
			return false;
		}
		if (userId.equals(post.getUserId()) || userId.equals(targetComment.getUserId())) {
			return true;
		}
		return areFriends(userId, post.getUserId()) || areFriends(userId, targetComment.getUserId());
	}

	private static String resolveNotificationType(SocialInteraction interaction) {
		if (interaction.isLiked()) {
			return interaction.getTargetInteractionId() == null ? "POST_LIKE" : "COMMENT_LIKE";
		}
		return interaction.getTargetInteractionId() == null ? "POST_COMMENT" : "COMMENT_REPLY";
	}

	private static int sanitizeFriendshipScore(Integer score) {
		return score == null ? 0 : Math.max(score, 0);
	}

	private static int resolveFriendshipFlowers(Integer score) {
		return Math.min(sanitizeFriendshipScore(score) / FRIENDSHIP_FLOWER_STEP, MAX_FRIENDSHIP_FLOWERS);
	}

	private static String serializeDateTime(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.format(ISO_LOCAL_DATE_TIME_FORMATTER);
	}

	private void increaseFriendshipScore(Long userId, Long friendUserId, int delta) {
		if (delta <= 0 || userId == null || friendUserId == null || userId.equals(friendUserId)) {
			return;
		}

		friendshipRepository.findByUserIdAndFriendUserId(userId, friendUserId)
				.ifPresent(friendship -> {
					friendship.setIntimacyLevel(sanitizeFriendshipScore(friendship.getIntimacyLevel()) + delta);
					friendshipRepository.save(friendship);
				});

		friendshipRepository.findByUserIdAndFriendUserId(friendUserId, userId)
				.ifPresent(friendship -> {
					friendship.setIntimacyLevel(sanitizeFriendshipScore(friendship.getIntimacyLevel()) + delta);
					friendshipRepository.save(friendship);
				});
	}

	private FriendshipResponse buildFriendshipResponse(Friendship friendship) {
		int friendshipScore = sanitizeFriendshipScore(friendship.getIntimacyLevel());
		return FriendshipResponse.from(friendship, friendshipScore, resolveFriendshipFlowers(friendshipScore));
	}

	// --- mood_diary ---

	/** 列出当前用户的情绪日记；可选 date 按日期筛选。 */
	@Operation(summary = "列出当前用户的情绪日记", description = "可选 query 参数 date=yyyy-MM-dd 按日期筛选")
	@GetMapping("/me/mood-diaries")
	public List<MoodDiary> listUserMoodDiaries(
			@Parameter(name = "X-User-Id", description = "用户 ID（网关从 JWT 注入）", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestParam(value = "date", required = false) LocalDate date) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (date != null) {
			return moodDiaryRepository.findByUserIdAndDate(userId, date);
		}
		return moodDiaryRepository.findByUserIdOrderByDateDescCreatedAtDesc(userId);
	}

	/** 按主键获取当前用户的一条情绪日记。 */
	@Operation(summary = "获取一条情绪日记")
	@GetMapping("/me/mood-diaries/{diaryId}")
	public ResponseEntity<MoodDiary> getMoodDiary(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/** 新建一条情绪日记。 */
	@Operation(summary = "新建情绪日记")
	@PostMapping("/me/mood-diaries")
	public ResponseEntity<MoodDiary> createMoodDiary(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestBody MoodDiaryWriteRequest request) {
		if (request == null || request.date() == null) {
			return ResponseEntity.<MoodDiary>badRequest().build();
		}

		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		LocalDate today = LocalDate.now();
		if (!today.equals(request.date())) {
			return ResponseEntity.<MoodDiary>status(403).build();
		}
		if (moodDiaryRepository.existsByUserIdAndDate(userId, request.date())) {
			return ResponseEntity.<MoodDiary>status(409).build();
		}
		MoodDiary diary = new MoodDiary();
		diary.setUserId(userId);
		diary.setDate(request.date());
		diary.setDominantEmotion(request.dominantEmotion());
		diary.setContext(request.context());
		try {
			return ResponseEntity.ok(moodDiaryRepository.save(diary));
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			return ResponseEntity.<MoodDiary>status(409).build();
		}
	}

	/** 更新当前用户已有的一条情绪日记。 */
	@Operation(summary = "更新情绪日记")
	@PutMapping("/me/mood-diaries/{diaryId}")
	public ResponseEntity<MoodDiary> updateMoodDiary(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId,
			@RequestBody MoodDiaryWriteRequest request) {
		if (request == null) {
			return ResponseEntity.<MoodDiary>badRequest().build();
		}

		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		LocalDate today = LocalDate.now();

		MoodDiary existing = moodDiaryRepository.findByIdAndUserId(diaryId, userId).orElse(null);
		if (existing == null) {
			return ResponseEntity.<MoodDiary>notFound().build();
		}
		if (!today.equals(existing.getDate())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).<MoodDiary>build();
		}
		if (request.date() != null && !request.date().equals(existing.getDate())) {
			return ResponseEntity.badRequest().<MoodDiary>build();
		}
		existing.setDominantEmotion(request.dominantEmotion());
		existing.setContext(request.context());
		return ResponseEntity.ok(moodDiaryRepository.save(existing));
	}

	/** 删除当前用户的一条情绪日记。 */
	@Operation(summary = "删除情绪日记")
	@DeleteMapping("/me/mood-diaries/{diaryId}")
	@Transactional
	public ResponseEntity<Void> deleteMoodDiary(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		try {
			long deleted = moodDiaryRepository.deleteByIdAndUserId(diaryId, userId);
			if (deleted <= 0) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.noContent().build();
		} catch (DataAccessException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete mood diary");
		}
	}

	// --- social_post（广场与「我的」）---

	/** 动态广场：按时间倒序分页；匿名帖对非作者隐藏 authorUserId。 */
	@Operation(summary = "动态广场分页", description = "匿名帖对非作者隐藏 authorUserId；X-User-Id 可选")
	@GetMapping("/posts")
	public Page<SocialPostResponse> listPostFeed(
			@Parameter(name = "X-User-Id", description = "可选，用于判断匿名帖是否对当前用户展示作者", in = ParameterIn.HEADER) @RequestHeader(value = "X-User-Id", required = false) String xUserId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Long viewerId = GatewayAuthSupport.requireUserId(xUserId);
		Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt"));
		List<Long> visibleAuthorIds = resolveVisibleAuthorIds(viewerId);
		return socialPostRepository.findByUserIdInOrderByCreatedAtDesc(visibleAuthorIds, p)
				.map(post -> SocialPostResponse.of(post, viewerId));
	}

	/** 查看单条动态（匿名规则与广场一致）。 */
	@Operation(summary = "查看单条动态")
	@GetMapping("/posts/{postId}")
	public ResponseEntity<SocialPostResponse> getPost(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER) @RequestHeader(value = "X-User-Id", required = false) String xUserId,
			@PathVariable("postId") Long postId) {
		Long viewerId = GatewayAuthSupport.requireUserId(xUserId);
		return socialPostRepository.findById(postId)
				.map(post -> {
					if (!canViewPost(viewerId, post)) {
						return ResponseEntity.status(HttpStatus.FORBIDDEN).<SocialPostResponse>build();
					}
					return ResponseEntity.ok(SocialPostResponse.of(post, viewerId));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 当前用户发布的动态列表。 */
	@Operation(summary = "我的动态列表")
	@GetMapping("/me/posts")
	public Page<SocialPostResponse> listMyPosts(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt"));
		return socialPostRepository.findByUserIdOrderByCreatedAtDesc(userId, p)
				.map(post -> SocialPostResponse.of(post, userId));
	}

	/** 发布动态。 */
	@Operation(summary = "发布动态")
	@PostMapping("/me/posts")
	public ResponseEntity<SocialPostResponse> createPost(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestBody SocialPostWriteRequest request) {
		if (request == null || request.content() == null || request.content().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		SocialPost post = new SocialPost();
		post.setUserId(userId);
		post.setContent(censorService.CensorContent(request.content().strip()));
		post.setMoodTag(normalizeMoodTag(request.moodTag()));
		post.setAnonymous(request.isAnonymous() != null && request.isAnonymous());
		SocialPost saved = socialPostRepository.save(post);
		return ResponseEntity.ok(SocialPostResponse.of(saved, userId));
	}

	/** 更新自己的动态。 */
	@Operation(summary = "更新自己的动态")
	@PutMapping("/me/posts/{postId}")
	public ResponseEntity<SocialPostResponse> updatePost(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId,
			@RequestBody SocialPostWriteRequest request) {
		if (request == null || request.content() == null || request.content().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return socialPostRepository.findByIdAndUserId(postId, userId)
				.map(existing -> {
					existing.setContent(censorService.CensorContent(request.content().strip()));
					existing.setMoodTag(normalizeMoodTag(request.moodTag()));
					if (request.isAnonymous() != null) {
						existing.setAnonymous(request.isAnonymous());
					}
					SocialPost saved = socialPostRepository.save(existing);
					return ResponseEntity.ok(SocialPostResponse.of(saved, userId));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 删除自己的动态（同时删除其下全部互动记录）。 */
	@Operation(summary = "删除动态", description = "同时删除该帖下全部互动记录")
	@DeleteMapping("/me/posts/{postId}")
	@Transactional
	public ResponseEntity<Void> deletePost(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return socialPostRepository.findByIdAndUserId(postId, userId)
				.map(post -> {
					socialInteractionRepository.deleteByPostId(post.getId());
					socialPostRepository.delete(post);
					return ResponseEntity.ok().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	// --- social_interaction ---

	/** 某条动态下的全部互动（点赞行 + 评论行），按时间正序。 */
	@Operation(summary = "某条动态的互动列表", description = "含点赞行与评论行，按创建时间正序")
	@GetMapping("/posts/{postId}/interactions")
	public ResponseEntity<List<SocialInteractionResponse>> listInteractions(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return socialPostRepository.findById(postId)
				.map(post -> {
					if (!canViewPost(userId, post)) {
						return ResponseEntity.status(HttpStatus.FORBIDDEN).<List<SocialInteractionResponse>>build();
					}
					List<SocialInteractionResponse> list = socialInteractionRepository.findByPostIdOrderByCreatedAtAsc(postId)
							.stream()
							.map(SocialInteractionResponse::from)
							.toList();
					return ResponseEntity.ok(list);
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 切换点赞：已点赞则取消，未点赞则点赞（每人至多一条点赞记录，comment 为空）。 */
	@Operation(summary = "切换点赞", description = "每人至多一条点赞记录（comment 为空）；再点一次为取消赞")
	@PostMapping("/posts/{postId}/like")
	@Transactional
	public ResponseEntity<LikeToggleResponse> toggleLike(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return socialPostRepository.findById(postId)
				.map(post -> {
					if (!canViewPost(userId, post)) {
						return ResponseEntity.status(HttpStatus.FORBIDDEN).<LikeToggleResponse>build();
					}
					return socialInteractionRepository.findByPostIdAndUserIdAndLikedTrueAndCommentIsNullAndTargetInteractionIdIsNull(postId, userId)
							.map(likeRow -> {
								socialInteractionRepository.delete(likeRow);
								return ResponseEntity.ok(new LikeToggleResponse(false));
							})
							.orElseGet(() -> {
								SocialInteraction row = new SocialInteraction();
								row.setPostId(postId);
								row.setUserId(userId);
								row.setLiked(true);
								row.setComment(null);
								row.setTargetInteractionId(null);
								row.setTargetUserId(post.getUserId());
								socialInteractionRepository.save(row);
								increaseFriendshipScore(userId, post.getUserId(), POST_LIKE_FRIENDSHIP_POINTS);
								return ResponseEntity.ok(new LikeToggleResponse(true));
							});
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 发表评论。 */
	@Operation(summary = "发表评论")
	@PostMapping("/posts/{postId}/comments")
	@Transactional
	public ResponseEntity<SocialInteractionResponse> addComment(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId,
			@RequestBody CommentWriteRequest request) {
		if (request == null || request.comment() == null || request.comment().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return socialPostRepository.findById(postId)
				.map(post -> {
					if (!canViewPost(userId, post)) {
						return ResponseEntity.status(HttpStatus.FORBIDDEN).<SocialInteractionResponse>build();
					}
					SocialInteraction row = new SocialInteraction();
					row.setPostId(postId);
					row.setUserId(userId);
					row.setLiked(false);
					row.setComment(censorService.CensorContent(request.comment().strip()));
					row.setTargetInteractionId(null);
					row.setTargetUserId(post.getUserId());
					SocialInteraction saved = socialInteractionRepository.save(row);
					increaseFriendshipScore(userId, post.getUserId(), POST_COMMENT_FRIENDSHIP_POINTS);
					return ResponseEntity.ok(SocialInteractionResponse.from(saved));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/posts/ai-response/{postId}")
	public ResponseEntity<?> getAiResponseForPost(@PathVariable("postId") Long postId, @RequestHeader(value = "Authorization", required = false) String authorization) {
		String postContent = socialPostRepository.findById(postId)
				.map(SocialPost::getContent)
				.orElse(null);
		if (postContent == null) {
			return ResponseEntity.notFound().build();
		}
		Map<String, String> requestBody = Map.of("content", postContent);
		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, buildAuthorizationHeaders(authorization));
		ResponseEntity<Object> aiResponse = restTemplate.postForEntity(
				aiServiceProperties.postsResponseUrl(),
				requestEntity,
				Object.class);
		return ResponseEntity.status(aiResponse.getStatusCode()).body(aiResponse.getBody());
	}
	

	@Operation(summary = "切换评论点赞", description = "对指定评论点赞或取消点赞")
	@PostMapping("/posts/{postId}/comments/{commentId}/like")
	@Transactional
	public ResponseEntity<LikeToggleResponse> toggleCommentLike(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId,
			@PathVariable("commentId") Long commentId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		SocialPost post = socialPostRepository.findById(postId).orElse(null);
		SocialInteraction targetComment = socialInteractionRepository.findById(commentId).orElse(null);
		if (post == null || targetComment == null || targetComment.getComment() == null || !postId.equals(targetComment.getPostId())) {
			return ResponseEntity.notFound().build();
		}
		if (!canInteractWithComment(userId, post, targetComment)) {
			return ResponseEntity.status(403).build();
		}
		return socialInteractionRepository.findByPostIdAndUserIdAndLikedTrueAndCommentIsNullAndTargetInteractionId(postId, userId, commentId)
				.map(likeRow -> {
					socialInteractionRepository.delete(likeRow);
					return ResponseEntity.ok(new LikeToggleResponse(false));
				})
				.orElseGet(() -> {
					SocialInteraction row = new SocialInteraction();
					row.setPostId(postId);
					row.setUserId(userId);
					row.setLiked(true);
					row.setComment(null);
					row.setTargetInteractionId(commentId);
					row.setTargetUserId(targetComment.getUserId());
					socialInteractionRepository.save(row);
					return ResponseEntity.ok(new LikeToggleResponse(true));
				});
	}

	@Operation(summary = "回复评论")
	@PostMapping("/posts/{postId}/comments/{commentId}/replies")
	public ResponseEntity<SocialInteractionResponse> replyComment(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId,
			@PathVariable("commentId") Long commentId,
			@RequestBody CommentWriteRequest request) {
		if (request == null || request.comment() == null || request.comment().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		SocialPost post = socialPostRepository.findById(postId).orElse(null);
		SocialInteraction targetComment = socialInteractionRepository.findById(commentId).orElse(null);
		if (post == null || targetComment == null || targetComment.getComment() == null || !postId.equals(targetComment.getPostId())) {
			return ResponseEntity.notFound().build();
		}
		if (!canInteractWithComment(userId, post, targetComment)) {
			return ResponseEntity.status(403).build();
		}
		SocialInteraction row = new SocialInteraction();
		row.setPostId(postId);
		row.setUserId(userId);
		row.setLiked(false);
		row.setComment(censorService.CensorContent(request.comment().strip()));
		row.setTargetInteractionId(commentId);
		row.setTargetUserId(targetComment.getUserId());
		SocialInteraction saved = socialInteractionRepository.save(row);
		return ResponseEntity.ok(SocialInteractionResponse.from(saved));
	}

	@Operation(summary = "我的社交提醒", description = "返回我收到的帖子点赞、帖子评论、评论点赞和评论回复")
	@GetMapping("/me/social-notifications")
	public List<SocialNotificationResponse> listMySocialNotifications(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return socialInteractionRepository.findByTargetUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.filter(interaction -> interaction.getUserId() != null && !interaction.getUserId().equals(userId))
				.map(SocialNotificationResponse::from)
				.toList();
	}

	/** 删除自己的一条互动（取消点赞或删除自己的评论）。 */
	@Operation(summary = "删除自己的互动", description = "删除自己的点赞行或评论行")
	@DeleteMapping("/me/interactions/{interactionId}")
	public ResponseEntity<Void> deleteMyInteraction(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("interactionId") Long interactionId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return socialInteractionRepository.findByIdAndUserId(interactionId, userId)
				.map(row -> {
					socialInteractionRepository.delete(row);
					return ResponseEntity.ok().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	// --- friendship ---

	private static boolean isValidIntimacyLevel(Integer level) {
		return level != null && level >= 1 && level <= MAX_FRIENDSHIP_FLOWERS;
	}

	/** 发送好友申请 */
	@Operation(summary = "发送好友申请")
	@PostMapping("/me/friend-requests")
	public ResponseEntity<?> sendFriendRequest(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestBody FriendshipCreateRequest request) {
		if (request == null || request.friendUserId() == null) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		Long targetId = request.friendUserId();

		if (userId.equals(targetId)) {
			return ResponseEntity.badRequest().body("不能添加自己为好友");
		}
		
		if (friendshipRepository.existsByUserIdAndFriendUserId(userId, targetId)) {
			return ResponseEntity.status(409).body("已经是好友了");
		}
		
		if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(userId, targetId, "PENDING")) {
			return ResponseEntity.status(409).body("已发送过好友申请，请等待对方处理");
		}

		FriendRequest friendRequest = new FriendRequest();
		friendRequest.setSenderId(userId);
		friendRequest.setReceiverId(targetId);
		friendRequest.setStatus("PENDING");
		friendRequestRepository.save(friendRequest);
		
		return ResponseEntity.ok("好友申请发送成功");
	}

	/** 获取收到的好友申请列表 */
	@Operation(summary = "获取收到的好友申请")
	@GetMapping("/me/friend-requests/received")
	public ResponseEntity<List<FriendRequestResponse>> getReceivedFriendRequests(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		List<FriendRequestResponse> list = friendRequestRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(userId, "PENDING")
				.stream()
				.map(FriendRequestResponse::from)
				.toList();
		return ResponseEntity.ok(list);
	}

	/** 处理好友申请（同意或拒绝） */
	@Operation(summary = "处理好友申请")
	@PutMapping("/me/friend-requests/{requestId}")
	@Transactional
	public ResponseEntity<?> handleFriendRequest(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("requestId") Long requestId,
			@RequestBody FriendRequestHandleRequest request) {
		
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		
		return friendRequestRepository.findByIdAndReceiverId(requestId, userId)
				.map(friendReq -> {
					if (!"PENDING".equals(friendReq.getStatus())) {
						return ResponseEntity.badRequest().body("该申请已被处理");
					}
					
					if (request.accept()) {
						friendReq.setStatus("ACCEPTED");
						
						// Create bilateral friendship
						Integer intimacyLevel = 0;
						if (!friendshipRepository.existsByUserIdAndFriendUserId(userId, friendReq.getSenderId())) {
							Friendship selfSide = new Friendship();
							selfSide.setUserId(userId);
							selfSide.setFriendUserId(friendReq.getSenderId());
							selfSide.setIntimacyLevel(intimacyLevel);
							friendshipRepository.save(selfSide);
						}
						
						if (!friendshipRepository.existsByUserIdAndFriendUserId(friendReq.getSenderId(), userId)) {
							Friendship peerSide = new Friendship();
							peerSide.setUserId(friendReq.getSenderId());
							peerSide.setFriendUserId(userId);
							peerSide.setIntimacyLevel(intimacyLevel);
							friendshipRepository.save(peerSide);
						}
					} else {
						friendReq.setStatus("REJECTED");
					}
					
					friendRequestRepository.save(friendReq);
					return ResponseEntity.ok("处理成功");
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 获取当前用户的好友列表 */
	@Operation(summary = "我的好友列表")
	@GetMapping("/me/friends")
	public List<FriendshipResponse> listMyFriends(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return friendshipRepository.findByUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(this::buildFriendshipResponse)
				.toList();
	}

	/** 更新好友亲密度（1~3）。 */
	@Operation(summary = "更新好友亲密度", description = "亲密度仅允许 1~3")
	@PutMapping("/me/friends/{friendshipId}/intimacy")
	public ResponseEntity<FriendshipResponse> updateFriendIntimacy(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("friendshipId") Long friendshipId,
			@RequestBody FriendshipIntimacyUpdateRequest request) {
		if (request == null || !isValidIntimacyLevel(request.intimacyLevel())) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return friendshipRepository.findByIdAndUserId(friendshipId, userId)
				.map(existing -> {
					existing.setIntimacyLevel(request.intimacyLevel() * FRIENDSHIP_FLOWER_STEP);
					return ResponseEntity.ok(buildFriendshipResponse(friendshipRepository.save(existing)));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 删除好友关系（双向删除）。 */
	@Operation(summary = "删除好友")
	@DeleteMapping("/me/friends/{friendshipId}")
	@Transactional
	public ResponseEntity<Void> deleteFriend(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("friendshipId") Long friendshipId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return friendshipRepository.findByIdAndUserId(friendshipId, userId)
				.map(existing -> {
					friendshipRepository.deleteByUserIdAndFriendUserId(existing.getFriendUserId(), existing.getUserId());
					friendshipRepository.delete(existing);
					return ResponseEntity.ok().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	public static record MoodDiaryWriteRequest(
			LocalDate date,
			String dominantEmotion,
			String context) {
	}

	public static record SocialPostWriteRequest(
			String content,
			String moodTag,
			Boolean isAnonymous) {
	}

	public static record SocialPostResponse(
			Long postId,
			Long authorUserId,
			String content,
			String moodTag,
			List<String> highlightTags,
			boolean anonymous,
			String createdAt) {

		static SocialPostResponse of(SocialPost post, Long viewerUserId) {
			return new SocialPostResponse(
					post.getId(),
					post.getUserId(),
					post.getContent(),
					post.getMoodTag(),
					parseMoodTags(post.getMoodTag()),
					post.isAnonymous(),
					serializeDateTime(post.getCreatedAt()));
		}
	}

	public static record SocialInteractionResponse(
			Long interactionId,
			Long postId,
			Long userId,
			boolean liked,
			String comment,
			Long targetInteractionId,
			Long targetUserId,
			String createdAt) {

		static SocialInteractionResponse from(SocialInteraction i) {
			return new SocialInteractionResponse(
					i.getId(),
					i.getPostId(),
					i.getUserId(),
					i.isLiked(),
					i.getComment(),
					i.getTargetInteractionId(),
					i.getTargetUserId(),
					serializeDateTime(i.getCreatedAt()));
		}
	}

	public static record SocialNotificationResponse(
			Long interactionId,
			Long postId,
			Long actorUserId,
			Long targetUserId,
			Long targetInteractionId,
			String type,
			String content,
			String createdAt) {

		static SocialNotificationResponse from(SocialInteraction i) {
			return new SocialNotificationResponse(
					i.getId(),
					i.getPostId(),
					i.getUserId(),
					i.getTargetUserId(),
					i.getTargetInteractionId(),
					resolveNotificationType(i),
					i.getComment(),
					serializeDateTime(i.getCreatedAt()));
		}
	}

	public static record LikeToggleResponse(boolean liked) {
	}

	public static record CommentWriteRequest(String comment) {
	}

	public static record FriendshipCreateRequest(
			Long friendUserId,
			Integer intimacyLevel) {
	}

	public static record FriendshipIntimacyUpdateRequest(
			Integer intimacyLevel) {
	}

	public static record FriendshipResponse(
			Long friendshipId,
			Long userId,
			Long friendUserId,
			Integer intimacyLevel,
			Integer friendshipScore,
			String createdAt,
			String updatedAt) {

		static FriendshipResponse from(Friendship f, Integer friendshipScore, Integer intimacyLevel) {
			return new FriendshipResponse(
					f.getId(),
					f.getUserId(),
					f.getFriendUserId(),
					intimacyLevel,
					friendshipScore,
					serializeDateTime(f.getCreatedAt()),
					serializeDateTime(f.getUpdatedAt()));
		}
	}

	public static record FriendRequestHandleRequest(boolean accept) {}

	public static record FriendRequestResponse(
			Long id,
			Long senderId,
			Long receiverId,
			String status,
			String createdAt) {
		static FriendRequestResponse from(FriendRequest r) {
			return new FriendRequestResponse(
					r.getId(),
					r.getSenderId(),
					r.getReceiverId(),
					r.getStatus(),
					serializeDateTime(r.getCreatedAt())
			);
		}
	}
}
