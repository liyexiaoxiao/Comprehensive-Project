package com.donffroodus.user_service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.donffroodus.user_service.dto.AdminResetPasswordRequest;
import com.donffroodus.user_service.dto.AdminUpdateUserRequest;
import com.donffroodus.user_service.dto.UserInfoRequest;
import com.donffroodus.user_service.dto.UserLoginRequest;
import com.donffroodus.user_service.dto.UserRegisterRequest;
import com.donffroodus.user_service.dto.UserUpdateMeRequest;
import com.donffroodus.user_service.entity.OperationLog;
import com.donffroodus.user_service.entity.UserInfo;
import com.donffroodus.user_service.repository.OperationLogRepository;
import com.donffroodus.user_service.repository.UserInfoRepository;
import com.donffroodus.user_service.utils.JwtUtils;

@Service
public class UserService {
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public UserInfo registerUser(UserRegisterRequest request)
    {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        if (userInfoRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && userInfoRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty() && userInfoRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already exists");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(request.getUsername());
        userInfo.setEmail(request.getEmail());
        userInfo.setPhone(request.getPhone());
        userInfo.setPassword(passwordEncoder.encode(request.getPassword()));

        return userInfoRepository.save(userInfo);
    }

    public String loginUser(UserLoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        UserInfo userInfo = userInfoRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), userInfo.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        return jwtUtils.generateToken(userInfo.getUsername(), userInfo.getId(), userInfo.getRole());
    }

    private void logOperation(String adminUserName, String type, String details, String ip) {
        userInfoRepository.findByUsername(adminUserName).ifPresent(admin -> {
            OperationLog log = new OperationLog();
            log.setOperatorId(admin.getId());
            log.setOperationType(type);
            log.setOperationDetails(details);
            log.setIpAddress(ip);
            operationLogRepository.save(log);
        });
    }

    public void deleteUserAsAdmin(Long targetUserId, String adminUserName, String ip) {
        UserInfo targetUser = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userInfoRepository.delete(targetUser);

        logOperation(adminUserName, "DELETE_USER", "删除了用户: " + targetUserId + "，用户名: " + targetUser.getUsername() + " (管理员: " + adminUserName + ")", ip);
    }

    public void updateUserAsAdmin(Long targetUserId, AdminUpdateUserRequest request, String adminUserNamd, String ip) {
        UserInfo targetUser = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUsername() != null && !request.getUsername().isEmpty() && !request.getUsername().equals(targetUser.getUsername())) {
            if (userInfoRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            targetUser.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && !request.getEmail().equals(targetUser.getEmail())) {
            if (userInfoRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            targetUser.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty() && !request.getPhone().equals(targetUser.getPhone())) {
            if (userInfoRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("Phone number already exists");
            }
            targetUser.setPhone(request.getPhone());
        }
        if (request.getNickname() != null) {
            targetUser.setNickname(request.getNickname());
        }
        if (request.getRole() != null) {
            targetUser.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            targetUser.setStatus(request.getStatus());
        }
        if (request.getAvatarUrl() != null) {
            targetUser.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            targetUser.setBio(request.getBio());
        }

        userInfoRepository.save(targetUser);

        logOperation(adminUserNamd, "UPDATE_USER", "更新了用户: " + targetUserId + "，用户名: " + targetUser.getUsername() + " (管理员: " + adminUserNamd + ")", ip);
    }

    public void resetPasswordAsAdmin(Long targetUserId, AdminResetPasswordRequest request, String adminUserName, String ip) {
        UserInfo targetUser = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        targetUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userInfoRepository.save(targetUser);

        logOperation(adminUserName, "RESET_PASSWORD", "重置了用户: " + targetUserId + "，用户名: " + targetUser.getUsername() + " 的密码 (管理员: " + adminUserName + ")", ip);
    }
    
    public void updateMyInfo(Long userId, UserUpdateMeRequest request) {
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !request.getEmail().isEmpty() && !request.getEmail().equals(userInfo.getEmail())) {
            if (userInfoRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            userInfo.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty() && !request.getPhone().equals(userInfo.getPhone())) {
            if (userInfoRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("Phone number already exists");
            }
            userInfo.setPhone(request.getPhone());
        }
        if (request.getNickname() != null) {
            userInfo.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            userInfo.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            userInfo.setBio(request.getBio());
        }

        userInfoRepository.save(userInfo);
    }

    public void changeMyPassword(Long userId, String oldPassword, String newPassword) {
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, userInfo.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        userInfo.setPassword(passwordEncoder.encode(newPassword));
        userInfoRepository.save(userInfo);
    }

    public void deleteMyAccount(Long userId) {
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long deletedUserId = userInfo.getId();
        userInfoRepository.delete(userInfo);

        rabbitTemplate.convertAndSend("user.exchange", "user.delete", deletedUserId);
        System.out.println("User deleted: " + deletedUserId);
    }

    public UserInfo getUserInfo(String userId) {
        return userInfoRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserInfo[] getUserInfos(int offset, int limit) {
        return userInfoRepository.findAll().stream().skip(offset).limit(limit).toArray(UserInfo[]::new);
    }

    public UserInfoRequest getUserById(Long userId) {
        UserInfo user = userInfoRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserInfoRequest safeUser = new UserInfoRequest();
        safeUser.setUserId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setNickname(user.getNickname());
        safeUser.setEmail(user.getEmail());
        safeUser.setPhone(user.getPhone());
        safeUser.setAvatarUrl(user.getAvatarUrl());
        safeUser.setBio(user.getBio());

        return safeUser;
    }

    public List<UserInfoRequest> searchUsers(String keyword, int limit, Long excludeUserId) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            return List.of();
        }
        int boundedLimit = Math.min(Math.max(limit, 1), 50);
        List<UserInfoRequest> result = new ArrayList<>();
        List<Long> seenUserIds = new ArrayList<>();

        try {
            Long exactId = Long.valueOf(q);
            userInfoRepository.findById(exactId).ifPresent(user -> {
                if ((excludeUserId == null || !excludeUserId.equals(user.getId()))) {
                    result.add(toSafeUserInfo(user));
                    seenUserIds.add(user.getId());
                }
            });
        } catch (NumberFormatException ex) {
            // q is not an id
        }

        int remain = boundedLimit - result.size();
        if (remain <= 0) {
            return result;
        }

        userInfoRepository.findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
                q,
                q,
                PageRequest.of(0, remain))
                .forEach(user -> {
                    if (excludeUserId != null && excludeUserId.equals(user.getId())) {
                        return;
                    }
                    if (seenUserIds.contains(user.getId())) {
                        return;
                    }
                    result.add(toSafeUserInfo(user));
                    seenUserIds.add(user.getId());
                });

        return result;
    }

    private static UserInfoRequest toSafeUserInfo(UserInfo user) {
        UserInfoRequest safeUser = new UserInfoRequest();
        safeUser.setUserId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setNickname(user.getNickname());
        safeUser.setEmail(user.getEmail());
        safeUser.setPhone(user.getPhone());
        safeUser.setAvatarUrl(user.getAvatarUrl());
        safeUser.setBio(user.getBio());
        return safeUser;
    }
}
