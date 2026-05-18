package com.donffroodus.social_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.donffroodus.social_service.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

	List<ChatMessage> findByConversationIdAndIdLessThanOrderByCreatedAtDesc(
			Long conversationId, Long beforeId, Pageable pageable);

	long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long senderId);

	@Modifying
	@Query("""
			UPDATE ChatMessage m
			SET m.readAt = :readAt
			WHERE m.conversationId = :conversationId
			  AND m.senderId <> :userId
			  AND m.readAt IS NULL
			""")
	int markAsReadForUser(
			@Param("conversationId") Long conversationId,
			@Param("userId") Long userId,
			@Param("readAt") LocalDateTime readAt);
}
