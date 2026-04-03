package com.ll.ai.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 生成题目的质量校验器
 */
@Component
public class AiQuestionValidator {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_STEM_LENGTH = 500;
    private static final int MAX_OPTION_LENGTH = 100;

    /**
     * 校验并解析单题 AI 返回的 JSON
     * @return 校验通过的 Map，或 null（校验失败）
     */
    public Map<String, Object> validateSingle(String json, String expectedType) {
        // 清理 JSON（去掉 markdown 代码块标记）
        String cleaned = cleanJson(json);

        JsonNode node;
        try {
            node = objectMapper.readTree(cleaned);
        } catch (JsonProcessingException e) {
            log.warn("AI 返回 JSON 解析失败: {}", e.getMessage());
            return null;
        }

        return validateNode(node, expectedType);
    }

    /**
     * 校验并解析批量 AI 返回的 JSON 数组
     * @return 校验通过的题目列表（过滤掉不合格的）
     */
    public List<Map<String, Object>> validateBatch(String json) {
        String cleaned = cleanJson(json);

        JsonNode root;
        try {
            root = objectMapper.readTree(cleaned);
        } catch (JsonProcessingException e) {
            log.warn("AI 批量返回 JSON 解析失败: {}", e.getMessage());
            return List.of();
        }

        if (!root.isArray()) {
            log.warn("AI 批量返回不是 JSON 数组");
            // 尝试单个对象
            Map<String, Object> single = validateNodeLoose(root);
            return single != null ? List.of(single) : List.of();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode item : root) {
            Map<String, Object> validated = validateNodeLoose(item);
            if (validated != null) {
                results.add(validated);
            }
        }
        return results;
    }

    private Map<String, Object> validateNode(JsonNode node, String expectedType) {
        // 1. stem 必填
        String stem = getTextOrNull(node, "stem");
        if (stem == null || stem.isBlank()) {
            log.warn("AI 题目缺少 stem");
            return null;
        }

        // 2. answer 必填
        String answer = getTextOrNull(node, "answer");
        if (answer == null || answer.isBlank()) {
            log.warn("AI 题目缺少 answer");
            return null;
        }

        // 3. 长度校验
        if (stem.length() > MAX_STEM_LENGTH) {
            stem = stem.substring(0, MAX_STEM_LENGTH);
        }

        // 4. 选择题校验 options（如果有的话）
        String options = null;
        if (node.has("options") && node.get("options").isArray() && !node.get("options").isEmpty()) {
            List<String> optionList = new ArrayList<>();
            for (JsonNode opt : node.get("options")) {
                String optText = opt.asText();
                if (optText.length() > MAX_OPTION_LENGTH) {
                    optText = optText.substring(0, MAX_OPTION_LENGTH);
                }
                optionList.add(optText);
            }

            // 选择题 answer 必须在 options 中
            if (expectedType != null && expectedType.contains("choice") && !optionList.contains(answer)) {
                log.warn("AI 选择题 answer '{}' 不在 options 中", answer);
                return null;
            }

            try {
                options = objectMapper.writeValueAsString(optionList);
            } catch (JsonProcessingException e) {
                return null;
            }
        } else if (expectedType != null && expectedType.contains("choice")) {
            // 选择题必须有 options
            log.warn("AI 选择题缺少 options");
            return null;
        }

        // 5. 重复检测：answer 不能和 stem 完全相同
        if (answer.equals(stem)) {
            log.warn("AI 题目 answer 与 stem 完全相同");
            return null;
        }

        String explanation = getTextOrNull(node, "explanation");

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("stem", stem);
        result.put("answer", answer);
        result.put("options", options);
        result.put("explanation", explanation);

        // Phase 2b 新字段
        result.put("knowledgePoints", getTextOrNull(node, "knowledgePoints"));
        result.put("exampleSentence", getTextOrNull(node, "exampleSentence"));
        result.put("exampleZh", getTextOrNull(node, "exampleZh"));

        // words 数组 → JSON 字符串
        if (node.has("words") && node.get("words").isArray()) {
            try {
                result.put("words", objectMapper.writeValueAsString(node.get("words")));
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        // extraData 对象 → JSON 字符串
        if (node.has("extraData") && !node.get("extraData").isNull()) {
            try {
                result.put("extraData", objectMapper.writeValueAsString(node.get("extraData")));
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        // stemZh（小学填空题中文翻译）
        String stemZh = getTextOrNull(node, "stemZh");
        if (stemZh != null) {
            result.put("stemZh", stemZh);
        }

        return result;
    }

    /**
     * 宽松模式校验（批量时用，从 node 中读取 type）
     */
    private Map<String, Object> validateNodeLoose(JsonNode node) {
        String type = getTextOrNull(node, "type");
        Map<String, Object> result = validateNode(node, type);
        if (result != null) {
            result.put("type", type);
            result.put("kpContent", getTextOrNull(node, "kpContent"));
        }
        return result;
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    /**
     * 清理 JSON 字符串：去掉 markdown 代码块标记、前后空白
     */
    private String cleanJson(String json) {
        if (json == null) return "";
        String s = json.strip();
        // 去掉 ```json ... ``` 包裹
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.strip();
        }
        return s;
    }
}
