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
 */
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
	@PostMapping("/logs/ai-chat")
	public ResponseEntity<AiChatLog> appendAiChatLog(
			@RequestHeader("X-User-Id") String xUserId,
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
	@PostMapping("/logs/emotion-snapshot")
	public ResponseEntity<EmotionSnapshot> appendEmotionSnapshot(
			@RequestHeader("X-User-Id") String xUserId,
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
	@PostMapping("/logs/user-behavior")
	public ResponseEntity<UserBehaviorLog> appendUserBehaviorLog(
			@RequestHeader("X-User-Id") String xUserId,
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
	@GetMapping("/me/ai-chat-logs")
	public List<AiChatLog> listAiChatLogs(
			@RequestHeader("X-User-Id") String xUserId,
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
	@GetMapping("/me/emotion-snapshots")
	public List<EmotionSnapshot> listEmotionSnapshots(
			@RequestHeader("X-User-Id") String xUserId,
			@RequestParam(value = "size", defaultValue = "50") int size) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		int pageSize = Math.min(Math.max(size, 1), 200);
		return emotionSnapshotRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, pageSize));
	}

	/** 查询当前用户近期的行为日志列表。 */
	@GetMapping("/me/user-behavior-logs")
	public List<UserBehaviorLog> listUserBehaviorLogs(
			@RequestHeader("X-User-Id") String xUserId,
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
