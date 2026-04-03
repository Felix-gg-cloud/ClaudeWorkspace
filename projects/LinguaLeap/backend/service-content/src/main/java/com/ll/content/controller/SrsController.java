package com.ll.content.controller;

import com.ll.common.util.UserContext;
import com.ll.common.dto.ApiResponse;
import com.ll.content.service.SrsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content/srs")
public class SrsController {

    private final SrsService srsService;

    public SrsController(SrsService srsService) {
        this.srsService = srsService;
    }

    @GetMapping("/due")
    public ApiResponse<List<Map<String, Object>>> getDueCards() {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(srsService.getDueCards(userId));
    }

    @PostMapping("/review")
    public ApiResponse<Map<String, Object>> review(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Long kpId = Long.valueOf(body.get("kpId").toString());
        boolean correct = Boolean.parseBoolean(body.get("correct").toString());
        return ApiResponse.ok(srsService.review(userId, kpId, correct));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(srsService.getStats(userId));
    }

    @GetMapping("/cards")
    public ApiResponse<List<Map<String, Object>>> getAllCards() {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(srsService.getAllCards(userId));
    }
}
