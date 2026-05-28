package com.donffroodus.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.donffroodus.user_service.repository.UserFeedbackRepository;

@Service
public class UserFeedbackService {
    @Autowired
    private UserFeedbackRepository userFeedbackRepository;
}
