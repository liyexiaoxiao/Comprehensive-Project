package com.donffroodus.social_service.dto;

import java.time.LocalDateTime;

import com.donffroodus.social_service.entity.ChatMessage;

public final class ChatDto {

	private ChatDto() {
	}

	public static record ChatMessageResponse(
			Long messageId,
			Long conversationId,
			Long senderId,
			String content,
			LocalDateTime createdAt,
			LocalDateTime readAt) {

		public static ChatMessageResponse from(ChatMessage message) {
			return new ChatMessageResponse(
					message.getId(),
					message.getConversationId(),
					message.getSenderId(),
					message.getContent(),
					message.getCreatedAt(),
					message.getReadAt());
		}
	}

	public static record ChatConversationResponse(
			Long conversationId,
			Long peerUserId,
			String lastMessagePreview,
			LocalDateTime lastMessageAt,
			long unreadCount) {
	}

	public static record MarkReadResponse(int markedCount) {
	}
}
