package com.lingobuddy.controller;

import com.lingobuddy.dto.AchievementDto;
import com.lingobuddy.entity.User;
import com.lingobuddy.repository.UserRepository;
import com.lingobuddy.service.AchievementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final UserRepository userRepository;

    public AchievementController(AchievementService achievementService, UserRepository userRepository) {
        this.achievementService = achievementService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<AchievementDto>> list() {
        User user = getCurrentUser();
        return ResponseEntity.ok(achievementService.getAllForUser(user.getId()));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }
}
