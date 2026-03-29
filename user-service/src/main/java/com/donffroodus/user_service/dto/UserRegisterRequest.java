package com.donffroodus.user_service.dto;

public class UserRegisterRequest {
    private String username;
    private String email;
    private String phone;
    private String password;

    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
