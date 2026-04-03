package com.ll.ai.controller;

import com.ll.ai.service.KnowledgeGenerateService;
import com.ll.ai.service.QuestionGenerateService;
import com.ll.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/generate")
public class GenerateController {

    private final QuestionGenerateService generateService;
    private final KnowledgeGenerateService knowledgeGenerateService;

    public GenerateController(QuestionGenerateService generateService,
                              KnowledgeGenerateService knowledgeGenerateService) {
        this.generateService = generateService;
        this.knowledgeGenerateService = knowledgeGenerateService;
    }

    /**
     * 为单个知识点 AI 出题
     * POST /api/ai/generate/question
     * {"kpId": 1, "questionType": "en2zh_choice", "grade": "初中"}
     */
    @PostMapping("/question")
    public ApiResponse<Map<String, Object>> generateSingle(@RequestBody Map<String, Object> request) {
        Long kpId = ((Number) request.get("kpId")).longValue();
        String questionType = (String) request.getOrDefault("questionType", "en2zh_choice");
        String grade = (String) request.getOrDefault("grade", "初中");

        Map<String, Object> result = generateService.generateSingle(kpId, questionType, grade);
        return ApiResponse.ok(result);
    }

    /**
     * 批量 AI 出题
     * POST /api/ai/generate/batch
     * {"bankId": 1, "questionTypes": ["en2zh_choice","fill_blank"], "count": 5, "grade": "初中"}
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/batch")
    public ApiResponse<Map<String, Object>> generateBatch(@RequestBody Map<String, Object> request) {
        Long bankId = ((Number) request.get("bankId")).longValue();
        List<String> types = (List<String>) request.getOrDefault("questionTypes",
                List.of("en2zh_choice", "zh2en_choice", "fill_blank", "translate"));
        int count = request.get("count") != null ? ((Number) request.get("count")).intValue() : 5;
        String grade = (String) request.getOrDefault("grade", "初中");

        Map<String, Object> result = generateService.generateBatch(bankId, types, count, grade);
        return ApiResponse.ok(result);
    }

    /**
     * AI 生成知识库单元内容
     * POST /api/ai/generate/unit-content
     * {"levelCode":"L1","levelName":"小学三年级","levelDesc":"...","topic":"colors","unitName":"Unit 2: Colors","count":15}
     */
    @PostMapping("/unit-content")
    public ApiResponse<List<Map<String, Object>>> generateUnitContent(@RequestBody Map<String, Object> request) {
        String levelCode = (String) request.get("levelCode");
        String levelName = (String) request.get("levelName");
        String levelDesc = (String) request.get("levelDesc");
        String topic = (String) request.get("topic");
        String unitName = (String) request.get("unitName");
        int count = request.get("count") != null ? ((Number) request.get("count")).intValue() : 15;

        List<Map<String, Object>> result = knowledgeGenerateService.generateUnitContent(
                levelCode, levelName, levelDesc, topic, unitName, count);
        return ApiResponse.ok(result);
    }
}
