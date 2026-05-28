package com.donffroodus.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.donffroodus.user_service.repository.UserFeedbackRepository;
import com.donffroodus.user_service.dto.UserFeedbackRequest;
import com.donffroodus.user_service.dto.UserFeedbackResponse;
import com.donffroodus.user_service.entity.UserFeedback;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.donffroodus.user_service.dto.ServiceScore;
import com.donffroodus.user_service.dto.UserPreference;

@Service
public class UserFeedbackService {
    @Autowired
    private UserFeedbackRepository userFeedbackRepository;

    private UserFeedbackResponse convertToResponse(UserFeedback feedback) {
        UserFeedbackResponse response = new UserFeedbackResponse();
        response.setService(feedback.getService());
        response.setFeedback(feedback.getFeedback());
        response.setRating(feedback.getRating());
        response.setCreatedAt(feedback.getCreatedAt());
        return response;
    }

    public ResponseEntity<?> saveFeedback(Long userId, UserFeedbackRequest feedbackRequest) {
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setService(feedbackRequest.getService());
        feedback.setFeedback(feedbackRequest.getFeedback());
        feedback.setRating(feedbackRequest.getRating());
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());

        userFeedbackRepository.save(feedback);
        return ResponseEntity.ok("Feedback submitted successfully");
    }

    public List<UserFeedbackResponse> getAllFeedback(String service, Float minRating, Float maxRating, String startDate, String endDate, Long userId) {
        List<UserFeedback> feedbackList = userFeedbackRepository.findAll();
        if (userId != null) {
            feedbackList = feedbackList.stream().filter(f -> f.getUserId().equals(userId)).collect(Collectors.toList());
        }
        if (service != null) {
            feedbackList = feedbackList.stream().filter(f -> f.getService().equalsIgnoreCase(service)).collect(Collectors.toList());
        }
        if (minRating != null) {
            feedbackList = feedbackList.stream().filter(f -> f.getRating() >= minRating).collect(Collectors.toList());
        }
        if (maxRating != null) {
            feedbackList = feedbackList.stream().filter(f -> f.getRating() <= maxRating).collect(Collectors.toList());
        }
        if (startDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate);
            feedbackList = feedbackList.stream().filter(f -> f.getCreatedAt().isAfter(start)).collect(Collectors.toList());
        }
        if (endDate != null) {
            LocalDateTime end = LocalDateTime.parse(endDate);
            feedbackList = feedbackList.stream().filter(f -> f.getCreatedAt().isBefore(end)).collect(Collectors.toList());
        }
        return feedbackList.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public UserFeedbackResponse getFeedbackById(Long id) {
        UserFeedback feedback = userFeedbackRepository.findById(id).orElseThrow(() -> new RuntimeException("Feedback not found"));
        return convertToResponse(feedback);
    }

    public UserPreference getUserPreferences(Long userId) {
        List<UserFeedback> feedbackList = userFeedbackRepository.findByUserId(userId);
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        ServiceScore serviceScore = new ServiceScore();
        feedbackList.forEach(f -> {
            serviceScore.setService(f.getService());
            serviceScore.setScore((float) feedbackList.stream().filter(fb -> fb.getService().equals(f.getService())).mapToDouble(UserFeedback::getRating).average().orElse(0.0));
            preference.getServiceScores().add(serviceScore);
        });
        return preference;
    }

}
