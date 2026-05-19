package com.donffroodus.music_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "official_playlist_config")
public class OfficialPlaylistConfig {

	@Id
	@Column(name = "playlist_key", nullable = false, length = 64)
	private String playlistKey;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "tag_name", nullable = false)
	private String tagName;

	@Column(name = "cover_emotion", nullable = false)
	private String coverEmotion;

	@Column(name = "emotion_keys", columnDefinition = "TEXT")
	private String emotionKeys;

	@Column(columnDefinition = "TEXT")
	private String keywords;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	public String getPlaylistKey() {
		return playlistKey;
	}

	public void setPlaylistKey(String playlistKey) {
		this.playlistKey = playlistKey;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getCoverEmotion() {
		return coverEmotion;
	}

	public void setCoverEmotion(String coverEmotion) {
		this.coverEmotion = coverEmotion;
	}

	public String getEmotionKeys() {
		return emotionKeys;
	}

	public void setEmotionKeys(String emotionKeys) {
		this.emotionKeys = emotionKeys;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
