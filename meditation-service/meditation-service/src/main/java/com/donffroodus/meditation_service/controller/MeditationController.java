package com.donffroodus.meditation_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeditationController {

    private String getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return (String) authentication.getPrincipal(); 
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
        
        String userId = getUserIdFromSecurityContext();
        String role = getUserRoleFromSecurityContext();

        // 2. 拼装返回信息
        String responseMessage = String.format(
                "-> 从 SecurityContext 提取的 UserID: %s\n" +
                "-> 从 SecurityContext 提取的 Role: %s\n" +
                "-> 从 HTTP Header 直接提取的 Username: %s",
                userId, role, headerUsername
        );

        return ResponseEntity.ok(responseMessage);
    }
}