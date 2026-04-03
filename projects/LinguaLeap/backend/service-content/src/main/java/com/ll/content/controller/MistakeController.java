package com.ll.content.controller;

import com.ll.common.util.UserContext;
import com.ll.common.dto.ApiResponse;
import com.ll.content.entity.MistakeRecord;
import com.ll.content.service.MistakeService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content/mistakes")
public class MistakeController {

    private final MistakeService mistakeService;

    public MistakeController(MistakeService mistakeService) {
        this.mistakeService = mistakeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) Long bankId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Boolean reviewed,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(mistakeService.list(userId, bankId, questionType, reviewed, dateFrom, dateTo, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(mistakeService.getDetail(userId, id));
    }

    @PutMapping("/{id}/review")
    public ApiResponse<Void> markReviewed(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        mistakeService.markReviewed(userId, id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        mistakeService.delete(userId, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/practice")
    public ApiResponse<List<Long>> getMistakeQuestionIds(
            @RequestParam(defaultValue = "10") int count) {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(mistakeService.getMistakeQuestionIds(userId, count));
    }
}
