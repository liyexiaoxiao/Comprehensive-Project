package com.donffroodus.user_service.dto;

import java.time.LocalDateTime;

public class UserFeedbackResponse {
    String service;
    String feedback;
    float rating;
    LocalDateTime createdAt;

    public String getService() {
        return service;
    }
    public void setService(String service) {
        this.service = service;
    }
    public String getFeedback() {
        return feedback;
    }
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    public float getRating() {
        return rating;
    }
    public void setRating(float rating) {
        this.rating = rating;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
