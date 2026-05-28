package com.donffroodus.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.user_service.service.UserFeedbackService;

@RestController
@RequestMapping("/api/feedback")
public class UserFeedbackController {
    @Autowired
    private UserFeedbackService userFeedbackService;

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

    
}
