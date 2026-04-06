package com.donffroodus.data_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.donffroodus.data_service.mongo.AiChatLog;

public interface AiChatLogRepository extends MongoRepository<AiChatLog, String> {

	List<AiChatLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	List<AiChatLog> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);
}
