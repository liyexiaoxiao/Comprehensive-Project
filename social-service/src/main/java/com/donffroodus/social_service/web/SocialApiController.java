package com.donffroodus.social_service.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.donffroodus.social_service.entity.Friendship;
import com.donffroodus.social_service.entity.MoodDiary;
import com.donffroodus.social_service.entity.SocialInteraction;
import com.donffroodus.social_service.entity.SocialPost;
import com.donffroodus.social_service.repository.FriendshipRepository;
import com.donffroodus.social_service.repository.MoodDiaryRepository;
import com.donffroodus.social_service.repository.SocialInteractionRepository;
import com.donffroodus.social_service.repository.SocialPostRepository;

/**
 * 社交服务 HTTP API：情绪日记、动态、点赞与评论。
 */
@Tag(name = "社交服务", description = "情绪日记、动态、点赞与评论。经网关访问时路径前缀为 /api/social")
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SocialApiController {

	private static final int MAX_PAGE_SIZE = 100;

	private final MoodDiaryRepository moodDiaryRepository;
	private final SocialPostRepository socialPostRepository;
	private final SocialInteractionRepository socialInteractionRepository;
	private final FriendshipRepository friendshipRepository;

	public SocialApiController(
			MoodDiaryRepository moodDiaryRepository,
			SocialPostRepository socialPostRepository,
			SocialInteractionRepository socialInteractionRepository,
			FriendshipRepository friendshipRepository) {
		this.moodDiaryRepository = moodDiaryRepository;
		this.socialPostRepository = socialPostRepository;
		this.socialInteractionRepository = socialInteractionRepository;
		this.friendshipRepository = friendshipRepository;
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
			return ResponseEntity.badRequest().build();
		}

		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		MoodDiary diary = new MoodDiary();
		diary.setUserId(userId);
		diary.setDate(request.date());
		diary.setDominantEmotion(request.dominantEmotion());
		diary.setContext(request.context());
		return ResponseEntity.ok(moodDiaryRepository.save(diary));
	}

	/** 更新当前用户已有的一条情绪日记。 */
	@Operation(summary = "更新情绪日记")
	@PutMapping("/me/mood-diaries/{diaryId}")
	public ResponseEntity<MoodDiary> updateMoodDiary(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId,
			@RequestBody MoodDiaryWriteRequest request) {
		if (request == null || request.date() == null) {
			return ResponseEntity.badRequest().build();
		}

		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(existing -> {
					existing.setDate(request.date());
					existing.setDominantEmotion(request.dominantEmotion());
					existing.setContext(request.context());
					return ResponseEntity.ok(moodDiaryRepository.save(existing));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/** 删除当前用户的一条情绪日记。 */
	@Operation(summary = "删除情绪日记")
	@DeleteMapping("/me/mood-diaries/{diaryId}")
	public ResponseEntity<Void> deleteMoodDiary(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("diaryId") Long diaryId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return moodDiaryRepository.findByIdAndUserId(diaryId, userId)
				.map(existing -> {
					moodDiaryRepository.delete(existing);
					return ResponseEntity.ok().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	// --- social_post（广场与「我的」）---

	/** 动态广场：按时间倒序分页；匿名帖对非作者隐藏 authorUserId。 */
	@Operation(summary = "动态广场分页", description = "匿名帖对非作者隐藏 authorUserId；X-User-Id 可选")
	@GetMapping("/posts")
	public Page<SocialPostResponse> listPostFeed(
			@Parameter(name = "X-User-Id", description = "可选，用于判断匿名帖是否对当前用户展示作者", in = ParameterIn.HEADER) @RequestHeader(value = "X-User-Id", required = false) String xUserId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Long viewerId = GatewayAuthSupport.optionalUserId(xUserId);
		Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt"));
		return socialPostRepository.findAllByOrderByCreatedAtDesc(p)
				.map(post -> SocialPostResponse.of(post, viewerId));
	}

	/** 查看单条动态（匿名规则与广场一致）。 */
	@Operation(summary = "查看单条动态")
	@GetMapping("/posts/{postId}")
	public ResponseEntity<SocialPostResponse> getPost(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER) @RequestHeader(value = "X-User-Id", required = false) String xUserId,
			@PathVariable("postId") Long postId) {
		Long viewerId = GatewayAuthSupport.optionalUserId(xUserId);
		return socialPostRepository.findById(postId)
				.map(post -> ResponseEntity.ok(SocialPostResponse.of(post, viewerId)))
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
		post.setContent(request.content().strip());
		post.setMoodTag(request.moodTag());
		post.setAnonymous(request.isAnonymous() == null || request.isAnonymous());
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
					existing.setContent(request.content().strip());
					existing.setMoodTag(request.moodTag());
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
	public ResponseEntity<List<SocialInteractionResponse>> listInteractions(@PathVariable("postId") Long postId) {
		if (!socialPostRepository.existsById(postId)) {
			return ResponseEntity.notFound().build();
		}
		List<SocialInteractionResponse> list = socialInteractionRepository.findByPostIdOrderByCreatedAtAsc(postId)
				.stream()
				.map(SocialInteractionResponse::from)
				.toList();
		return ResponseEntity.ok(list);
	}

	/** 切换点赞：已点赞则取消，未点赞则点赞（每人至多一条点赞记录，comment 为空）。 */
	@Operation(summary = "切换点赞", description = "每人至多一条点赞记录（comment 为空）；再点一次为取消赞")
	@PostMapping("/posts/{postId}/like")
	@Transactional
	public ResponseEntity<LikeToggleResponse> toggleLike(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (!socialPostRepository.existsById(postId)) {
			return ResponseEntity.notFound().build();
		}
		return socialInteractionRepository.findByPostIdAndUserIdAndCommentIsNull(postId, userId)
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
					socialInteractionRepository.save(row);
					return ResponseEntity.ok(new LikeToggleResponse(true));
				});
	}

	/** 发表评论。 */
	@Operation(summary = "发表评论")
	@PostMapping("/posts/{postId}/comments")
	public ResponseEntity<SocialInteractionResponse> addComment(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("postId") Long postId,
			@RequestBody CommentWriteRequest request) {
		if (request == null || request.comment() == null || request.comment().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (!socialPostRepository.existsById(postId)) {
			return ResponseEntity.notFound().build();
		}
		SocialInteraction row = new SocialInteraction();
		row.setPostId(postId);
		row.setUserId(userId);
		row.setLiked(false);
		row.setComment(request.comment().strip());
		SocialInteraction saved = socialInteractionRepository.save(row);
		return ResponseEntity.ok(SocialInteractionResponse.from(saved));
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

	private static boolean isValidIntimacy(Integer level) {
		return level != null && level >= 1 && level <= 3;
	}

	/** 当前用户好友列表。 */
	@Operation(summary = "我的好友列表")
	@GetMapping("/me/friends")
	public List<FriendshipResponse> listMyFriends(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return friendshipRepository.findByUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(FriendshipResponse::from)
				.toList();
	}

	/** 新增好友（双向落地）。 */
	@Operation(summary = "新增好友", description = "双向落地：同时新增 A->B 与 B->A，两侧亲密度默认同值")
	@PostMapping("/me/friends")
	@Transactional
	public ResponseEntity<FriendshipResponse> addFriend(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestBody FriendshipCreateRequest request) {
		if (request == null || request.friendUserId() == null) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		if (request.friendUserId().equals(userId)) {
			return ResponseEntity.badRequest().build();
		}
		if (friendshipRepository.existsByUserIdAndFriendUserId(userId, request.friendUserId())) {
			return ResponseEntity.status(409).build();
		}
		Integer intimacyLevel = request.intimacyLevel() == null ? 1 : request.intimacyLevel();
		if (!isValidIntimacy(intimacyLevel)) {
			return ResponseEntity.badRequest().build();
		}
		Friendship selfSide = new Friendship();
		selfSide.setUserId(userId);
		selfSide.setFriendUserId(request.friendUserId());
		selfSide.setIntimacyLevel(intimacyLevel);
		Friendship savedSelfSide = friendshipRepository.save(selfSide);

		if (!friendshipRepository.existsByUserIdAndFriendUserId(request.friendUserId(), userId)) {
			Friendship peerSide = new Friendship();
			peerSide.setUserId(request.friendUserId());
			peerSide.setFriendUserId(userId);
			peerSide.setIntimacyLevel(intimacyLevel);
			friendshipRepository.save(peerSide);
		}
		return ResponseEntity.ok(FriendshipResponse.from(savedSelfSide));
	}

	/** 更新好友亲密度（1~3）。 */
	@Operation(summary = "更新好友亲密度", description = "亲密度仅允许 1~3")
	@PutMapping("/me/friends/{friendshipId}/intimacy")
	public ResponseEntity<FriendshipResponse> updateFriendIntimacy(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("friendshipId") Long friendshipId,
			@RequestBody FriendshipIntimacyUpdateRequest request) {
		if (request == null || !isValidIntimacy(request.intimacyLevel())) {
			return ResponseEntity.badRequest().build();
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return friendshipRepository.findByIdAndUserId(friendshipId, userId)
				.map(existing -> {
					existing.setIntimacyLevel(request.intimacyLevel());
					return ResponseEntity.ok(FriendshipResponse.from(friendshipRepository.save(existing)));
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
			boolean anonymous,
			LocalDateTime createdAt) {

		static SocialPostResponse of(SocialPost post, Long viewerUserId) {
			boolean hideAuthor = post.isAnonymous()
					&& (viewerUserId == null || !post.getUserId().equals(viewerUserId));
			return new SocialPostResponse(
					post.getId(),
					hideAuthor ? null : post.getUserId(),
					post.getContent(),
					post.getMoodTag(),
					post.isAnonymous(),
					post.getCreatedAt());
		}
	}

	public static record SocialInteractionResponse(
			Long interactionId,
			Long postId,
			Long userId,
			boolean liked,
			String comment,
			LocalDateTime createdAt) {

		static SocialInteractionResponse from(SocialInteraction i) {
			return new SocialInteractionResponse(
					i.getId(),
					i.getPostId(),
					i.getUserId(),
					i.isLiked(),
					i.getComment(),
					i.getCreatedAt());
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
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {

		static FriendshipResponse from(Friendship f) {
			return new FriendshipResponse(
					f.getId(),
					f.getUserId(),
					f.getFriendUserId(),
					f.getIntimacyLevel(),
					f.getCreatedAt(),
					f.getUpdatedAt());
		}
	}
}
