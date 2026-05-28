package com.donffroodus.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.user_service.dto.UserFeedbackRequest;
import com.donffroodus.user_service.service.UserFeedbackService;

@RestController
@RequestMapping("/api/feedback")
public class UserFeedbackController {
    @Autowired
    private UserFeedbackService userFeedbackService;

    @PostMapping("/mine")
    public ResponseEntity<?> submitFeedback(@RequestHeader("X-User-ID") Long userId, @RequestBody UserFeedbackRequest feedbackRequest) {
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return userFeedbackService.saveFeedback(userId, feedbackRequest);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getFeedbackOfUser(@RequestHeader("X-User-ID") Long userId, @RequestParam(required = false) String service, @RequestParam(required = false) Float minRating, @RequestParam(required = false) Float maxRating, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(userFeedbackService.getAllFeedback(service, minRating, maxRating, startDate, endDate, userId));
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> getUserPreferences(@RequestHeader("X-User-ID") Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(userFeedbackService.getUserPreferences(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<?> getUserPreferencesAdmin(@PathVariable Long userId) {
        return ResponseEntity.ok(userFeedbackService.getUserPreferences(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllFeedback(@RequestParam(required = false) String service, @RequestParam(required = false) Float minRating, @RequestParam(required = false) Float maxRating, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate, @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(userFeedbackService.getAllFeedback(service, minRating, maxRating, startDate, endDate, userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getFeedbackById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userFeedbackService.getFeedbackById(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
