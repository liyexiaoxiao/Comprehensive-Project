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
            return Long.parseLong(authentication.getPrincipal().toString()); 
        }
        return null; 
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
            @RequestHeader(value = "X-User-Name", required = false) String headerUsername) {
        
        Long userId = getUserIdFromSecurityContext();
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
    public ResponseEntity<?> getAllMyLogs() {
        Long userId = getUserIdFromSecurityContext();
        return ResponseEntity.ok(meditationService.getMediListByUserId(userId));
    }
    
    @PostMapping("/my-meditation-logs")
    public ResponseEntity<?> saveMyLog(@RequestBody MeditationRequest request) {
        Long userId = getUserIdFromSecurityContext();
        return ResponseEntity.ok(meditationService.saveMeditationLog(userId, request));
    }

    @PostMapping("/my-meditation-logs/delete")
    public ResponseEntity<?> deleteMyLog(@RequestBody MeditationLogDeleteRequest request) {
        Long userId = getUserIdFromSecurityContext();
        meditationService.userDeleteMeditationLog(userId, request.getLogId());
        return ResponseEntity.ok("Deleted meditation log with ID: " + request.getLogId());
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