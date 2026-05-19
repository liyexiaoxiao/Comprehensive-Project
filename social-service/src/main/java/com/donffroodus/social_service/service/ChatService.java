package com.donffroodus.social_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.donffroodus.social_service.dto.ChatDto.ChatConversationResponse;
import com.donffroodus.social_service.dto.ChatDto.ChatMessageResponse;
import com.donffroodus.social_service.entity.ChatConversation;
import com.donffroodus.social_service.entity.ChatMessage;
import com.donffroodus.social_service.repository.ChatConversationRepository;
import com.donffroodus.social_service.repository.ChatMessageRepository;
import com.donffroodus.social_service.repository.FriendshipRepository;

@Service
public class ChatService {

	private static final int MAX_CONTENT_LENGTH = 2000;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int DEFAULT_PAGE_SIZE = 30;
	private static final int PREVIEW_MAX_LENGTH = 255;

	private final ChatConversationRepository conversationRepository;
	private final ChatMessageRepository messageRepository;
	private final FriendshipRepository friendshipRepository;

	public ChatService(
			ChatConversationRepository conversationRepository,
			ChatMessageRepository messageRepository,
			FriendshipRepository friendshipRepository) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.friendshipRepository = friendshipRepository;
	}

	public List<ChatConversationResponse> listConversations(Long userId) {
		return conversationRepository.findAllForUser(userId).stream()
				.map(conv -> toConversationResponse(conv, userId))
				.toList();
	}

	public List<ChatMessageResponse> getMessages(Long userId, Long peerUserId, Long beforeId, Integer limit) {
		requireFriend(userId, peerUserId);
		long[] pair = ChatConversation.orderedPair(userId, peerUserId);
		var conversationOpt = conversationRepository.findByUserOneIdAndUserTwoId(pair[0], pair[1]);
		if (conversationOpt.isEmpty()) {
			return List.of();
		}
		ChatConversation conversation = conversationOpt.get();
		int pageSize = sanitizeLimit(limit);
		Pageable pageable = PageRequest.of(0, pageSize);
		List<ChatMessage> messages = beforeId == null
				? messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable)
				: messageRepository.findByConversationIdAndIdLessThanOrderByCreatedAtDesc(
						conversation.getId(), beforeId, pageable);
		return messages.stream()
				.map(ChatMessageResponse::from)
				.toList();
	}

	@Transactional
	public ChatMessageResponse sendMessage(Long userId, Long peerUserId, String content) {
		requireFriend(userId, peerUserId);
		String text = normalizeContent(content);
		ChatConversation conversation = getOrCreateConversation(userId, peerUserId);

		ChatMessage message = new ChatMessage();
		message.setConversationId(conversation.getId());
		message.setSenderId(userId);
		message.setContent(text);
		ChatMessage saved = messageRepository.save(message);

		conversation.setLastMessageAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now());
		conversation.setLastMessagePreview(truncatePreview(text));
		conversationRepository.save(conversation);

		return ChatMessageResponse.from(saved);
	}

	@Transactional
	public int markAsRead(Long userId, Long peerUserId) {
		requireFriend(userId, peerUserId);
		long[] pair = ChatConversation.orderedPair(userId, peerUserId);
		return conversationRepository.findByUserOneIdAndUserTwoId(pair[0], pair[1])
				.map(conv -> messageRepository.markAsReadForUser(conv.getId(), userId, LocalDateTime.now()))
				.orElse(0);
	}

	private void requireFriend(Long userId, Long peerUserId) {
		if (userId.equals(peerUserId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能与自己聊天");
		}
		if (!friendshipRepository.existsByUserIdAndFriendUserId(userId, peerUserId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "非好友，无法聊天");
		}
	}

	private ChatConversation getOrCreateConversation(Long userId, Long peerUserId) {
		long[] pair = ChatConversation.orderedPair(userId, peerUserId);
		return conversationRepository.findByUserOneIdAndUserTwoId(pair[0], pair[1])
				.orElseGet(() -> {
					ChatConversation created = new ChatConversation();
					created.setUserOneId(pair[0]);
					created.setUserTwoId(pair[1]);
					return conversationRepository.save(created);
				});
	}

	private ChatConversationResponse toConversationResponse(ChatConversation conv, Long userId) {
		long peerUserId = conv.peerUserId(userId);
		long unread = messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(conv.getId(), userId);
		return new ChatConversationResponse(
				conv.getId(),
				peerUserId,
				conv.getLastMessagePreview(),
				conv.getLastMessageAt(),
				unread);
	}

	private static String normalizeContent(String content) {
		if (content == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
		}
		String text = content.strip();
		if (text.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
		}
		if (text.length() > MAX_CONTENT_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容过长");
		}
		return text;
	}

	private static String truncatePreview(String text) {
		if (text.length() <= PREVIEW_MAX_LENGTH) {
			return text;
		}
		return text.substring(0, PREVIEW_MAX_LENGTH - 3) + "...";
	}

	private static int sanitizeLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(limit, MAX_PAGE_SIZE);
	}
}
