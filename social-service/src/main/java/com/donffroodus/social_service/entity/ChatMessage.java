package com.donffroodus.social_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_message")
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "message_id")
	private Long id;

	@Column(name = "conversation_id", nullable = false)
	private Long conversationId;

	@Column(name = "sender_id", nullable = false)
	private Long senderId;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getConversationId() {
		return conversationId;
	}

	public void setConversationId(Long conversationId) {
		this.conversationId = conversationId;
	}

	public Long getSenderId() {
		return senderId;
	}

	public void setSenderId(Long senderId) {
		this.senderId = senderId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getReadAt() {
		return readAt;
	}

	public void setReadAt(LocalDateTime readAt) {
		this.readAt = readAt;
	}
}
