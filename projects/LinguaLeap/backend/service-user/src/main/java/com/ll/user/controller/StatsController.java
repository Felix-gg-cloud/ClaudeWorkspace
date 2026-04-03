package com.ll.user.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.user.service.StatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/today")
    public ApiResponse<Map<String, Object>> getToday(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.ok(statsService.getToday(userId));
    }

    @GetMapping("/range")
    public ApiResponse<List<Map<String, Object>>> getRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.ok(statsService.getRange(userId, from, to));
    }

    @PostMapping("/record")
    public ApiResponse<Void> record(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Integer> body) {
        statsService.record(
                userId,
                body.getOrDefault("correctCount", 0),
                body.getOrDefault("wrongCount", 0),
                body.getOrDefault("wordsLearned", 0),
                body.getOrDefault("studyMinutes", 0));
        return ApiResponse.ok(null);
    }

    @GetMapping("/streak")
    public ApiResponse<Map<String, Object>> getStreak(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.ok(statsService.getStreak(userId));
    }
}
