package com.ll.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.ai.prompt.PromptTemplates;
import com.ll.ai.prompt.PromptTemplates.PromptPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI 翻译评判服务（供初中/高中翻译题使用）
 */
@Service
public class TranslationJudgeService {

    private static final Logger log = LoggerFactory.getLogger(TranslationJudgeService.class);
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranslationJudgeService(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 调用 AI 评判翻译
     * @return {"correct": true/false, "score": 85, "feedback": "...", "corrections": [...]}
     */
    public Map<String, Object> judge(String stem, String referenceAnswer, String userAnswer, String grade) {
        PromptPair prompt = PromptTemplates.judgeTranslate(stem, referenceAnswer, userAnswer, grade);

        try {
            String aiReply = aiService.chatWithSystem(prompt.system(), prompt.user(), "judge_translate");
            return parseJudgeResult(aiReply);
        } catch (Exception e) {
            log.warn("AI 翻译评判异常，降级为字符串比较: {}", e.getMessage());
            return fallbackJudge(referenceAnswer, userAnswer);
        }
    }

    private Map<String, Object> parseJudgeResult(String json) {
        String cleaned = cleanJson(json);
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("correct", node.has("correct") && node.get("correct").asBoolean(false));
            result.put("score", node.has("score") ? node.get("score").asInt(0) : 0);
            result.put("feedback", node.has("feedback") ? node.get("feedback").asText("") : "");

            List<String> corrections = new ArrayList<>();
            if (node.has("corrections") && node.get("corrections").isArray()) {
                for (JsonNode c : node.get("corrections")) {
                    corrections.add(c.asText());
                }
            }
            result.put("corrections", corrections);
            return result;
        } catch (JsonProcessingException e) {
            log.warn("AI 评判结果解析失败: {}", e.getMessage());
            return fallbackJudge("", "");
        }
    }

    private Map<String, Object> fallbackJudge(String referenceAnswer, String userAnswer) {
        boolean correct = referenceAnswer.trim().equalsIgnoreCase(userAnswer.trim());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", correct);
        result.put("score", correct ? 100 : 0);
        result.put("feedback", correct ? "回答正确" : "回答不正确，请参考标准答案");
        result.put("corrections", List.of());
        return result;
    }

    private String cleanJson(String json) {
        if (json == null) return "";
        String s = json.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.strip();
        }
        return s;
    }
}
