package com.donffroodus.data_service.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.donffroodus.data_service.mongo.AiChatLog;
import com.donffroodus.data_service.mongo.EmotionSnapshot;
import com.donffroodus.data_service.mongo.UserBehaviorLog;
import com.donffroodus.data_service.repository.AiChatLogRepository;
import com.donffroodus.data_service.repository.EmotionSnapshotRepository;
import com.donffroodus.data_service.repository.UserBehaviorLogRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 数据服务 HTTP API：将 AI 对话、情绪快照、用户行为等事件写入 MongoDB，并支持按用户查询近期记录。
 * <p>Swagger 展示说明需使用 {@link Operation} 等注解。</p>
 */
@Tag(name = "数据服务", description = "MongoDB 日志写入与查询。经网关访问时路径前缀为 /api/data")
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class DataApiController {

	private final AiChatLogRepository aiChatLogRepository;
	private final EmotionSnapshotRepository emotionSnapshotRepository;
	private final UserBehaviorLogRepository userBehaviorLogRepository;

	public DataApiController(
			AiChatLogRepository aiChatLogRepository,
			EmotionSnapshotRepository emotionSnapshotRepository,
			UserBehaviorLogRepository userBehaviorLogRepository) {
		this.aiChatLogRepository = aiChatLogRepository;
		this.emotionSnapshotRepository = emotionSnapshotRepository;
		this.userBehaviorLogRepository = userBehaviorLogRepository;
	}

	/** 追加一条 AI 对话日志（需网关传入 X-User-Id）。 */
	@Operation(summary = "追加 AI 对话日志", description = "userId 取自 X-User-Id；写入 ai_chat_log 集合")
	@PostMapping("/logs/ai-chat")
	public ResponseEntity<AiChatLog> appendAiChatLog(
			@Parameter(name = "X-User-Id", description = "用户 ID（网关从 JWT 注入）", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@Valid @RequestBody AiChatLogWriteRequest body) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		AiChatLog doc = new AiChatLog();
		doc.setUserId(userId);
		doc.setSessionId(body.sessionId());
		doc.setRole(body.role());
		doc.setContent(body.content());
		doc.setEmotionLabel(body.emotionLabel());
		doc.setCreatedAt(Instant.now());
		return ResponseEntity.ok(aiChatLogRepository.save(doc));
	}

	/** 追加一条情绪快照（分析结果等）。 */
	@Operation(summary = "追加情绪快照", description = "写入 emotion_snapshot 集合；source 如 voice/text/multimodal")
	@PostMapping("/logs/emotion-snapshot")
	public ResponseEntity<EmotionSnapshot> appendEmotionSnapshot(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@Valid @RequestBody EmotionSnapshotWriteRequest body) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		EmotionSnapshot doc = new EmotionSnapshot();
		doc.setUserId(userId);
		doc.setSource(body.source());
		doc.setEmotion(body.emotion());
		doc.setScore(body.score());
		doc.setScores(body.scores());
		doc.setCreatedAt(Instant.now());
		return ResponseEntity.ok(emotionSnapshotRepository.save(doc));
	}

	/** 追加一条用户行为日志（播放、点击等业务动作）。 */
	@Operation(summary = "追加用户行为日志", description = "写入 user_behavior_log；actionType 如 play_music、publish_post 等")
	@PostMapping("/logs/user-behavior")
	public ResponseEntity<UserBehaviorLog> appendUserBehaviorLog(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@Valid @RequestBody UserBehaviorLogWriteRequest body) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		UserBehaviorLog doc = new UserBehaviorLog();
		doc.setUserId(userId);
		doc.setActionType(body.actionType());
		doc.setTargetType(body.targetType());
		doc.setTargetId(body.targetId());
		doc.setMetadata(body.metadata());
		doc.setCreatedAt(Instant.now());
		return ResponseEntity.ok(userBehaviorLogRepository.save(doc));
	}

	/** 查询当前用户的 AI 对话历史；可选 sessionId 按会话筛选。 */
	@Operation(summary = "查询 AI 对话历史", description = "可选 sessionId；size 默认 50，最大 200")
	@GetMapping("/me/ai-chat-logs")
	public List<AiChatLog> listAiChatLogs(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestParam(value = "sessionId", required = false) String sessionId,
			@RequestParam(value = "size", defaultValue = "50") int size) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		int pageSize = Math.min(Math.max(size, 1), 200);
		if (sessionId != null && !sessionId.isBlank()) {
			return aiChatLogRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId.strip());
		}
		return aiChatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, pageSize));
	}

	/** 查询当前用户近期的情绪快照列表。 */
	@Operation(summary = "查询情绪快照列表", description = "按创建时间倒序；size 默认 50，最大 200")
	@GetMapping("/me/emotion-snapshots")
	public List<EmotionSnapshot> listEmotionSnapshots(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestParam(value = "size", defaultValue = "50") int size) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		int pageSize = Math.min(Math.max(size, 1), 200);
		return emotionSnapshotRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, pageSize));
	}

	/** 查询当前用户近期的行为日志列表。 */
	@Operation(summary = "查询用户行为日志", description = "按创建时间倒序；size 默认 50，最大 200")
	@GetMapping("/me/user-behavior-logs")
	public List<UserBehaviorLog> listUserBehaviorLogs(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@RequestParam(value = "size", defaultValue = "50") int size) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		int pageSize = Math.min(Math.max(size, 1), 200);
		return userBehaviorLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, pageSize));
	}

	public record AiChatLogWriteRequest(
			@NotBlank String sessionId,
			@NotBlank String role,
			@NotBlank String content,
			String emotionLabel) {
	}

	public record EmotionSnapshotWriteRequest(
			@NotBlank String source,
			@NotBlank String emotion,
			@NotNull Double score,
			Map<String, Double> scores) {
	}

	public record UserBehaviorLogWriteRequest(
			@NotBlank String actionType,
			String targetType,
			Long targetId,
			Map<String, Object> metadata) {
	}
}
