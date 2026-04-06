package com.donffroodus.data_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.donffroodus.data_service.mongo.EmotionSnapshot;

public interface EmotionSnapshotRepository extends MongoRepository<EmotionSnapshot, String> {

	List<EmotionSnapshot> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
