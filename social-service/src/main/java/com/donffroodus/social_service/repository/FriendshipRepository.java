package com.donffroodus.social_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.social_service.entity.Friendship;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	List<Friendship> findByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<Friendship> findByIdAndUserId(Long id, Long userId);

	Optional<Friendship> findByUserIdAndFriendUserId(Long userId, Long friendUserId);

	boolean existsByUserIdAndFriendUserId(Long userId, Long friendUserId);

	void deleteByUserIdAndFriendUserId(Long userId, Long friendUserId);
}
