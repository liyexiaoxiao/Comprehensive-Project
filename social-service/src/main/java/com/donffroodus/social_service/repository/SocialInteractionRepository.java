package com.donffroodus.social_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.donffroodus.social_service.entity.SocialInteraction;

public interface SocialInteractionRepository extends JpaRepository<SocialInteraction, Long> {

	List<SocialInteraction> findByPostIdOrderByCreatedAtAsc(Long postId);

	Optional<SocialInteraction> findByPostIdAndUserIdAndLikedTrueAndCommentIsNullAndTargetInteractionIdIsNull(Long postId,
			Long userId);

	Optional<SocialInteraction> findByPostIdAndUserIdAndLikedTrueAndCommentIsNullAndTargetInteractionId(Long postId,
			Long userId,
			Long targetInteractionId);

	List<SocialInteraction> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

	List<SocialInteraction> findByUserIdAndCommentIsNotNullOrderByCreatedAtDesc(Long userId);

	@Modifying
	@Query("delete from SocialInteraction i where i.postId = :postId")
	void deleteByPostId(@Param("postId") Long postId);

	Optional<SocialInteraction> findByIdAndUserId(Long id, Long userId);
}
