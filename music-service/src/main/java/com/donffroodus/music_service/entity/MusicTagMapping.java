package com.donffroodus.music_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "music_tag_mapping")
public class MusicTagMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mapping_id")
	private Long id;

	@Column(name = "music_id", nullable = false)
	private Long musicId;

	@Column(name = "tag_id", nullable = false)
	private Long tagId;

	private String source;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMusicId() {
		return musicId;
	}

	public void setMusicId(Long musicId) {
		this.musicId = musicId;
	}

	public Long getTagId() {
		return tagId;
	}

	public void setTagId(Long tagId) {
		this.tagId = tagId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
