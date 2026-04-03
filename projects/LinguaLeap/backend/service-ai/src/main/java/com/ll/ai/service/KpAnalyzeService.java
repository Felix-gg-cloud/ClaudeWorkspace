package com.ll.ai.service;

import com.ll.ai.client.ContentServiceClient;
import com.ll.ai.prompt.PromptTemplates;
import com.ll.ai.prompt.PromptTemplates.PromptPair;
import com.ll.common.exception.BizException;
import com.ll.common.util.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KpAnalyzeService {

    private static final Logger log = LoggerFactory.getLogger(KpAnalyzeService.class);
    private final AiService aiService;
    private final CacheService cacheService;
    private final ContentServiceClient contentClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KpAnalyzeService(AiService aiService, CacheService cacheService,
                            ContentServiceClient contentClient) {
        this.aiService = aiService;
        this.cacheService = cacheService;
        this.contentClient = contentClient;
    }

    /**
     * 从文本中 AI 提取知识点
     */
    public Map<String, Object> analyzeText(String text, String grade, Long bankId) {
        if (text == null || text.isBlank()) {
            throw new BizException("文本不能为空");
        }

        // 1. 检查缓存
        String cacheKey = text.strip() + ":" + grade;
        String cachedResult = cacheService.get(cacheKey, "kp_analyze");

        String aiReply;
        if (cachedResult != null) {
            log.info("知识点解析命中缓存");
            aiReply = cachedResult;
        } else {
            // 2. 调用 AI
            PromptPair prompt = PromptTemplates.kpAnalyze(grade, text);
            aiReply = aiService.chatWithSystem(prompt.system(), prompt.user(), "kp_analyze");
            cacheService.put(cacheKey, "kp_analyze", aiReply, "gpt-4o", null);
        }

        // 3. 解析 AI 返回
        List<Map<String, Object>> kpList = parseKpResult(aiReply);
        if (kpList.isEmpty()) {
            throw new BizException("AI 未能从文本中提取到知识点");
        }

        // 4. 如果指定了 bankId，保存到 service-content
        int savedCount = 0;
        if (bankId != null) {
            savedCount = contentClient.saveKnowledgePoints(kpList, bankId, UserContext.getUserId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("knowledgePoints", kpList);
        result.put("total", kpList.size());
        result.put("saved", savedCount);
        return result;
    }

    private List<Map<String, Object>> parseKpResult(String json) {
        String cleaned = cleanJson(json);
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode kpArray;
            if (root.has("knowledgePoints")) {
                kpArray = root.get("knowledgePoints");
            } else if (root.isArray()) {
                kpArray = root;
            } else {
                log.warn("AI 知识点解析格式不符");
                return List.of();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : kpArray) {
                String content = item.has("content") ? item.get("content").asText() : null;
                String meaningZh = item.has("meaningZh") ? item.get("meaningZh").asText() : null;
                if (content == null || content.isBlank() || meaningZh == null || meaningZh.isBlank()) {
                    continue;
                }
                Map<String, Object> kp = new HashMap<>();
                kp.put("content", content);
                kp.put("meaningZh", meaningZh);
                kp.put("type", item.has("type") ? item.get("type").asText() : "word");
                kp.put("difficulty", item.has("difficulty") ? item.get("difficulty").asInt(1) : 1);
                result.add(kp);
            }
            return result;
        } catch (Exception e) {
            log.warn("AI 知识点解析 JSON 失败: {}", e.getMessage());
            return List.of();
        }
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
