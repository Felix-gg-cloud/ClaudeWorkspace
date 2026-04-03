package com.ll.ai.controller;

import com.ll.ai.entity.ChatSession;
import com.ll.ai.service.AssessmentService;
import com.ll.ai.service.TeacherChatService;
import com.ll.common.dto.ApiResponse;
import com.ll.common.exception.BizException;
import com.ll.common.util.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 老师对话 API
 */
@RestController
@RequestMapping("/api/ai/teacher")
public class TeacherController {

    private final TeacherChatService chatService;
    private final AssessmentService assessmentService;

    public TeacherController(TeacherChatService chatService, AssessmentService assessmentService) {
        this.chatService = chatService;
        this.assessmentService = assessmentService;
    }

    // ========== 对话 API ==========

    /**
     * 发送消息给 AI 老师
     */
    @PostMapping("/chat/send")
    public ApiResponse<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        Long sessionId = toLong(body.get("sessionId"));
        String message = (String) body.get("message");
        String grade = (String) body.get("grade");

        if (message == null || message.isBlank()) {
            throw new BizException(400, "消息不能为空");
        }

        return ApiResponse.ok(chatService.sendMessage(userId, sessionId, message, grade));
    }

    /**
     * 获取/创建活跃对话会话
     */
    @PostMapping("/chat/session")
    public ApiResponse<Map<String, Object>> getOrCreateSession(@RequestBody(required = false) Map<String, String> body) {
        Long userId = getUserId();
        String type = body != null && body.get("type") != null ? body.get("type") : "chat";
        ChatSession session = chatService.getOrCreateSession(userId, type);

        return ApiResponse.ok(Map.of(
                "id", session.getId(),
                "type", session.getType(),
                "title", session.getTitle() != null ? session.getTitle() : "",
                "status", session.getStatus()
        ));
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/chat/history/{sessionId}")
    public ApiResponse<List<Map<String, Object>>> getHistory(@PathVariable Long sessionId) {
        return ApiResponse.ok(chatService.getHistory(getUserId(), sessionId));
    }

    /**
     * 获取用户的所有会话列表
     */
    @GetMapping("/chat/sessions")
    public ApiResponse<List<Map<String, Object>>> getSessions() {
        return ApiResponse.ok(chatService.getSessions(getUserId()));
    }

    // ========== 入学评估 API ==========

    /**
     * 开始入学评估
     */
    @PostMapping("/assessment/start")
    public ApiResponse<Map<String, Object>> startAssessment(@RequestBody Map<String, String> body) {
        Long userId = getUserId();
        String grade = body.get("grade");
        return ApiResponse.ok(assessmentService.startAssessment(userId, grade));
    }

    /**
     * 评估对话中发消息
     */
    @PostMapping("/assessment/chat")
    public ApiResponse<Map<String, Object>> assessmentChat(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();
        Long sessionId = toLong(body.get("sessionId"));
        String message = (String) body.get("message");
        String grade = (String) body.get("grade");

        if (message == null || message.isBlank()) {
            throw new BizException(400, "消息不能为空");
        }

        return ApiResponse.ok(assessmentService.chat(userId, sessionId, message, grade));
    }

    /**
     * 检查是否已完成评估
     */
    @GetMapping("/assessment/status")
    public ApiResponse<Map<String, Object>> assessmentStatus() {
        Long userId = getUserId();
        boolean hasProfile = assessmentService.hasProfile(userId);
        Map<String, Object> result = Map.of("assessed", hasProfile);
        return ApiResponse.ok(result);
    }

    /**
     * 获取学生画像
     */
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile() {
        Map<String, Object> profile = assessmentService.getProfile(getUserId());
        if (profile == null) {
            throw new BizException(404, "尚未完成入学评估");
        }
        return ApiResponse.ok(profile);
    }

    // ========== 工具方法 ==========

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizException(401, "未登录");
        return userId;
    }

    private Long toLong(Object val) {
        if (val == null) throw new BizException(400, "缺少必要参数");
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
