package com.lingobuddy.controller;

import com.lingobuddy.dto.LoginRequest;
import com.lingobuddy.dto.UserInfo;
import com.lingobuddy.entity.LevelConfig;
import com.lingobuddy.entity.User;
import com.lingobuddy.repository.DailyCheckinRepository;
import com.lingobuddy.repository.TaskProgressRepository;
import com.lingobuddy.repository.UserRepository;
import com.lingobuddy.service.CheckinService;
import com.lingobuddy.service.LevelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LevelService levelService;
    private final CheckinService checkinService;
    private final DailyCheckinRepository checkinRepository;
    private final TaskProgressRepository progressRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          LevelService levelService,
                          CheckinService checkinService,
                          DailyCheckinRepository checkinRepository,
                          TaskProgressRepository progressRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.levelService = levelService;
        this.checkinService = checkinService;
        this.checkinRepository = checkinRepository;
        this.progressRepository = progressRepository;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        return ResponseEntity.ok(buildUserInfo(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        return ResponseEntity.ok(buildUserInfo(user));
    }

    private UserInfo buildUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setTotalXp(user.getTotalXp());
        info.setCoins(user.getCoins());
        info.setCurrentLevel(user.getCurrentLevel());

        LevelConfig config = levelService.getLevelConfig(user.getCurrentLevel());
        info.setLevelTitle(config != null ? config.getTitle() : "Level " + user.getCurrentLevel());
        info.setNextLevelXp(levelService.getNextLevelXp(user.getCurrentLevel()));
        info.setStreak(checkinService.getCurrentStreak(user.getId()));
        info.setTotalCheckins(checkinRepository.countByUserId(user.getId()));
        info.setTotalTasksCompleted(progressRepository.countByUserIdAndCompletedTrue(user.getId()));
        return info;
    }
}
