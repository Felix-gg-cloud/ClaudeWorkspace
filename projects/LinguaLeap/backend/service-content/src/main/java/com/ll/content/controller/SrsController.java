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

    /**
     * Phase 5a: 按知识点内容复习（AI 对话反馈联动，服务间调用）
     * 请求体：{ "userId": 1, "content": "apple", "correct": true }
     */
    @PostMapping("/review-by-content")
    public ApiResponse<Map<String, Object>> reviewByContent(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null
                ? Long.valueOf(body.get("userId").toString())
                : UserContext.getUserId();
        String content = (String) body.get("content");
        boolean correct = Boolean.parseBoolean(body.get("correct").toString());
        Map<String, Object> result = srsService.reviewByContent(userId, content, correct);
        return result != null ? ApiResponse.ok(result) : ApiResponse.ok(null);
    }
}
