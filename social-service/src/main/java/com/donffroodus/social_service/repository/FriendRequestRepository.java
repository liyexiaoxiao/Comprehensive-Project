package com.donffroodus.social_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.social_service.entity.FriendRequest;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

	List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, String status);

	List<FriendRequest> findBySenderIdOrderByCreatedAtDesc(Long senderId);

	Optional<FriendRequest> findByIdAndReceiverId(Long id, Long receiverId);

	boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, String status);
}
