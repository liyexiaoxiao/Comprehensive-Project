package com.donffroodus.user_service.controller;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.donffroodus.user_service.dto.AdminResetPasswordRequest;
import com.donffroodus.user_service.dto.AdminUpdateUserRequest;
import com.donffroodus.user_service.dto.UserChangePasswordRequest;
import com.donffroodus.user_service.dto.UserInfoRequest;
import com.donffroodus.user_service.dto.UserLoginRequest;
import com.donffroodus.user_service.dto.UserRegisterRequest;
import com.donffroodus.user_service.dto.UserUpdateMeRequest;
import com.donffroodus.user_service.entity.UserInfo;
import com.donffroodus.user_service.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegisterRequest request) {
        try {
            userService.registerUser(request);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        try {
            String token = userService.loginUser(request);
            return ResponseEntity.ok("Bearer " + token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyAvatar(@RequestHeader("X-User-Id") Long userId, @RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = userService.uploadMyAvatar(userId, file);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<?> getAvatar(@PathVariable String filename) {
        try {
            Resource resource = userService.loadAvatarResource(filename);
            String contentType = Files.probeContentType(resource.getFile().toPath());
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        if (limit <= 0) {
            return ResponseEntity.badRequest().body("limit must be positive");
        }
        return ResponseEntity.ok(userService.searchUsers(keyword, limit, currentUserId));
    }

    @GetMapping("/summaries")
    public ResponseEntity<?> getUserSummaries(@RequestParam("ids") String ids) {
        if (ids == null || ids.isBlank()) {
            return ResponseEntity.badRequest().body("ids is required");
        }

        try {
            List<Long> userIds = java.util.Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .map(Long::valueOf)
                    .toList();
            return ResponseEntity.ok(userService.getUserSummariesByIds(userIds));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ids must be comma-separated numbers");
        }
    }

    @PreAuthorize("hasRole('ADMIN')") 
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        try {
            userService.deleteUserAsAdmin(id, SecurityContextHolder.getContext().getAuthentication().getName(), request.getRemoteAddr());
            return ResponseEntity.ok("删除操作成功，用户ID: " + id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody AdminUpdateUserRequest updateDto, HttpServletRequest httpRequest) {
        try {
            userService.updateUserAsAdmin(id, updateDto, SecurityContextHolder.getContext().getAuthentication().getName(), httpRequest.getRemoteAddr());
            return ResponseEntity.ok("用户信息更新成功，用户ID: " + id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody AdminResetPasswordRequest resetDto, HttpServletRequest httpRequest) {
        try {
            userService.resetPasswordAsAdmin(id, resetDto, SecurityContextHolder.getContext().getAuthentication().getName(), httpRequest.getRemoteAddr());
            return ResponseEntity.ok("密码重置成功，用户ID: " + id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PatchMapping("/update-info")
    public ResponseEntity<?> updateMyInfo(@RequestBody UserUpdateMeRequest updateDto, @RequestHeader("X-User-Id") Long userId) {
        try {
            userService.updateMyInfo(userId, updateDto);
            return ResponseEntity.ok("个人信息更新成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-me")
    public ResponseEntity<?> deleteMyAccount(@RequestHeader("X-User-Id") Long userId) {
        try {
            userService.deleteMyAccount(userId);
            return ResponseEntity.ok("账户删除成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changeMyPassword(@RequestBody UserChangePasswordRequest request, @RequestHeader("X-User-Id") Long userId) {
        try {
            userService.changeMyPassword(userId, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok("密码修改成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-users")
    public ResponseEntity<?> getUsers(@RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "10") int limit) {
        if (offset < 0 || limit <= 0) {
            return ResponseEntity.badRequest().body("Invalid offset or limit");
        }
        UserInfo[] users = userService.getUserInfos(offset, limit);
        UserInfoRequest[] userInfoRequests = new UserInfoRequest[users.length];
        for (int i = 0; i < users.length; i++) {
            UserInfo user = users[i];
            UserInfoRequest userInfoRequest = new UserInfoRequest();
            userInfoRequest.setUserId(user.getId());
            userInfoRequest.setUsername(user.getUsername());
            userInfoRequest.setNickname(user.getNickname());
            userInfoRequest.setEmail(user.getEmail());
            userInfoRequest.setPhone(user.getPhone());
            userInfoRequest.setRole(user.getRole());
            userInfoRequest.setStatus(user.getStatus());
            userInfoRequest.setAvatarUrl(user.getAvatarUrl());
            userInfoRequest.setBio(user.getBio());
            userInfoRequests[i] = userInfoRequest;
        }
        return ResponseEntity.ok(userInfoRequests);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserInfoRequest user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
