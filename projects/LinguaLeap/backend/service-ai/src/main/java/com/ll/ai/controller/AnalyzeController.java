package com.ll.ai.controller;

import com.ll.ai.service.KpAnalyzeService;
import com.ll.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/analyze")
public class AnalyzeController {

    private final KpAnalyzeService kpAnalyzeService;

    public AnalyzeController(KpAnalyzeService kpAnalyzeService) {
        this.kpAnalyzeService = kpAnalyzeService;
    }

    /**
     * 从文本提取知识点
     * POST /api/ai/analyze/text
     * {"text": "...", "grade": "初中", "bankId": 1}
     */
    @PostMapping("/text")
    public ApiResponse<Map<String, Object>> analyzeText(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        String grade = (String) request.getOrDefault("grade", "初中");
        Long bankId = request.get("bankId") != null ? ((Number) request.get("bankId")).longValue() : null;

        Map<String, Object> result = kpAnalyzeService.analyzeText(text, grade, bankId);
        return ApiResponse.ok(result);
    }
}
