package com.ll.ai.controller;

import com.ll.ai.service.ContentExtractService;
import com.ll.common.dto.ApiResponse;
import com.ll.common.util.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/extract")
public class ContentExtractController {

    private final ContentExtractService extractService;

    public ContentExtractController(ContentExtractService extractService) {
        this.extractService = extractService;
    }

    /**
     * AI 提取 + 分类文本内容
     */
    @PostMapping("/content")
    public ApiResponse<Map<String, Object>> extractContent(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String grade = body.getOrDefault("grade", "junior");
        String sourceType = body.getOrDefault("sourceType", "text");
        String userNote = body.get("userNote");
        return ApiResponse.ok(extractService.extractAndClassify(text, grade, sourceType, userNote));
    }

    /**
     * AI 生成出题策略
     */
    @PostMapping("/strategy")
    public ApiResponse<Map<String, Object>> generateStrategy(@RequestBody Map<String, Object> body) {
        String grade = (String) body.getOrDefault("grade", "junior");
        String userNote = (String) body.get("userNote");
        String summary = (String) body.get("summary");

        @SuppressWarnings("unchecked")
        Map<String, Integer> categoryCounts = (Map<String, Integer>) body.get("categoryCounts");
        if (categoryCounts == null) {
            categoryCounts = Map.of();
        }

        return ApiResponse.ok(extractService.generateStrategy(grade, userNote, categoryCounts, summary));
    }
}
