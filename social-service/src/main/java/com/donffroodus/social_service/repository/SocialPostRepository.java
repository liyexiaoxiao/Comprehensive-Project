package com.donffroodus.social_service.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.social_service.entity.SocialPost;

public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {

	Page<SocialPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

	Page<SocialPost> findByUserIdInOrderByCreatedAtDesc(Collection<Long> userIds, Pageable pageable);

	Page<SocialPost> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Optional<SocialPost> findByIdAndUserId(Long id, Long userId);
}
