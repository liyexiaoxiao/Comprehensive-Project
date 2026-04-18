package com.donffroodus.meditation_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.donffroodus.meditation_service.dto.MeditationRequest;
import com.donffroodus.meditation_service.entity.MeditationLog;
import com.donffroodus.meditation_service.repository.MeditationLogRepository;

@Service
public class MeditationService {
    @Autowired
    private MeditationLogRepository meditationLogRepository;

    public List<MeditationLog> getMediListByUserId(Long userId) {
        return meditationLogRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    public MeditationLog saveMeditationLog(Long userId, MeditationRequest request) {
        MeditationLog log = new MeditationLog();
        log.setUserId(userId);
        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now();
        log.setStartTime(startTime);
        log.setDuration(request.getDuration());
        log.setMusicId(request.getMusicId());
        log.setImageId(request.getImageId());
        return meditationLogRepository.save(log);
    }

    public void userDeleteMeditationLog(Long userId, Long logId) {
        MeditationLog log = meditationLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Meditation log not found"));
        if (!log.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        meditationLogRepository.delete(log);
    }

    public void adminDeleteMeditationLog(Long logId) {
        MeditationLog log = meditationLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Meditation log not found"));
        meditationLogRepository.delete(log);
    }
}
