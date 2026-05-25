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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.meditation_service.dto.MiniMissionIdRequest;
import com.donffroodus.meditation_service.dto.MiniMissionRequest;
import com.donffroodus.meditation_service.dto.MiniMissionResponse;
import com.donffroodus.meditation_service.service.MiniMissionService;


@RestController
@RequestMapping("/api/mini-missions")
public class MiniMissionController {
    @Autowired
    private MiniMissionService miniMissionService;

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

    private String getUserRoleFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .findFirst()
                .orElse(null);
        }
        return null; 
    }

    @GetMapping("/{mission_id}")
    public ResponseEntity<?> getMethodName(@PathVariable Long mission_id) {
        if ("ROLE_USER".equals(getUserRoleFromSecurityContext())) {
            return ResponseEntity.ok(miniMissionService.getMiniMissionById(mission_id));
        }
        if ("ROLE_ADMIN".equals(getUserRoleFromSecurityContext())) {
            return ResponseEntity.ok(miniMissionService.getMiniMissionByIdAdmin(mission_id));
        }
        throw new IllegalStateException("User not authorized to access this resource");
    }

    @PostMapping("/start")
    public ResponseEntity<MiniMissionResponse> startMiniMission(@RequestBody MiniMissionIdRequest request) {
        Long missionId = request.getMissionId();
        Long userId = getUserIdFromSecurityContext();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        miniMissionService.startMiniMission(missionId, userId);
        return ResponseEntity.ok(miniMissionService.getMiniMissionById(missionId));
    }

    @PostMapping("/abort")
    public ResponseEntity<MiniMissionResponse> abortMiniMission(@RequestBody MiniMissionIdRequest request) {
        Long missionId = request.getMissionId();
        Long userId = getUserIdFromSecurityContext();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        miniMissionService.AbortMiniMission(missionId, userId);
        return ResponseEntity.ok(miniMissionService.getMiniMissionById(missionId));
    }

    @PostMapping("/complete")
    public ResponseEntity<MiniMissionResponse> completeMiniMission(@RequestBody MiniMissionIdRequest request) {
        Long missionId = request.getMissionId();
        Long userId = getUserIdFromSecurityContext();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        miniMissionService.completeMiniMission(missionId, userId);
        return ResponseEntity.ok(miniMissionService.getMiniMissionById(missionId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    public ResponseEntity<MiniMissionResponse> addNewMiniMission(@RequestBody MiniMissionRequest request) {
        miniMissionService.addNewMiniMission(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/delete")
    public ResponseEntity<Void> deleteMiniMission(@RequestBody MiniMissionIdRequest request) {
        Long missionId = request.getMissionId();
        miniMissionService.deleteMiniMission(missionId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllMiniMissions() {
        return ResponseEntity.ok(miniMissionService.getAllMiniMissions());
    }
}
