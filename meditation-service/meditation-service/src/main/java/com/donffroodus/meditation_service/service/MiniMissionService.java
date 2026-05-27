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

    private MiniMission getRequiredMission(Long missionId) {
        return miniMissionRepository.findById(missionId)
            .orElseThrow(() -> new IllegalArgumentException("Mini mission not found"));
    }

    private MiniMissionResponse toResponse(MiniMission mission) {
        MiniMissionResponse response = new MiniMissionResponse();
        response.setTitle(mission.getTitle());
        response.setDescription(mission.getDescription());
        response.setRewardValue(mission.getRewardValue());
        return response;
    }

    private MiniMissionResponse fallbackResponse(Long missionId, String statusHint, Integer rewardValue) {
        MiniMissionResponse response = new MiniMissionResponse();
        response.setTitle("任务 #" + missionId);
        response.setDescription("该微任务记录仍然存在，原任务配置已不存在。" + statusHint);
        response.setRewardValue(rewardValue != null && rewardValue > 0 ? rewardValue : 1);
        return response;
    }

    public MiniMissionResponse getMiniMissionById(Long missionId) {
        return toResponse(getRequiredMission(missionId));
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

    public List<MiniMissionAdminResponse> getActiveMiniMissions() {
        return miniMissionRepository.findByIsActiveTrueOrderByIdAsc().stream().map(mission -> {
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

    public MiniMissionResponse startMiniMission(Long missionId, Long userId) {
        if (miniMissionLogRepository.existsByStatusAndUserId(MiniMissionLog.MiniMissionStatus.IN_PROGRESS, userId)) {
            throw new IllegalStateException("User already has an active mini mission");
        }
        MiniMission mission = getRequiredMission(missionId);
        MiniMissionLog log = new MiniMissionLog();
        log.setUserId(userId);
        log.setMiniMissionId(missionId);
        log.setStatus(MiniMissionLog.MiniMissionStatus.IN_PROGRESS);
        log.setCreatedAt(java.time.LocalDateTime.now());
        miniMissionLogRepository.save(log);
        return toResponse(mission);
    }

    public MiniMissionResponse abortMiniMission(Long missionId, Long userId) {
        List<MiniMissionLog> activeMissions = miniMissionLogRepository.findByUserIdAndStatus(userId, MiniMissionLog.MiniMissionStatus.IN_PROGRESS);
        if (activeMissions.isEmpty()) {
            throw new IllegalStateException("User does not have an active mini mission to abort");
        }

        MiniMissionLog targetLog = activeMissions.stream()
            .filter(log -> log.getMiniMissionId().equals(missionId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Active mini mission does not match the requested mission"));

        targetLog.setStatus(MiniMissionLog.MiniMissionStatus.FAILED);
        targetLog.setUpdatedAt(java.time.LocalDateTime.now());
        miniMissionLogRepository.save(targetLog);

        return miniMissionRepository.findById(missionId)
            .map(this::toResponse)
            .orElseGet(() -> fallbackResponse(missionId, "当前日志已标记为放弃。", targetLog.getEarnedValue()));
    }

    public MiniMissionResponse completeMiniMission(Long missionId, Long userId) {
        List<MiniMissionLog> activeMissions = miniMissionLogRepository.findByUserIdAndStatus(userId, MiniMissionLog.MiniMissionStatus.IN_PROGRESS);
        if (activeMissions.isEmpty()) {
            throw new IllegalStateException("User does not have an active mini mission to complete");
        }

        MiniMissionLog targetLog = activeMissions.stream()
            .filter(log -> log.getMiniMissionId().equals(missionId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Active mini mission does not match the requested mission"));

        MiniMission mission = miniMissionRepository.findById(missionId).orElse(null);
        int rewardValue = mission != null ? mission.getRewardValue() : (targetLog.getEarnedValue() > 0 ? targetLog.getEarnedValue() : 1);

        targetLog.setStatus(MiniMissionLog.MiniMissionStatus.COMPLETED);
        targetLog.setEarnedValue(rewardValue);
        targetLog.setUpdatedAt(java.time.LocalDateTime.now());
        miniMissionLogRepository.save(targetLog);

        if (mission != null) {
            return toResponse(mission);
        }
        return fallbackResponse(missionId, "当前日志已标记为完成。", rewardValue);
    }
}
