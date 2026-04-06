package com.donffroodus.data_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.donffroodus.data_service.mongo.UserBehaviorLog;

public interface UserBehaviorLogRepository extends MongoRepository<UserBehaviorLog, String> {

	List<UserBehaviorLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
