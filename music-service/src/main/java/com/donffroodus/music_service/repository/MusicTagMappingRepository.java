package com.donffroodus.music_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.MusicTagMapping;

@Repository
public interface MusicTagMappingRepository extends JpaRepository<MusicTagMapping, Long> {

	List<MusicTagMapping> findByMusicId(Long musicId);

	List<MusicTagMapping> findByTagId(Long tagId);

	void deleteByMusicId(Long musicId);

	void deleteByTagId(Long tagId);
}
