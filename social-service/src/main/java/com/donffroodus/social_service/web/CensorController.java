package com.donffroodus.social_service.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donffroodus.social_service.dto.SingleWordRequestDto;
import com.donffroodus.social_service.entity.CensorWord;
import com.donffroodus.social_service.service.CensorService;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/api/censor")
public class CensorController {
    @Autowired
    private CensorService censorService;

    @GetMapping("/all-words")
    public List<CensorWord> getAllCensorWords(@RequestHeader("X-User-Role") String userRole, @RequestParam(required = false) int active) {
        if (!"ROLE_ADMIN".equalsIgnoreCase(userRole)) {
            throw new RuntimeException("Unauthorized");
        }
        return censorService.getAllCensorWords(active);
    }

    @PostMapping("/add-word")
    public ResponseEntity<?> addCensorWord(@RequestHeader("X-User-Role") String userRole, SingleWordRequestDto request) {
        if (!"ROLE_ADMIN".equalsIgnoreCase(userRole)) {
            throw new RuntimeException("Unauthorized");
        }
        // Logic to add the word to the database
        censorService.addCensorWord(request.getWord());
        return ResponseEntity.ok(censorService.getCensorWordByWord(request.getWord()));
    }
    
    @PostMapping("/set-word-activity")
    public ResponseEntity<?> setWordActivity(@RequestHeader("X-User-Role") String userRole, SingleWordRequestDto request, @RequestParam boolean active) {
        if (!"ROLE_ADMIN".equalsIgnoreCase(userRole)) {
            throw new RuntimeException("Unauthorized");
        }
        censorService.setWordActivity(request.getWord(), active);
        return ResponseEntity.ok(censorService.getCensorWordByWord(request.getWord()));
    }

    @DeleteMapping("/delete-word")
    public ResponseEntity<?> deleteCensorWord(@RequestHeader("X-User-Role") String userRole, @RequestParam String word) {
        if (!"ROLE_ADMIN".equalsIgnoreCase(userRole)) {
            throw new RuntimeException("Unauthorized");
        }
        censorService.deleteCensorWord(word);
        return ResponseEntity.ok("Censor word deleted successfully");
    }
}
