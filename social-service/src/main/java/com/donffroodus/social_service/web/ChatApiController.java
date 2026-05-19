package com.donffroodus.social_service.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.donffroodus.social_service.dto.ChatDto.ChatConversationResponse;
import com.donffroodus.social_service.dto.ChatDto.ChatMessageResponse;
import com.donffroodus.social_service.dto.ChatDto.MarkReadResponse;
import com.donffroodus.social_service.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "好友聊天", description = "好友一对一私信。经网关访问时路径前缀为 /api/social")
@RestController
@RequestMapping("/api/v1/me/chat")
@CrossOrigin(origins = "*")
public class ChatApiController {

	private final ChatService chatService;

	public ChatApiController(ChatService chatService) {
		this.chatService = chatService;
	}

	@Operation(summary = "会话列表", description = "返回当前用户参与的所有会话，按最后消息时间倒序")
	@GetMapping("/conversations")
	public List<ChatConversationResponse> listConversations(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return chatService.listConversations(userId);
	}

	@Operation(summary = "拉取与好友的历史消息", description = "beforeId 为游标，取更早的消息；不传则取最新一页。尚无会话时返回空列表")
	@GetMapping("/with/{peerUserId}/messages")
	public List<ChatMessageResponse> getMessages(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("peerUserId") Long peerUserId,
			@RequestParam(value = "beforeId", required = false) Long beforeId,
			@RequestParam(value = "limit", required = false) Integer limit) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return chatService.getMessages(userId, peerUserId, beforeId, limit);
	}

	@Operation(summary = "发送消息", description = "仅好友可发送；自动创建会话")
	@PostMapping("/with/{peerUserId}/messages")
	public ResponseEntity<ChatMessageResponse> sendMessage(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("peerUserId") Long peerUserId,
			@RequestBody SendMessageRequest request) {
		if (request == null || request.content() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
		}
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		return ResponseEntity.ok(chatService.sendMessage(userId, peerUserId, request.content()));
	}

	@Operation(summary = "标记与好友的会话为已读")
	@PutMapping("/with/{peerUserId}/read")
	public MarkReadResponse markAsRead(
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true) @RequestHeader("X-User-Id") String xUserId,
			@PathVariable("peerUserId") Long peerUserId) {
		Long userId = GatewayAuthSupport.requireUserId(xUserId);
		int updated = chatService.markAsRead(userId, peerUserId);
		return new MarkReadResponse(updated);
	}

	public static record SendMessageRequest(String content) {
	}
}
