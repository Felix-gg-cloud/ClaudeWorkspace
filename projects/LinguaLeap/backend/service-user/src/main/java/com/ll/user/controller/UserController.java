package com.ll.user.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.user.entity.User;
import com.ll.user.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getMe(@RequestHeader("X-User-Id") Long userId) {
        User user = authService.getUserById(userId);
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                "grade", user.getGrade()
        ));
    }

    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> updateMe(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        User user = authService.updateUser(userId, body.get("displayName"), body.get("grade"));
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                "grade", user.getGrade()
        ));
    }
}
