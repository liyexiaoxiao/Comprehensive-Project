package com.donffroodus.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.donffroodus.user_service.dto.UserInfoRequest;
import com.donffroodus.user_service.dto.UserLoginRequest;
import com.donffroodus.user_service.dto.UserRegisterRequest;
import com.donffroodus.user_service.dto.UserUpdateMeRequest;
import com.donffroodus.user_service.entity.UserInfo;
import com.donffroodus.user_service.repository.OperationLogRepository;
import com.donffroodus.user_service.repository.UserInfoRepository;
import com.donffroodus.user_service.utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OperationLogRepository operationLogRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserShouldSaveEncodedPasswordWhenRequestIsValid() {
        UserRegisterRequest request = registerRequest("alice", "alice@example.com", "13800000000", "raw-password");
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userInfoRepository.save(any(UserInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserInfo savedUser = userService.registerUser(request);

        assertEquals("alice", savedUser.getUsername());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("13800000000", savedUser.getPhone());
        assertEquals("encoded-password", savedUser.getPassword());
        verify(userInfoRepository).save(any(UserInfo.class));
    }

    @Test
    void registerUserShouldRejectDuplicateUsername() {
        UserRegisterRequest request = registerRequest("alice", null, null, "raw-password");
        when(userInfoRepository.existsByUsername("alice")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.registerUser(request));

        assertEquals("Username already exists", exception.getMessage());
        verify(userInfoRepository, never()).save(any(UserInfo.class));
    }

    @Test
    void registerUserShouldRejectMissingPassword() {
        UserRegisterRequest request = registerRequest("alice", null, null, "");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.registerUser(request));

        assertEquals("Password is required", exception.getMessage());
        verify(userInfoRepository, never()).save(any(UserInfo.class));
    }

    @Test
    void loginUserShouldReturnTokenWhenPasswordMatches() {
        UserLoginRequest request = loginRequest("alice", "raw-password");
        UserInfo existingUser = user(7L, "alice", "encoded-password", "user");
        when(userInfoRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);
        when(jwtUtils.generateToken("alice", 7L, "user")).thenReturn("jwt-token");

        String token = userService.loginUser(request);

        assertEquals("jwt-token", token);
    }

    @Test
    void loginUserShouldRejectWrongPassword() {
        UserLoginRequest request = loginRequest("alice", "wrong-password");
        UserInfo existingUser = user(7L, "alice", "encoded-password", "user");
        when(userInfoRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.loginUser(request));

        assertEquals("Invalid username or password", exception.getMessage());
        verify(jwtUtils, never()).generateToken(any(), any(), any());
    }

    @Test
    void updateMyInfoShouldRejectDuplicateEmail() {
        UserInfo existingUser = user(7L, "alice", "encoded-password", "user");
        existingUser.setEmail("old@example.com");
        UserUpdateMeRequest request = new UserUpdateMeRequest();
        request.setEmail("new@example.com");
        when(userInfoRepository.findById(7L)).thenReturn(Optional.of(existingUser));
        when(userInfoRepository.existsByEmail("new@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.updateMyInfo(7L, request));

        assertEquals("Email already exists", exception.getMessage());
        verify(userInfoRepository, never()).save(any(UserInfo.class));
    }

    @Test
    void changeMyPasswordShouldSaveEncodedNewPasswordWhenOldPasswordMatches() {
        UserInfo existingUser = user(7L, "alice", "old-encoded", "user");
        when(userInfoRepository.findById(7L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("old-password", "old-encoded")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded");

        userService.changeMyPassword(7L, "old-password", "new-password");

        ArgumentCaptor<UserInfo> userCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoRepository).save(userCaptor.capture());
        assertEquals("new-encoded", userCaptor.getValue().getPassword());
    }

    @Test
    void deleteMyAccountShouldPublishUserDeleteEvent() {
        UserInfo existingUser = user(7L, "alice", "encoded-password", "user");
        when(userInfoRepository.findById(7L)).thenReturn(Optional.of(existingUser));

        userService.deleteMyAccount(7L);

        verify(userInfoRepository).delete(existingUser);
        verify(rabbitTemplate).convertAndSend("user.exchange", "user.delete", 7L);
    }

    @Test
    void searchUsersShouldReturnEmptyListWhenKeywordIsBlank() {
        List<UserInfoRequest> result = userService.searchUsers("   ", 20, null);

        assertTrue(result.isEmpty());
        verify(userInfoRepository, never()).findById(any());
    }

    @Test
    void searchUsersShouldKeepExactIdFirstAndSkipDuplicatesAndExcludedUser() {
        UserInfo exactMatch = user(7L, "alice", "encoded-password", "user");
        exactMatch.setNickname("Alice");
        UserInfo duplicate = user(7L, "alice", "encoded-password", "user");
        UserInfo excluded = user(8L, "bob", "encoded-password", "user");
        UserInfo fuzzyMatch = user(9L, "carol", "encoded-password", "user");
        fuzzyMatch.setNickname("Carol");

        when(userInfoRepository.findById(7L)).thenReturn(Optional.of(exactMatch));
        when(userInfoRepository.findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
                eq("7"),
                eq("7"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(duplicate, excluded, fuzzyMatch)));

        List<UserInfoRequest> result = userService.searchUsers("7", 10, 8L);

        assertEquals(2, result.size());
        assertEquals(7L, result.get(0).getUserId());
        assertEquals(9L, result.get(1).getUserId());
    }

    private static UserRegisterRequest registerRequest(String username, String email, String phone, String password) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword(password);
        return request;
    }

    private static UserLoginRequest loginRequest(String username, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private static UserInfo user(Long id, String username, String password, String role) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }
}
