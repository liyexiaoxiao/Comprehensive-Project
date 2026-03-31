package com.donffroodus.user_service.dto;

public class AdminResetPasswordRequest {
    private String newPassword;

    // Getters and Setters
    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
