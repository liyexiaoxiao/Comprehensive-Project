package com.donffroodus.music_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.donffroodus.music_service.entity.MusicResource;

@Repository
public interface MusicResourceRepository extends JpaRepository<MusicResource, Long> {

	List<MusicResource> findByUploaderIdOrderByIdDesc(Long uploaderId);

	/**
	 * 标题或艺人模糊匹配（不区分大小写）；artist 为 NULL 时按空串参与匹配。
	 * 使用显式 JPQL，避免派生查询名在 Or + ContainingIgnoreCase 组合下生成意外 SQL。
	 */
	@Query("""
			SELECT m FROM MusicResource m
			WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
			   OR LOWER(COALESCE(m.artist, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			""")
	List<MusicResource> searchByTitleOrArtistIgnoreCase(@Param("keyword") String keyword);
}
