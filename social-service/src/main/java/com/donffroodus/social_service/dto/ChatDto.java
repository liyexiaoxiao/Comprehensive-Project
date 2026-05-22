package com.donffroodus.social_service.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.donffroodus.social_service.entity.ChatConversation;
import com.donffroodus.social_service.entity.ChatMessage;

public final class ChatDto {

	private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private ChatDto() {
	}

	public static record ChatMessageResponse(
			Long messageId,
			Long conversationId,
			Long senderId,
			String content,
			String createdAt,
			String readAt) {

		public static ChatMessageResponse from(ChatMessage message) {
			return new ChatMessageResponse(
					message.getId(),
					message.getConversationId(),
					message.getSenderId(),
					message.getContent(),
					serializeDateTime(message.getCreatedAt()),
					serializeDateTime(message.getReadAt()));
		}
	}

	public static record ChatConversationResponse(
			Long conversationId,
			Long peerUserId,
			String lastMessagePreview,
			String lastMessageAt,
			long unreadCount) {
		public static ChatConversationResponse from(ChatConversation conversation, Long userId, long unreadCount) {
			return new ChatConversationResponse(
					conversation.getId(),
					conversation.peerUserId(userId),
					conversation.getLastMessagePreview(),
					serializeDateTime(conversation.getLastMessageAt()),
					unreadCount);
		}
	}

	public static record MarkReadResponse(int markedCount) {
	}

	private static String serializeDateTime(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.format(ISO_LOCAL_DATE_TIME_FORMATTER);
	}
}
