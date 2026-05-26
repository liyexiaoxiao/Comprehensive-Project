package com.donffroodus.meditation_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.donffroodus.meditation_service.dto.MiniMissionAdminResponse;
import com.donffroodus.meditation_service.dto.MiniMissionRequest;
import com.donffroodus.meditation_service.dto.MiniMissionResponse;
import com.donffroodus.meditation_service.entity.MiniMission;
import com.donffroodus.meditation_service.entity.MiniMissionLog;
import com.donffroodus.meditation_service.repository.MiniMissionLogRepository;
import com.donffroodus.meditation_service.repository.MiniMissionRepository;

import jakarta.transaction.Transactional;

@Service
public class MiniMissionService {
    @Autowired
    private MiniMissionRepository miniMissionRepository;

    @Autowired
    private MiniMissionLogRepository miniMissionLogRepository;

    public MiniMissionResponse getMiniMissionById(Long missionId) {
        MiniMissionResponse response = new MiniMissionResponse();
        miniMissionRepository.findById(missionId).ifPresent(mission -> {
            response.setTitle(mission.getTitle());
            response.setDescription(mission.getDescription());
            response.setRewardValue(mission.getRewardValue());
        });
        if (response.getTitle() == null) {
            throw new IllegalArgumentException("Mini mission not found");
        }
        return response;
    }

    public MiniMissionAdminResponse getMiniMissionByIdAdmin(Long missionId) {
        MiniMissionAdminResponse response = new MiniMissionAdminResponse();
        miniMissionRepository.findById(missionId).ifPresent(mission -> {
            response.setId(mission.getId());
            response.setTitle(mission.getTitle());
            response.setDescription(mission.getDescription());
            response.setRewardValue(mission.getRewardValue());
            response.setActive(mission.isActive());
            response.setCreatedAt(mission.getCreatedAt());
            response.setUpdatedAt(mission.getUpdatedAt());
        });
        if (response.getTitle() == null) {
            throw new IllegalArgumentException("Mini mission not found");
        }
        return response;
    }

    public MiniMissionAdminResponse getMiniMissionByTitleAdmin(String title) {
        MiniMissionAdminResponse response = new MiniMissionAdminResponse();
        miniMissionRepository.findByTitle(title).ifPresent(mission -> {
            response.setId(mission.getId());
            response.setTitle(mission.getTitle());
            response.setDescription(mission.getDescription());
            response.setRewardValue(mission.getRewardValue());
            response.setActive(mission.isActive());
            response.setCreatedAt(mission.getCreatedAt());
            response.setUpdatedAt(mission.getUpdatedAt());
        });
        if (response.getTitle() == null) {
            throw new IllegalArgumentException("Mini mission not found");
        }
        return response;
    }

    public List<MiniMissionAdminResponse> getAllMiniMissions() {
        return miniMissionRepository.findAll().stream().map(mission -> {
            MiniMissionAdminResponse response = new MiniMissionAdminResponse();
            response.setId(mission.getId());
            response.setTitle(mission.getTitle());
            response.setDescription(mission.getDescription());
            response.setRewardValue(mission.getRewardValue());
            response.setActive(mission.isActive());
            response.setCreatedAt(mission.getCreatedAt());
            response.setUpdatedAt(mission.getUpdatedAt());
            return response;
        }).toList();
    }

    public List<MiniMissionLog> getMyLogs(Long userId) {
        return miniMissionLogRepository.findByUserId(userId);
    }

    public void addNewMiniMission(MiniMissionRequest request) {
        MiniMission mission = new MiniMission();
        if (miniMissionRepository.existsByTitle(request.getTitle())) {
            throw new IllegalArgumentException("Mini mission with this title already exists");
        }
        mission.setTitle(request.getTitle());
        mission.setDescription(request.getDescription());
        mission.setRewardValue(request.getRewardValue());
        mission.setCreatedAt(java.time.LocalDateTime.now());
        miniMissionRepository.save(mission);
    }

    @Transactional
    public void deleteMiniMission(Long missionId) {
        miniMissionRepository.deleteById(missionId);
    }

    public void startMiniMission(Long missionId, Long userId) {
        if (miniMissionLogRepository.existsByStatusAndUserId(MiniMissionLog.MiniMissionStatus.IN_PROGRESS, userId)) {
            throw new IllegalStateException("User already has an active mini mission");
        }
        MiniMissionLog log = new MiniMissionLog();
        log.setUserId(userId);
        log.setMiniMissionId(missionId);
        log.setStatus(MiniMissionLog.MiniMissionStatus.IN_PROGRESS);
        log.setCreatedAt(java.time.LocalDateTime.now());
        miniMissionLogRepository.save(log);
    }

    public void AbortMiniMission(Long missionId, Long userId) {
        boolean hasActiveMission = miniMissionLogRepository.existsByStatusAndUserId(MiniMissionLog.MiniMissionStatus.IN_PROGRESS, userId);
        if (!hasActiveMission) {
            throw new IllegalStateException("User does not have an active mini mission to abort");
        }

        List<MiniMissionLog> activeMissions = miniMissionLogRepository.findByUserIdAndStatus(userId, MiniMissionLog.MiniMissionStatus.IN_PROGRESS);
        activeMissions.stream()
            .filter(log -> log.getMiniMissionId().equals(missionId))
            .findFirst()
            .ifPresent(log -> {
                log.setStatus(MiniMissionLog.MiniMissionStatus.FAILED);
                log.setUpdatedAt(java.time.LocalDateTime.now());
                miniMissionLogRepository.save(log);
            });
        
        if (activeMissions.isEmpty()) {
            throw new IllegalStateException("No active mini mission found for the user");
        }
    }

    public void completeMiniMission(Long missionId, Long userId) {
        boolean hasActiveMission = miniMissionLogRepository.existsByStatusAndUserId(MiniMissionLog.MiniMissionStatus.IN_PROGRESS, userId);
        if (!hasActiveMission) {
            throw new IllegalStateException("User does not have an active mini mission to complete");
        }
        List<MiniMissionLog> activeMissions = miniMissionLogRepository.findByUserIdAndStatus(userId, MiniMissionLog.MiniMissionStatus.IN_PROGRESS);
        activeMissions.stream()
            .filter(log -> log.getMiniMissionId().equals(missionId))
            .findFirst()
            .ifPresent(log -> {
                log.setStatus(MiniMissionLog.MiniMissionStatus.COMPLETED);
                log.setUpdatedAt(java.time.LocalDateTime.now());
                miniMissionLogRepository.save(log);
            });

        if (activeMissions.isEmpty()) {
            throw new IllegalStateException("No active mini mission found for the user");
        }
    }
}
