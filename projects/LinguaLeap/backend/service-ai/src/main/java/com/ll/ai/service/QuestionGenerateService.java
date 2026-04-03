package com.ll.ai.service;

import com.ll.ai.client.ContentServiceClient;
import com.ll.ai.prompt.PromptTemplates;
import com.ll.ai.prompt.PromptTemplates.PromptPair;
import com.ll.ai.prompt.PromptTemplates.WordItem;
import com.ll.ai.validator.AiQuestionValidator;
import com.ll.common.exception.BizException;
import com.ll.common.util.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuestionGenerateService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerateService.class);
    private static final int MAX_RETRY = 1;

    private final AiService aiService;
    private final ContentServiceClient contentClient;
    private final AiQuestionValidator validator;
    private final CacheService cacheService;

    public QuestionGenerateService(AiService aiService, ContentServiceClient contentClient,
                                   AiQuestionValidator validator, CacheService cacheService) {
        this.aiService = aiService;
        this.contentClient = contentClient;
        this.validator = validator;
        this.cacheService = cacheService;
    }

    /**
     * 为单个知识点生成一道 AI 题目
     */
    public Map<String, Object> generateSingle(Long kpId, String questionType, String grade) {
        // 1. 获取知识点信息
        Map<String, Object> kp = contentClient.getKnowledgePoint(kpId);
        if (kp == null) {
            throw new BizException(404, "知识点不存在");
        }

        Long bankId = toLong(kp.get("bankId"));
        String word = (String) kp.get("content");
        String phonetic = (String) kp.get("phonetic");
        String meaning = (String) kp.get("meaningZh");
        String example = (String) kp.get("exampleSentence");
        Integer difficulty = kp.get("difficulty") != null ? ((Number) kp.get("difficulty")).intValue() : 1;

        // 2. 构造 Prompt
        PromptPair prompt = PromptTemplates.forType(questionType, grade, word, phonetic, meaning, example);

        // 2.5 检查缓存
        String cacheKey = kpId + ":" + questionType + ":" + grade;
        String cachedReply = cacheService.get(cacheKey, "generate_question");

        // 3. 调用 AI（带重试）或使用缓存
        Map<String, Object> validated = null;
        if (cachedReply != null) {
            validated = validator.validateSingle(cachedReply, questionType);
            if (validated != null) {
                log.info("AI 出题命中缓存: kpId={}, type={}", kpId, questionType);
            }
        }

        if (validated == null) {
            try {
                for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
                    String aiReply = aiService.chatWithSystem(prompt.system(), prompt.user(), "generate_question");
                    validated = validator.validateSingle(aiReply, questionType);
                    if (validated != null) {
                        cacheService.put(cacheKey, "generate_question", aiReply, "gpt-4o", null);
                        break;
                    }
                    log.warn("AI 出题校验失败，第 {} 次重试, kpId={}, type={}", attempt + 1, kpId, questionType);
                }
            } catch (Exception e) {
                log.warn("AI 出题异常: {}", e.getMessage());
            }
        }

        if (validated == null) {
            throw new BizException("AI 出题失败，请稍后重试");
        }

        // 4. 组装题目数据（含 Phase 2b 新字段）
        Map<String, Object> question = new HashMap<>();
        question.put("bankId", bankId);
        question.put("kpId", kpId);
        question.put("type", questionType);
        question.put("grade", grade != null ? grade : "junior");
        question.put("stem", validated.get("stem"));
        question.put("options", validated.get("options"));
        question.put("answer", validated.get("answer"));
        question.put("explanation", validated.get("explanation"));
        question.put("difficulty", difficulty);
        question.put("createdBy", "ai");
        question.put("knowledgePoints", validated.get("knowledgePoints"));
        question.put("words", validated.get("words"));
        question.put("exampleSentence", validated.get("exampleSentence"));
        question.put("exampleZh", validated.get("exampleZh"));
        question.put("extraData", validated.get("extraData"));

        // 5. 保存到 service-content
        int saved = contentClient.saveAiQuestions(List.of(question), UserContext.getUserId());
        if (saved == 0) {
            throw new BizException("题目保存失败");
        }

        return question;
    }

    /**
     * 批量生成 AI 题目
     */
    public Map<String, Object> generateBatch(Long bankId, List<String> questionTypes, int count, String grade) {
        // 1. 获取题库下所有知识点
        List<Map<String, Object>> kpList = contentClient.listKnowledgePoints(bankId);
        if (kpList.isEmpty()) {
            throw new BizException("题库中没有知识点，无法生成题目");
        }

        // 2. 随机选取知识点
        Collections.shuffle(kpList);
        int toGenerate = Math.min(count, kpList.size());

        List<Map<String, Object>> allQuestions = new ArrayList<>();
        int failCount = 0;

        // 3. 逐题生成（避免批量 prompt 太长导致质量下降）
        for (int i = 0; i < toGenerate; i++) {
            Map<String, Object> kp = kpList.get(i);
            Long kpId = toLong(kp.get("id"));
            String type = questionTypes.get(i % questionTypes.size());
            String word = (String) kp.get("content");
            String phonetic = (String) kp.get("phonetic");
            String meaning = (String) kp.get("meaningZh");
            String example = (String) kp.get("exampleSentence");
            Integer difficulty = kp.get("difficulty") != null ? ((Number) kp.get("difficulty")).intValue() : 1;

            PromptPair prompt = PromptTemplates.forType(type, grade, word, phonetic, meaning, example);

            Map<String, Object> validated = null;
            for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
                try {
                    String aiReply = aiService.chatWithSystem(prompt.system(), prompt.user(), "generate_batch");
                    validated = validator.validateSingle(aiReply, type);
                    if (validated != null) break;
                } catch (Exception e) {
                    log.warn("批量生成第 {} 题时 AI 调用异常: {}", i + 1, e.getMessage());
                }
            }

            if (validated != null) {
                Map<String, Object> question = new HashMap<>();
                question.put("bankId", bankId);
                question.put("kpId", kpId);
                question.put("type", type);
                question.put("grade", grade != null ? grade : "junior");
                question.put("stem", validated.get("stem"));
                question.put("options", validated.get("options"));
                question.put("answer", validated.get("answer"));
                question.put("explanation", validated.get("explanation"));
                question.put("difficulty", difficulty);
                question.put("createdBy", "ai");
                question.put("knowledgePoints", validated.get("knowledgePoints"));
                question.put("words", validated.get("words"));
                question.put("exampleSentence", validated.get("exampleSentence"));
                question.put("exampleZh", validated.get("exampleZh"));
                question.put("extraData", validated.get("extraData"));
                allQuestions.add(question);
            } else {
                failCount++;
                log.warn("批量生成跳过: kpId={}, word={}, type={}", kpId, word, type);
            }
        }

        // 4. 批量保存
        int savedCount = 0;
        if (!allQuestions.isEmpty()) {
            savedCount = contentClient.saveAiQuestions(allQuestions, UserContext.getUserId());
        }

        if (failCount > 0) {
            log.warn("批量出题 {} 题 AI 生成失败（共 {} 题）", failCount, toGenerate);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", toGenerate);
        result.put("success", savedCount);
        result.put("failed", failCount);
        result.put("questions", allQuestions);
        return result;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }
}
