package com.ll.user.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.user.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.register(
                body.get("username"),
                body.get("password"),
                body.get("displayName"),
                body.get("grade")
        ));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.login(
                body.get("username"),
                body.get("password")
        ));
    }
}
