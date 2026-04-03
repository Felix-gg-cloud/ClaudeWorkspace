package com.ll.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.ai.prompt.PromptTemplates;
import com.ll.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeGenerateService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGenerateService.class);

    private final AiService aiService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeGenerateService(AiService aiService, CacheService cacheService) {
        this.aiService = aiService;
        this.cacheService = cacheService;
    }

    /**
     * AI 生成单元知识点内容
     */
    public List<Map<String, Object>> generateUnitContent(
            String levelCode, String levelName, String levelDesc,
            String topic, String unitName, int count) {

        // 根据级别决定类型分配提示
        String typeHint = getTypeHint(levelCode, count);

        PromptTemplates.PromptPair prompt = PromptTemplates.unitGenerate(
                levelCode, levelName, levelDesc, topic, unitName, count, typeHint);

        // 检查缓存
        String cacheKey = levelCode + ":" + topic + ":" + unitName;
        String cachedReply = cacheService.get(cacheKey, "unit_generate");
        if (cachedReply != null) {
            List<Map<String, Object>> result = parseKpList(cachedReply);
            if (result != null && !result.isEmpty()) {
                log.info("知识库生成命中缓存: level={}, topic={}", levelCode, topic);
                return result;
            }
        }

        // 调用 AI
        for (int attempt = 0; attempt <= 1; attempt++) {
            try {
                String aiReply = aiService.chatWithSystem(prompt.system(), prompt.user(), "unit_generate");
                List<Map<String, Object>> result = parseKpList(aiReply);
                if (result != null && !result.isEmpty()) {
                    cacheService.put(cacheKey, "unit_generate", aiReply, "gpt-4o", null);
                    return result;
                }
                log.warn("知识库生成 JSON 解析失败，第 {} 次重试", attempt + 1);
            } catch (Exception e) {
                log.warn("知识库生成 AI 调用异常: {}", e.getMessage());
                if (attempt == 1) throw new BizException("AI 生成失败: " + e.getMessage());
            }
        }
        throw new BizException("AI 生成知识点失败，请稍后重试");
    }

    private String getTypeHint(String levelCode, int count) {
        return switch (levelCode) {
            case "L1" -> String.format("word %d 个, sentence %d 个", (int)(count * 0.6), (int)(count * 0.4));
            case "L2" -> String.format("word %d 个, phrase %d 个, sentence %d 个",
                    (int)(count * 0.5), (int)(count * 0.2), (int)(count * 0.3));
            case "L3", "L4" -> String.format("word %d 个, phrase %d 个, sentence %d 个",
                    (int)(count * 0.5), (int)(count * 0.3), (int)(count * 0.2));
            case "L5", "L6", "L7" -> String.format("word %d 个, phrase %d 个",
                    (int)(count * 0.7), (int)(count * 0.3));
            default -> String.format("word %d 个", count);
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseKpList(String json) {
        try {
            String trimmed = json.trim();
            // 去掉可能的 markdown 代码块包裹
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.replaceFirst("```[a-z]*\\n?", "").replaceFirst("\\n?```$", "").trim();
            }
            List<Map<String, Object>> list = objectMapper.readValue(trimmed,
                    new TypeReference<List<Map<String, Object>>>() {});
            // 基础校验
            for (Map<String, Object> item : list) {
                if (item.get("content") == null || item.get("meaningZh") == null || item.get("type") == null) {
                    return null;
                }
            }
            return list;
        } catch (Exception e) {
            log.warn("知识点 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
