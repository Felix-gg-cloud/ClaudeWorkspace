package com.ll.ai.service;

import com.ll.ai.entity.ChatMessage;
import com.ll.ai.entity.ChatSession;
import com.ll.ai.entity.StudentProfile;
import com.ll.ai.prompt.TeacherPrompts;
import com.ll.ai.repository.ChatMessageRepository;
import com.ll.ai.repository.ChatSessionRepository;
import com.ll.ai.repository.StudentProfileRepository;
import com.ll.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 入学评估服务
 * 流程：欢迎对话 → 收集自述 → 诊断测试 → 生成画像
 */
@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final StudentProfileRepository profileRepo;
    private final TeacherChatService chatService;
    private final RateLimiter rateLimiter;

    public AssessmentService(ChatModel chatModel,
                             ChatSessionRepository sessionRepo,
                             ChatMessageRepository messageRepo,
                             StudentProfileRepository profileRepo,
                             TeacherChatService chatService,
                             RateLimiter rateLimiter) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.profileRepo = profileRepo;
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 开始入学评估 — 创建评估会话，AI 发出欢迎消息
     */
    @Transactional
    public Map<String, Object> startAssessment(Long userId, String grade) {
        rateLimiter.check(userId);

        // 检查是否已有评估
        if (profileRepo.existsByUserId(userId)) {
            throw new BizException(400, "已完成入学评估");
        }

        // 创建评估会话
        ChatSession session = chatService.createSession(userId, "assessment", "入学评估");

        // AI 发出欢迎消息
        String welcomeReply = callAi(TeacherPrompts.ASSESSMENT_WELCOME, Collections.emptyList());
        chatService.saveMessage(session.getId(), "assistant", welcomeReply, "text", null);

        rateLimiter.record(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("reply", welcomeReply);
        result.put("phase", "welcome");
        return result;
    }

    /**
     * 评估对话中发送消息 — 根据阶段自动判断下一步
     */
    @Transactional
    public Map<String, Object> chat(Long userId, Long sessionId, String userMessage, String grade) {
        rateLimiter.check(userId);

        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BizException(404, "会话不存在"));
        if (!session.getUserId().equals(userId) || !"assessment".equals(session.getType())) {
            throw new BizException(403, "无权访问");
        }

        // 保存用户消息
        chatService.saveMessage(sessionId, "user", userMessage, "text", null);

        // 获取历史消息数，判断评估阶段
        long msgCount = messageRepo.countBySessionId(sessionId);
        List<Message> history = buildHistory(sessionId);

        String phase;
        String aiReply;

        if (msgCount <= 6) {
            // 欢迎阶段 — 继续对话了解学生
            aiReply = callAi(TeacherPrompts.ASSESSMENT_WELCOME, history);
            phase = "welcome";
        } else if (msgCount <= 8) {
            // 收集到够信息 — 出第一道诊断题
            String selfDesc = extractSelfDescription(sessionId);
            String quizPrompt = TeacherPrompts.assessmentQuiz(grade, selfDesc);
            aiReply = callAi(quizPrompt, Collections.emptyList());
            phase = "quiz";
        } else if (msgCount <= 14) {
            // 继续出题（根据之前的答题情况）
            String selfDesc = extractSelfDescription(sessionId);
            String quizPrompt = TeacherPrompts.assessmentQuiz(grade, selfDesc);
            // 加入之前的题目和回答作为上下文
            aiReply = callAi(quizPrompt, getQuizHistory(sessionId));
            phase = "quiz";
        } else {
            // 评估完成 — 分析结果生成画像
            phase = "complete";
            aiReply = completeAssessment(userId, sessionId, grade);
        }

        // 保存 AI 回复 — 解析 [QUIZ]...[/QUIZ] 标签
        String msgType = "text";
        String content = aiReply;
        String metadata = null;

        if ("quiz".equals(phase) && aiReply != null) {
            int quizStart = aiReply.indexOf("[QUIZ]");
            int quizEnd = aiReply.indexOf("[/QUIZ]");
            if (quizStart >= 0 && quizEnd > quizStart) {
                metadata = aiReply.substring(quizStart + 6, quizEnd).trim();
                content = aiReply.substring(0, quizStart).trim();
                if (content.isEmpty()) {
                    content = "来，试试这道题～ 😊";
                }
                msgType = "quiz";
            } else {
                // AI 可能直接返回了 JSON（不带标签的情况）
                String trimmed = aiReply.trim();
                if (trimmed.startsWith("{") && trimmed.contains("\"stem\"")) {
                    metadata = trimmed;
                    content = "来，试试这道题～ 😊";
                    msgType = "quiz";
                }
            }
        }

        chatService.saveMessage(sessionId, "assistant", content, msgType, metadata);

        rateLimiter.record(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("reply", content);
        result.put("phase", phase);
        result.put("msgType", msgType);
        if (metadata != null) {
            result.put("quizData", metadata);
        }
        return result;
    }

    /**
     * 检查用户是否已完成入学评估
     */
    public boolean hasProfile(Long userId) {
        return profileRepo.existsByUserId(userId);
    }

    /**
     * 获取用户的学生画像
     */
    public Map<String, Object> getProfile(Long userId) {
        StudentProfile p = profileRepo.findByUserId(userId).orElse(null);
        if (p == null) return null;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("vocabularyLevel", p.getVocabularyLevel());
        map.put("grammarLevel", p.getGrammarLevel());
        map.put("listeningLevel", p.getListeningLevel());
        map.put("interests", p.getInterests());
        map.put("weakPoints", p.getWeakPoints());
        map.put("strongPoints", p.getStrongPoints());
        map.put("learningStyle", p.getLearningStyle());
        map.put("aiAssessment", p.getAiAssessment());
        map.put("assessedAt", p.getAssessedAt());
        return map;
    }

    // ========== 内部方法 ==========

    /**
     * 从对话历史中提取学生的自我描述
     */
    private String extractSelfDescription(Long sessionId) {
        List<ChatMessage> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .map(ChatMessage::getContent)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 获取评估中的测试题历史
     */
    private List<Message> getQuizHistory(Long sessionId) {
        List<ChatMessage> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .filter(m -> "quiz".equals(m.getMsgType()) || "user".equals(m.getRole()))
                .map(m -> "user".equals(m.getRole())
                        ? (Message) new UserMessage(m.getContent())
                        : (Message) new AssistantMessage(m.getContent()))
                .collect(Collectors.toList());
    }

    /**
     * 完成评估 — 分析结果 + 生成画像 + 发布结果
     */
    private String completeAssessment(Long userId, Long sessionId, String grade) {
        String selfDesc = extractSelfDescription(sessionId);
        String quizResults = buildQuizResultSummary(sessionId);

        // 1. 让 AI 分析并生成画像 JSON
        String analyzePrompt = TeacherPrompts.assessmentAnalyze(grade, selfDesc, quizResults);
        String analysisJson = callAi(analyzePrompt, Collections.emptyList());

        // 2. 解析并保存画像
        try {
            saveStudentProfile(userId, selfDesc, analysisJson);
        } catch (Exception e) {
            log.error("解析学生画像失败: {}", analysisJson, e);
            // 即使解析失败，也创建一个基础画像
            StudentProfile profile = profileRepo.findByUserId(userId).orElse(new StudentProfile());
            profile.setUserId(userId);
            profile.setSelfDescription(selfDesc);
            profile.setAiAssessment(analysisJson);
            profile.setAssessedAt(LocalDateTime.now());
            // 兜底 levelCode：从 grade 推导
            String fallbackLevel = (grade != null && grade.matches("L\\d{1,2}")) ? grade : "L7";
            profile.setLevelCode(fallbackLevel);
            profileRepo.save(profile);
        }

        // 3. 关闭评估会话
        ChatSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session != null) {
            session.setStatus("closed");
            sessionRepo.save(session);
        }

        // 4. 生成友好的结果发布消息
        StudentProfile saved = profileRepo.findByUserId(userId).orElse(null);
        String assessment = saved != null && saved.getAiAssessment() != null
                ? saved.getAiAssessment() : "评估完成";
        String resultPrompt = TeacherPrompts.assessmentResult(grade, assessment);
        return callAi(resultPrompt, Collections.emptyList());
    }

    /**
     * 构建测试题结果摘要
     */
    private String buildQuizResultSummary(Long sessionId) {
        List<ChatMessage> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        StringBuilder sb = new StringBuilder();
        String lastQuiz = null;

        for (ChatMessage m : messages) {
            if ("quiz".equals(m.getMsgType())) {
                lastQuiz = m.getContent();
            } else if ("user".equals(m.getRole()) && lastQuiz != null) {
                sb.append("题目：").append(lastQuiz).append("\n");
                sb.append("学生回答：").append(m.getContent()).append("\n\n");
                lastQuiz = null;
            }
        }
        return sb.toString();
    }

    /**
     * 解析 AI 输出的 JSON 并保存学生画像
     */
    private void saveStudentProfile(Long userId, String selfDesc, String json) {
        // 清理 markdown 代码围栏
        String cleaned = json.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").trim();
        }

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> data = null;
        try {
            data = mapper.readValue(cleaned, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败: " + cleaned, e);
        }

        StudentProfile profile = profileRepo.findByUserId(userId).orElse(new StudentProfile());
        profile.setUserId(userId);
        profile.setSelfDescription(selfDesc);
        profile.setVocabularyLevel((String) data.get("vocabularyLevel"));
        profile.setGrammarLevel((String) data.get("grammarLevel"));
        if (data.get("interests") != null) {
            try { profile.setInterests(mapper.writeValueAsString(data.get("interests"))); } catch (Exception ignored) {}
        }
        if (data.get("weakPoints") != null) {
            try { profile.setWeakPoints(mapper.writeValueAsString(data.get("weakPoints"))); } catch (Exception ignored) {}
        }
        if (data.get("strongPoints") != null) {
            try { profile.setStrongPoints(mapper.writeValueAsString(data.get("strongPoints"))); } catch (Exception ignored) {}
        }
        profile.setLearningStyle((String) data.get("learningStyle"));
        profile.setAiAssessment((String) data.get("aiAssessment"));
        profile.setAssessedAt(LocalDateTime.now());

        // 设置 levelCode — 优先使用 AI 输出，兜底从 grade 推导
        String levelCode = (String) data.get("levelCode");
        if (levelCode == null || !levelCode.matches("L\\d{1,2}")) {
            levelCode = "L7"; // 默认七年级
        }
        profile.setLevelCode(levelCode);

        profileRepo.save(profile);
        log.info("学生画像已保存: userId={}, levelCode={}, vocabLevel={}", userId, profile.getLevelCode(), profile.getVocabularyLevel());
    }

    private List<Message> buildHistory(Long sessionId) {
        List<ChatMessage> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .filter(m -> !"system".equals(m.getRole()))
                .map(m -> "user".equals(m.getRole())
                        ? (Message) new UserMessage(m.getContent())
                        : (Message) new AssistantMessage(m.getContent()))
                .collect(Collectors.toList());
    }

    private String callAi(String systemPrompt, List<Message> history) {
        try {
            List<Message> allMessages = new ArrayList<>();
            allMessages.add(new SystemMessage(systemPrompt));
            allMessages.addAll(history);

            Prompt prompt = new Prompt(allMessages);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("AI 评估调用失败", e);
            return "抱歉，Lily 老师暂时遇到了一点小问题，请稍后再试哦～ 🌸";
        }
    }
}
