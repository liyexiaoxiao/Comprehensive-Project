package com.donffroodus.social_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_conversation")
public class ChatConversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "conversation_id")
	private Long id;

	@Column(name = "user_one_id", nullable = false)
	private Long userOneId;

	@Column(name = "user_two_id", nullable = false)
	private Long userTwoId;

	@Column(name = "last_message_at")
	private LocalDateTime lastMessageAt;

	@Column(name = "last_message_preview", length = 255)
	private String lastMessagePreview;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	public static long[] orderedPair(long userA, long userB) {
		if (userA < userB) {
			return new long[] { userA, userB };
		}
		return new long[] { userB, userA };
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserOneId() {
		return userOneId;
	}

	public void setUserOneId(Long userOneId) {
		this.userOneId = userOneId;
	}

	public Long getUserTwoId() {
		return userTwoId;
	}

	public void setUserTwoId(Long userTwoId) {
		this.userTwoId = userTwoId;
	}

	public LocalDateTime getLastMessageAt() {
		return lastMessageAt;
	}

	public void setLastMessageAt(LocalDateTime lastMessageAt) {
		this.lastMessageAt = lastMessageAt;
	}

	public String getLastMessagePreview() {
		return lastMessagePreview;
	}

	public void setLastMessagePreview(String lastMessagePreview) {
		this.lastMessagePreview = lastMessagePreview;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public long peerUserId(long userId) {
		return userId == userOneId ? userTwoId : userOneId;
	}
}
