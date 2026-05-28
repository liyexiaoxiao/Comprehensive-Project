package com.donffroodus.user_service.dto;

import java.util.List;

public class UserPreference {
    private Long userId;
    private List<ServiceScore> serviceScores;

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public List<ServiceScore> getServiceScores() {
        return serviceScores;
    }

    public void setServiceScores(List<ServiceScore> serviceScores) {
        this.serviceScores = serviceScores;
    }
}
