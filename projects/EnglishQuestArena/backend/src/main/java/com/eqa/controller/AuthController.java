package com.eqa.controller;

import com.eqa.entity.User;
import com.eqa.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request) {
        try {
            User user = authService.register(req.username(), req.password(), req.displayName());
            // 注册后自动登录
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return ResponseEntity.ok(toUserInfo(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        // 创建新 session
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        User user = authService.findByUsername(req.username());
        return ResponseEntity.ok(toUserInfo(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        User user = authService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
        }
        return ResponseEntity.ok(toUserInfo(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileRequest req, Authentication auth) {
        User user = authService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        if (req.displayName() != null && !req.displayName().isBlank()) {
            user.setDisplayName(req.displayName().trim());
        }
        if (req.avatar() != null && !req.avatar().isBlank()) {
            user.setAvatar(req.avatar().trim());
        }
        if (req.cefrLevel() != null) {
            user.setCefrLevel(req.cefrLevel());
        }
        if (req.ttsVoice() != null) {
            user.setTtsVoice(req.ttsVoice());
        }
        if (user.isFirstLogin()) {
            user.setFirstLogin(false);
        }
        user.setUpdatedAt(java.time.LocalDateTime.now());
        authService.saveUser(user);
        return ResponseEntity.ok(toUserInfo(user));
    }

    private Map<String, Object> toUserInfo(User user) {
        return Map.ofEntries(
                Map.entry("id", user.getId()),
                Map.entry("username", user.getUsername()),
                Map.entry("displayName", user.getDisplayName()),
                Map.entry("avatar", user.getAvatar()),
                Map.entry("cefrLevel", user.getCefrLevel()),
                Map.entry("currentLevel", user.getCurrentLevel()),
                Map.entry("totalXp", user.getTotalXp()),
                Map.entry("xpToNextLevel", user.getXpToNextLevel()),
                Map.entry("coins", user.getCoins()),
                Map.entry("skillPoints", user.getSkillPoints()),
                Map.entry("streak", user.getStreak()),
                Map.entry("totalCheckins", user.getTotalCheckins()),
                Map.entry("firstLogin", user.isFirstLogin())
        );
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 4, max = 100) String password,
            String displayName
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record ProfileRequest(
            String displayName,
            String avatar,
            String cefrLevel,
            String ttsVoice
    ) {}
}
