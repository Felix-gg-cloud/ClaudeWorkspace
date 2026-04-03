package com.ll.content.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.content.entity.Question;
import com.ll.content.service.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/questions/generate")
    @SuppressWarnings("unchecked")
    public ApiResponse<List<Question>> generate(@RequestBody Map<String, Object> body) {
        Long bankId = ((Number) body.get("bankId")).longValue();
        List<Long> kpIds = null;
        if (body.get("kpIds") != null) {
            kpIds = ((List<Number>) body.get("kpIds")).stream().map(Number::longValue).toList();
        }
        List<String> types = (List<String>) body.get("types");
        int count = body.get("count") != null ? ((Number) body.get("count")).intValue() : 10;

        return ApiResponse.ok(questionService.generate(bankId, kpIds, types, count));
    }

    @GetMapping("/banks/{bankId}/questions")
    public ApiResponse<Page<Question>> listByBank(
            @PathVariable Long bankId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(questionService.listByBank(bankId, page, size));
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<Question> getById(@PathVariable Long id) {
        return ApiResponse.ok(questionService.getById(id));
    }

    /**
     * AI 生成题目保存接口（供 service-ai 内部调用）
     */
    @PostMapping("/questions/ai-save")
    public ApiResponse<List<Question>> saveAiQuestions(@RequestBody List<Question> questions) {
        return ApiResponse.ok(questionService.saveAiQuestions(questions));
    }
}
