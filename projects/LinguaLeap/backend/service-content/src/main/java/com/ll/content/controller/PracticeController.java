package com.ll.content.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.common.util.UserContext;
import com.ll.content.service.PracticeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/content/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        Long bankId = ((Number) body.get("bankId")).longValue();
        String questionType = (String) body.get("questionType");
        String grade = (String) body.get("grade");
        int count = body.get("count") != null ? ((Number) body.get("count")).intValue() : 10;

        return ApiResponse.ok(practiceService.start(userId, bankId, questionType, grade, count));
    }

    @PostMapping("/start-by-unit")
    public ApiResponse<Map<String, Object>> startByUnit(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        Long unitId = ((Number) body.get("unitId")).longValue();
        String questionType = (String) body.get("questionType");
        String grade = (String) body.get("grade");
        int count = body.get("count") != null ? ((Number) body.get("count")).intValue() : 10;

        return ApiResponse.ok(practiceService.startByUnit(userId, unitId, questionType, grade, count));
    }

    @PostMapping("/start-by-study-set")
    public ApiResponse<Map<String, Object>> startByStudySet(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        Long studySetId = ((Number) body.get("studySetId")).longValue();
        String questionType = (String) body.get("questionType");
        String grade = (String) body.get("grade");
        int count = body.get("count") != null ? ((Number) body.get("count")).intValue() : 10;

        return ApiResponse.ok(practiceService.startByStudySet(userId, studySetId, questionType, grade, count));
    }

    @GetMapping("/{sessionId}/next")
    public ApiResponse<Map<String, Object>> next(@PathVariable Long sessionId) {
        return ApiResponse.ok(practiceService.next(sessionId));
    }

    @PostMapping("/{sessionId}/answer")
    public ApiResponse<Map<String, Object>> answer(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        Long questionId = ((Number) body.get("questionId")).longValue();
        String userAnswer = (String) body.get("answer");

        return ApiResponse.ok(practiceService.answer(sessionId, userId, questionId, userAnswer));
    }

    @PostMapping("/{sessionId}/finish")
    public ApiResponse<Map<String, Object>> finish(@PathVariable Long sessionId) {
        return ApiResponse.ok(practiceService.finish(sessionId));
    }

    @GetMapping("/{sessionId}/result")
    public ApiResponse<Map<String, Object>> result(@PathVariable Long sessionId) {
        return ApiResponse.ok(practiceService.result(sessionId));
    }

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        return userId != null ? userId : 0L;
    }
}
