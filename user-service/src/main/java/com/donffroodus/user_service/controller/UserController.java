package com.donffroodus.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.donffroodus.user_service.dto.AdminResetPasswordRequest;
import com.donffroodus.user_service.dto.AdminUpdateUserRequest;
import com.donffroodus.user_service.dto.UserChangePasswordRequest;
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
    public ResponseEntity<?> updateMyInfo(@RequestBody UserUpdateMeRequest updateDto, @RequestHeader("X-User-Id") Long userId, HttpServletRequest httpRequest) {
        try {
            userService.updateMyInfo(userId, updateDto);
            return ResponseEntity.ok("个人信息更新成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-me")
    public ResponseEntity<?> deleteMyAccount(@RequestHeader("X-User-Id") Long userId,HttpServletRequest httpRequest) {
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
    public String getMethodName(@RequestParam(defaultValue = "0") String offset, @RequestParam(defaultValue = "10") String limit) {
        UserInfo[] users = userService.getUserInfos(Integer.parseInt(offset), Integer.parseInt(limit));
        String JsonResponse = "[";
        for (UserInfo user : users) {
            JsonResponse += "{ \"id\": " + user.getId() + ", \"username\": \"" + user.getUsername() + "\", \"nickname\": \"" + user.getNickname() + "\", \"email\": \"" + user.getEmail() + "\", \"phone\": \"" + user.getPhone() + "\", \"role\": \"" + user.getRole() + "\", \"status\": \"" + user.getStatus() + "\", \"avatarUrl\": \"" + user.getAvatarUrl() + "\", \"bio\": \"" + user.getBio() + "\" },";
        }
        if (users.length > 0) {
            JsonResponse = JsonResponse.substring(0, JsonResponse.length() - 1); // Remove trailing comma
        }
        JsonResponse += "]";
        return JsonResponse;

    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserInfo user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
