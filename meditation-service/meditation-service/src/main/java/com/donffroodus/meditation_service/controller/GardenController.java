package com.donffroodus.meditation_service.controller;

import com.donffroodus.meditation_service.dto.GardenDataResponse;
import com.donffroodus.meditation_service.dto.GardenItemDTO;
import com.donffroodus.meditation_service.service.GardenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/garden")
public class GardenController {

    @Autowired
    private GardenService gardenService;

    private Long resolveUserId(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(xUserId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<GardenDataResponse> getMyGarden(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        Long userId = resolveUserId(xUserId);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(gardenService.getUserGardenData(userId));
    }

    @PostMapping("/me/reward")
    public ResponseEntity<GardenItemDTO> rewardItem(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        Long userId = resolveUserId(xUserId);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(gardenService.rewardRandomItem(userId));
    }

    @PostMapping("/me/unlock-plant/{plantId}")
    public ResponseEntity<?> unlockPlant(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable Integer plantId) {
        Long userId = resolveUserId(xUserId);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        boolean success = gardenService.unlockPlant(userId, plantId);
        if (success) {
            return ResponseEntity.ok("Plant unlocked successfully");
        } else {
            return ResponseEntity.badRequest().body("Plant already unlocked or invalid");
        }
    }
}
