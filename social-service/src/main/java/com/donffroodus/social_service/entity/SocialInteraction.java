package com.donffroodus.social_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 一条记录表示一次互动：纯点赞（comment 为空且 {@code is_like} 为 true）或一条评论（comment 非空）。
 */
@Entity
@Table(name = "social_interaction")
public class SocialInteraction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "interaction_id")
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "is_like", nullable = false)
	private boolean liked;

	@Column(columnDefinition = "text")
	private String comment;

	@Column(name = "target_interaction_id")
	private Long targetInteractionId;

	@Column(name = "target_user_id")
	private Long targetUserId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPostId() {
		return postId;
	}

	public void setPostId(Long postId) {
		this.postId = postId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public boolean isLiked() {
		return liked;
	}

	public void setLiked(boolean liked) {
		this.liked = liked;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Long getTargetInteractionId() {
		return targetInteractionId;
	}

	public void setTargetInteractionId(Long targetInteractionId) {
		this.targetInteractionId = targetInteractionId;
	}

	public Long getTargetUserId() {
		return targetUserId;
	}

	public void setTargetUserId(Long targetUserId) {
		this.targetUserId = targetUserId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
