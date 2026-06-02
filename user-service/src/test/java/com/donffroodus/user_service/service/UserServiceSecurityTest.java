package com.donffroodus.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.donffroodus.user_service.dto.UserInfoRequest;
import com.donffroodus.user_service.dto.UserRegisterRequest;
import com.donffroodus.user_service.dto.UserUpdateMeRequest;
import com.donffroodus.user_service.entity.UserInfo;
import com.donffroodus.user_service.repository.OperationLogRepository;
import com.donffroodus.user_service.repository.UserInfoRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceSecurityTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OperationLogRepository operationLogRepository;

    @InjectMocks
    private UserService userService;

    private UserInfo otherUser;

    @BeforeEach
    void setUp() {
        otherUser = new UserInfo();
        otherUser.setId(2L);
        otherUser.setUsername("other-user");
        otherUser.setNickname("其他用户");
        otherUser.setEmail("other@example.com");
        otherUser.setAvatarUrl("/api/users/avatars/avatar-2-demo.jpg");
    }

    @Test
    void registerUserRejectsWeakPassword() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newbie");
        request.setPassword("12345678");

        assertThrows(RuntimeException.class, () -> userService.registerUser(request));
        verify(userInfoRepository, never()).save(any(UserInfo.class));
    }

    @Test
    void getUserSummariesByIdsHidesSensitiveFieldsForOtherUsers() {
        when(userInfoRepository.findAllById(List.of(2L))).thenReturn(List.of(otherUser));

        List<UserInfoRequest> summaries = userService.getUserSummariesByIds(List.of(2L), 1L);

        assertEquals(1, summaries.size());
        UserInfoRequest summary = summaries.get(0);
        assertEquals(2L, summary.getUserId());
        assertEquals("其他用户", summary.getNickname());
        assertEquals("/api/users/avatars/avatar-2-demo.jpg", summary.getAvatarUrl());
        assertNull(summary.getUsername());
        assertNull(summary.getEmail());
        assertNull(summary.getPhone());
    }

    @Test
    void updateMyInfoIgnoresAvatarUrlField() {
        UserInfo currentUser = new UserInfo();
        currentUser.setId(1L);
        currentUser.setUsername("me");
        currentUser.setAvatarUrl("/api/users/avatars/avatar-1-old.jpg");

        when(userInfoRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        UserUpdateMeRequest request = new UserUpdateMeRequest();
        request.setNickname("新昵称");
        request.setAvatarUrl("http://evil.example/steal");

        userService.updateMyInfo(1L, request);

        assertEquals("新昵称", currentUser.getNickname());
        assertEquals("/api/users/avatars/avatar-1-old.jpg", currentUser.getAvatarUrl());
        verify(userInfoRepository).save(currentUser);
    }
}
