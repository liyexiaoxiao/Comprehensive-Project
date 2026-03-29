package com.donffroodus.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.donffroodus.user_service.dto.UserRegisterRequest;
import com.donffroodus.user_service.entity.UserInfo;
import com.donffroodus.user_service.repository.UserInfoRepository;

@Service
public class UserService {
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserInfo registerUser(UserRegisterRequest request)
    {
        if (userInfoRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && userInfoRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty() && userInfoRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already exists");
        }
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(request.getUsername());
        userInfo.setEmail(request.getEmail());
        userInfo.setPhone(request.getPhone());
        userInfo.setPassword(passwordEncoder.encode(request.getPassword()));

        return userInfoRepository.save(userInfo);
    }
}
