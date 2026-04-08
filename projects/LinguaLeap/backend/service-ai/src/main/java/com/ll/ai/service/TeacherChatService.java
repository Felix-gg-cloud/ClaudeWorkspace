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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 老师多轮对话服务
 * 核心能力：管理对话上下文、调用 GPT-4o、保存消息历史
 */
@Service
public class TeacherChatService {

    private static final Logger log = LoggerFactory.getLogger(TeacherChatService.class);
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final StudentProfileRepository profileRepo;
    private final RateLimiter rateLimiter;
    private final OrchestratorService orchestratorService;

    public TeacherChatService(ChatModel chatModel,
                              ChatSessionRepository sessionRepo,
                              ChatMessageRepository messageRepo,
                              StudentProfileRepository profileRepo,
                              RateLimiter rateLimiter,
                              OrchestratorService orchestratorService) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.profileRepo = profileRepo;
        this.rateLimiter = rateLimiter;
        this.orchestratorService = orchestratorService;
    }

    /**
     * 获取或创建用户的活跃聊天会话
     */
    @Transactional
    public ChatSession getOrCreateSession(Long userId, String type) {
        return sessionRepo.findFirstByUserIdAndTypeAndStatusOrderByCreatedAtDesc(userId, type, "active")
                .orElseGet(() -> {
                    ChatSession session = new ChatSession();
                    session.setUserId(userId);
                    session.setType(type);
                    session.setTitle(type.equals("assessment") ? "入学评估" : "与 Lily 老师的对话");
                    return sessionRepo.save(session);
                });
    }

    /**
     * 发送消息并获取 AI 回复 — 核心多轮对话
     */
    @Transactional
    public Map<String, Object> sendMessage(Long userId, Long sessionId, String userMessage, String grade) {
        rateLimiter.check(userId);

        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BizException(404, "会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BizException(403, "无权访问该会话");
        }

        // 1. 保存用户消息
        saveMessage(sessionId, "user", userMessage, "text", null);

        // 2. 构建多轮上下文
        String systemPrompt = buildSystemPrompt(userId, grade, session.getType());
        List<Message> history = buildHistory(sessionId);

        // 3. 调用 AI
        String aiReply = callAiWithHistory(systemPrompt, history);

        // 4. 解析 [QUIZ]...[/QUIZ] 标签
        String content = aiReply;
        String metadata = null;
        String msgType = "text";

        if (aiReply != null) {
            int quizStart = aiReply.indexOf("[QUIZ]");
            int quizEnd = aiReply.indexOf("[/QUIZ]");
            if (quizStart >= 0 && quizEnd > quizStart) {
                metadata = aiReply.substring(quizStart + 6, quizEnd).trim();
                content = aiReply.substring(0, quizStart).trim();
                if (content.isEmpty()) content = "来做道题试试～";
                msgType = "quiz";
            }

            // Phase 5a: 解析 [FEEDBACK]...[/FEEDBACK] 标签，自动更新 Learner Model
            int fbStart = aiReply.indexOf("[FEEDBACK]");
            int fbEnd = aiReply.indexOf("[/FEEDBACK]");
            if (fbStart >= 0 && fbEnd > fbStart) {
                String fbJson = aiReply.substring(fbStart + 10, fbEnd).trim();
                parseFeedbackAndUpdate(userId, fbJson);
                // 从显示内容中移除 FEEDBACK 标签
                content = content.replace("[FEEDBACK]" + fbJson + "[/FEEDBACK]", "").trim();
            }
        }

        // 5. 保存 AI 回复
        saveMessage(sessionId, "assistant", content, msgType, metadata);

        // 6. 更新会话时间
        session.setUpdatedAt(null); // trigger @PreUpdate
        sessionRepo.save(session);

        rateLimiter.record(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("reply", content);
        result.put("role", "assistant");
        result.put("msgType", msgType);
        if (metadata != null) {
            result.put("quizData", metadata);
        }
        return result;
    }

    /**
     * 获取会话的消息历史
     */
    public List<Map<String, Object>> getHistory(Long userId, Long sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BizException(404, "会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BizException(403, "无权访问");
        }

        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .filter(m -> !"system".equals(m.getRole()))
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    map.put("msgType", m.getMsgType());
                    map.put("metadata", m.getMetadata());
                    map.put("createdAt", m.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的聊天会话列表
     */
    public List<Map<String, Object>> getSessions(Long userId) {
        return sessionRepo.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", s.getId());
                    map.put("type", s.getType());
                    map.put("title", s.getTitle());
                    map.put("status", s.getStatus());
                    map.put("createdAt", s.getCreatedAt());
                    map.put("updatedAt", s.getUpdatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建新的对话会话
     */
    @Transactional
    public ChatSession createSession(Long userId, String type, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setType(type);
        session.setTitle(title != null ? title : "与 Lily 老师的对话");
        return sessionRepo.save(session);
    }

    /**
     * 保存消息到数据库
     */
    public ChatMessage saveMessage(Long sessionId, String role, String content, String msgType, String metadata) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMsgType(msgType != null ? msgType : "text");
        msg.setMetadata(metadata);
        return messageRepo.save(msg);
    }

    // ========== 内部方法 ==========

    /**
     * 构建 System Prompt — 根据会话类型和学生画像
     * Phase 5a: 普通对话走编排引擎，获取带约束的 prompt
     */
    private String buildSystemPrompt(Long userId, String grade, String sessionType) {
        if ("assessment".equals(sessionType)) {
            return TeacherPrompts.ASSESSMENT_WELCOME;
        }

        // Phase 5a: 尝试通过编排引擎获取带约束的 prompt
        try {
            OrchestratorService.OrchestratedPrompt plan = orchestratorService.orchestrate(userId);
            if (!"assessment".equals(plan.phase()) && plan.systemPrompt() != null) {
                log.info("编排引擎: userId={}, phase={}, level={}", userId, plan.phase(), plan.levelCode());
                return plan.systemPrompt();
            }
        } catch (Exception e) {
            log.warn("编排引擎降级: {}", e.getMessage());
        }

        // 降级：使用旧的简单 prompt
        String profileSummary = getProfileSummary(userId);
        return TeacherPrompts.chatSystem(grade, profileSummary);
    }

    /**
     * 获取学生画像摘要（注入到 system prompt）
     */
    private String getProfileSummary(Long userId) {
        return profileRepo.findByUserId(userId)
                .map(p -> {
                    StringBuilder sb = new StringBuilder();
                    if (p.getVocabularyLevel() != null) sb.append("词汇水平：").append(p.getVocabularyLevel()).append("\n");
                    if (p.getGrammarLevel() != null) sb.append("语法水平：").append(p.getGrammarLevel()).append("\n");
                    if (p.getInterests() != null) sb.append("兴趣爱好：").append(p.getInterests()).append("\n");
                    if (p.getWeakPoints() != null) sb.append("薄弱环节：").append(p.getWeakPoints()).append("\n");
                    if (p.getStrongPoints() != null) sb.append("优势：").append(p.getStrongPoints()).append("\n");
                    if (p.getAiAssessment() != null) sb.append("综合评估：").append(p.getAiAssessment()).append("\n");
                    return sb.length() > 0 ? sb.toString() : "暂无画像信息，这是新学生。";
                })
                .orElse("暂无画像信息，这是新学生。");
    }

    /**
     * 构建多轮对话历史（最近 N 条）
     */
    private List<Message> buildHistory(Long sessionId) {
        List<ChatMessage> recent = messageRepo.findTop30BySessionIdOrderByCreatedAtDesc(sessionId);
        // 反转为时间正序，取最近 MAX_HISTORY_MESSAGES 条
        Collections.reverse(recent);
        if (recent.size() > MAX_HISTORY_MESSAGES) {
            recent = recent.subList(recent.size() - MAX_HISTORY_MESSAGES, recent.size());
        }

        return recent.stream()
                .map(m -> switch (m.getRole()) {
                    case "user" -> (Message) new UserMessage(m.getContent());
                    case "assistant" -> (Message) new AssistantMessage(m.getContent());
                    default -> (Message) new SystemMessage(m.getContent());
                })
                .collect(Collectors.toList());
    }

    /**
     * 带历史上下文的 AI 调用
     */
    private String callAiWithHistory(String systemPrompt, List<Message> history) {
        try {
            List<Message> allMessages = new ArrayList<>();
            allMessages.add(new SystemMessage(systemPrompt));
            allMessages.addAll(history);

            Prompt prompt = new Prompt(allMessages);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("AI 老师对话失败", e);
            return "抱歉，Lily 老师暂时遇到了一点小问题，请稍后再试哦～ 🌸";
        }
    }

    /**
     * Phase 5a: 解析 [FEEDBACK] JSON 并更新 Learner Model
     */
    private void parseFeedbackAndUpdate(Long userId, String fbJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode fb =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(fbJson);
            boolean correct = fb.path("correct").asBoolean(false);
            String kp = fb.path("kp").asText(null);
            orchestratorService.updateAfterAnswer(userId, correct, kp);
            log.debug("FEEDBACK 更新: userId={}, correct={}, kp={}", userId, correct, kp);
        } catch (Exception e) {
            log.warn("解析 FEEDBACK 失败: {}", e.getMessage());
        }
    }
}
