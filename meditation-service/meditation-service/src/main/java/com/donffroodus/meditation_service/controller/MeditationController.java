package com.donffroodus.meditation_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.meditation_service.dto.MeditationCountDownStartRequest;
import com.donffroodus.meditation_service.dto.MeditationLogDeleteRequest;
import com.donffroodus.meditation_service.dto.MeditationRequest;
import com.donffroodus.meditation_service.service.MeditationService;

@RestController
@RequestMapping("/api")
public class MeditationController {

    @Autowired
    private MeditationService meditationService;

    private Long getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null; 
    }

    private Long resolveUserId(String xUserId) {
        if (xUserId != null && !xUserId.isBlank()) {
            try {
                return Long.parseLong(xUserId.trim());
            } catch (NumberFormatException ignored) {
                // fall back to SecurityContext below
            }
        }
        return getUserIdFromSecurityContext();
    }

    private String getUserRoleFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().toString(); 
        }
        return null; 
    }

    @GetMapping("/test")
    public ResponseEntity<String> testGatewayHeaders(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Name", required = false) String headerUsername) {
        
        Long userId = resolveUserId(xUserId);
        String role = getUserRoleFromSecurityContext();

        // 2. 拼装返回信息
        String responseMessage = String.format(
                "-> 从 SecurityContext 提取的 UserID: %d\n" +
                "-> 从 SecurityContext 提取的 Role: %s\n" +
                "-> 从 HTTP Header 直接提取的 Username: %s",
                userId, role, headerUsername
        );

        return ResponseEntity.ok(responseMessage);
    }

    @GetMapping("/my-meditation-logs")
    public ResponseEntity<?> getAllMyLogs(@RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        Long userId = resolveUserId(xUserId);
        return ResponseEntity.ok(meditationService.getMediListByUserId(userId));
    }
    
    @PostMapping("/my-meditation-logs")
    public ResponseEntity<?> saveMyLog(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody MeditationRequest request) {
        Long userId = resolveUserId(xUserId);
        return ResponseEntity.ok(meditationService.saveMeditationLog(userId, request));
    }

    @PostMapping("/my-meditation-logs/delete")
    public ResponseEntity<?> deleteMyLog(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody MeditationLogDeleteRequest request) {
        Long userId = resolveUserId(xUserId);
        meditationService.userDeleteMeditationLog(userId, request.getLogId());
        return ResponseEntity.ok("Deleted meditation log with ID: " + request.getLogId());
    }

    @PostMapping("/start-countdown")
    public ResponseEntity<?> startCountdown(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody MeditationCountDownStartRequest request) {
        Long userId = resolveUserId(xUserId);
        return ResponseEntity.ok(meditationService.startCountDownSession(userId, request));
    }

    @PostMapping("/stop-countdown")
    public ResponseEntity<?> stopCountdown(@RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        Long userId = resolveUserId(xUserId);
        meditationService.finishCountDownSession(userId);
        return ResponseEntity.ok("Meditation session stopped for user ID: " + userId);
    }

    @PostMapping("/complete-countdown")
    public ResponseEntity<?> completeCountdown(@RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        Long userId = resolveUserId(xUserId);
        meditationService.completeCountDownSession(userId);
        return ResponseEntity.ok("Meditation session completed for user ID: " + userId);
    }

    @GetMapping("/countdown-left")
    public ResponseEntity<?> getCountdownLeft(@RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        Long userId = resolveUserId(xUserId);
        Integer leftDuration = meditationService.getSessionLeftDuration(userId);
        return ResponseEntity.ok(leftDuration);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/meditation-logs/{user_id}")
    public ResponseEntity<?> getAdminLogs(@PathVariable Long user_id) {
        return ResponseEntity.ok(meditationService.getMediListByUserId(user_id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/meditation-logs/{user_id}")
    public ResponseEntity<?> postAdminLog(@PathVariable Long user_id, @RequestBody MeditationRequest request) {
        return ResponseEntity.ok(meditationService.saveMeditationLog(user_id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/meditation-logs/delete")
    public ResponseEntity<?> deleteAdminLog(@RequestBody MeditationLogDeleteRequest request) {
        meditationService.adminDeleteMeditationLog(request.getLogId());
        return ResponseEntity.ok("Admin deleted meditation log with ID: " + request.getLogId());
    }
}
