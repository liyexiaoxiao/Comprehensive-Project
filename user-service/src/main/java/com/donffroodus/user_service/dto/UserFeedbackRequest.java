package com.donffroodus.user_service.dto;

public class UserFeedbackRequest {
    private String service;
    private String feedback;
    private float rating;

    // Getters and Setters
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
}
