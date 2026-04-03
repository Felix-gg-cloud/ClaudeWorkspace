package com.ll.ai.service;

import com.ll.ai.prompt.PromptTemplates;
import com.ll.ai.prompt.PromptTemplates.PromptPair;
import com.ll.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContentExtractService {

    private static final Logger log = LoggerFactory.getLogger(ContentExtractService.class);
    private final AiService aiService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContentExtractService(AiService aiService, CacheService cacheService) {
        this.aiService = aiService;
        this.cacheService = cacheService;
    }

    /**
     * AI 提取 + 分类用户上传的内容
     * @return {items: [...], summary: "..."}
     */
    public Map<String, Object> extractAndClassify(String text, String grade, String sourceType, String userNote) {
        if (text == null || text.isBlank()) {
            throw new BizException("文本不能为空");
        }

        log.info("内容提取请求: grade={}, sourceType={}, userNote={}, textLength={}",
                grade, sourceType, userNote, text.length());

        // 截取前 5000 字符避免超长
        String trimmedText = text.length() > 5000 ? text.substring(0, 5000) : text;

        // 缓存
        String cacheKey = trimmedText.hashCode() + ":" + grade + ":" + sourceType + ":" +
                (userNote != null ? userNote.hashCode() : "none");
        String cached = cacheService.get(cacheKey, "content_extract");

        String aiReply;
        if (cached != null) {
            log.info("内容提取命中缓存");
            aiReply = cached;
        } else {
            PromptPair prompt = PromptTemplates.contentExtract(grade, trimmedText, sourceType, userNote);
            log.info("System prompt 前200字: {}", prompt.system().substring(0, Math.min(200, prompt.system().length())));
            log.info("User prompt 前300字: {}", prompt.user().substring(0, Math.min(300, prompt.user().length())));
            aiReply = aiService.chatWithSystem(prompt.system(), prompt.user(), "content_extract");
            log.info("AI 返回长度: {}, 前500字: {}", aiReply.length(), aiReply.substring(0, Math.min(500, aiReply.length())));
            cacheService.put(cacheKey, "content_extract", aiReply, "gpt-4o", null);
        }

        return parseExtractResult(aiReply);
    }

    /**
     * AI 生成出题策略
     */
    public Map<String, Object> generateStrategy(String grade, String userNote,
                                                  Map<String, Integer> categoryCounts,
                                                  String summary) {
        int vocabCount = categoryCounts.getOrDefault("vocabulary", 0);
        int grammarCount = categoryCounts.getOrDefault("grammar", 0);
        int sentenceCount = categoryCounts.getOrDefault("sentence_pattern", 0);
        int passageCount = categoryCounts.getOrDefault("passage", 0);

        PromptPair prompt = PromptTemplates.studyStrategy(
                grade, userNote, vocabCount, grammarCount, sentenceCount, passageCount, summary);
        String aiReply = aiService.chatWithSystem(prompt.system(), prompt.user(), "study_strategy");

        return parseStrategyResult(aiReply);
    }

    private Map<String, Object> parseExtractResult(String json) {
        String cleaned = cleanJson(json);
        try {
            JsonNode root = objectMapper.readTree(cleaned);

            String summary = root.has("summary") ? root.get("summary").asText() : "";

            JsonNode itemsNode = root.has("items") ? root.get("items") :
                                 root.isArray() ? root : null;
            if (itemsNode == null || !itemsNode.isArray()) {
                throw new BizException("AI 返回格式不符");
            }

            List<Map<String, Object>> items = new ArrayList<>();
            for (JsonNode item : itemsNode) {
                String content = item.has("content") ? item.get("content").asText() : null;
                if (content == null || content.isBlank()) continue;

                Map<String, Object> li = new LinkedHashMap<>();
                li.put("category", item.has("category") ? item.get("category").asText() : "vocabulary");
                li.put("content", content);
                li.put("meaningZh", item.has("meaningZh") ? item.get("meaningZh").asText() : "");
                li.put("phonetic", item.has("phonetic") ? item.get("phonetic").asText(null) : null);
                li.put("exampleSentence", item.has("exampleSentence") ? item.get("exampleSentence").asText(null) : null);
                li.put("exampleZh", item.has("exampleZh") ? item.get("exampleZh").asText(null) : null);
                li.put("extraData", item.has("extraData") && !item.get("extraData").isNull() ?
                        item.get("extraData").toString() : null);
                li.put("difficulty", item.has("difficulty") ? item.get("difficulty").asInt(1) : 1);
                li.put("aiNote", item.has("aiNote") ? item.get("aiNote").asText("") : "");
                items.add(li);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("summary", summary);
            result.put("total", items.size());
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 内容提取 JSON 解析失败: {}", e.getMessage());
            throw new BizException("AI 内容提取结果解析失败");
        }
    }

    private Map<String, Object> parseStrategyResult(String json) {
        String cleaned = cleanJson(json);
        try {
            JsonNode root = objectMapper.readTree(cleaned);

            Map<String, Object> strategy = new LinkedHashMap<>();
            strategy.put("focus", root.has("focus") ? root.get("focus").asText() : "vocabulary");
            strategy.put("reasoning", root.has("reasoning") ? root.get("reasoning").asText() : "");

            if (root.has("weights")) {
                Map<String, Double> weights = new LinkedHashMap<>();
                root.get("weights").fields().forEachRemaining(e ->
                        weights.put(e.getKey(), e.getValue().asDouble(0)));
                strategy.put("weights", weights);
            }

            if (root.has("questionTypePreference")) {
                Map<String, Double> qtp = new LinkedHashMap<>();
                root.get("questionTypePreference").fields().forEachRemaining(e ->
                        qtp.put(e.getKey(), e.getValue().asDouble(0)));
                strategy.put("questionTypePreference", qtp);
            }

            strategy.put("totalRecommended", root.has("totalRecommended") ? root.get("totalRecommended").asInt(30) : 30);
            strategy.put("dailyTarget", root.has("dailyTarget") ? root.get("dailyTarget").asInt(10) : 10);

            return strategy;
        } catch (Exception e) {
            log.warn("AI 策略生成 JSON 解析失败: {}", e.getMessage());
            // 返回默认策略
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("focus", "vocabulary");
            fallback.put("reasoning", "默认策略");
            fallback.put("weights", Map.of("vocabulary", 0.5, "grammar", 0.2, "sentence_pattern", 0.2, "passage", 0.1));
            fallback.put("questionTypePreference", Map.of("en2zh_choice", 0.3, "zh2en_choice", 0.3, "fill_blank", 0.2, "translate", 0.2));
            fallback.put("totalRecommended", 30);
            fallback.put("dailyTarget", 10);
            return fallback;
        }
    }

    private String cleanJson(String json) {
        if (json == null) return "";
        String s = json.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        }
        return s.strip();
    }
}
