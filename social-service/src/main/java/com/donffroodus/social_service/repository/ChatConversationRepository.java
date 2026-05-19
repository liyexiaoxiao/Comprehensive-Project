package com.donffroodus.social_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.donffroodus.social_service.entity.ChatConversation;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

	Optional<ChatConversation> findByUserOneIdAndUserTwoId(Long userOneId, Long userTwoId);

	@Query("""
			SELECT c FROM ChatConversation c
			WHERE c.userOneId = :userId OR c.userTwoId = :userId
			ORDER BY c.lastMessageAt DESC, c.createdAt DESC
			""")
	List<ChatConversation> findAllForUser(@Param("userId") Long userId);
}
