package com.donffroodus.user_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donffroodus.user_service.entity.UserFeedback;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
    List<UserFeedback> findByUserId(Long userId);
    List<UserFeedback> findByService(String service);
    List<UserFeedback> findByUserIdAndService(Long userId, String service);
    List<UserFeedback> findByRatingGreaterThanEqual(float rating);
    List<UserFeedback> findByRatingLessThanEqual(float rating);
}
