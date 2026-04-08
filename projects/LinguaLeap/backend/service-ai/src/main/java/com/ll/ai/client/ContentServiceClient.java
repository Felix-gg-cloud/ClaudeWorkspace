package com.ll.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 调用 service-content 的 HTTP 客户端
 */
@Component
public class ContentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ContentServiceClient.class);

    @Value("${app.content-service.url:http://localhost:8082}")
    private String contentServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取知识点详情
     * @return KP 字段 Map，获取失败返回 null
     */
    public Map<String, Object> getKnowledgePoint(Long kpId) {
        try {
            String url = contentServiceUrl + "/api/content/kps/" + kpId;
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.get("code").asInt() != 200) {
                log.warn("获取知识点失败: {}", root.get("message").asText());
                return null;
            }
            return objectMapper.convertValue(root.get("data"), Map.class);
        } catch (Exception e) {
            log.error("调用 content-service 获取 KP 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取题库下所有知识点
     * @return KP 列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listKnowledgePoints(Long bankId) {
        try {
            String url = contentServiceUrl + "/api/content/banks/" + bankId + "/kps?page=0&size=1000";
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.get("code").asInt() != 200) {
                log.warn("获取知识点列表失败: {}", root.get("message").asText());
                return List.of();
            }
            JsonNode content = root.get("data").get("content");
            return objectMapper.convertValue(content,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            log.error("调用 content-service 获取 KP 列表失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 保存 AI 生成的题目到 service-content
     * @return 保存成功的题目数量
     */
    public int saveAiQuestions(List<Map<String, Object>> questions, Long userId) {
        try {
            String url = contentServiceUrl + "/api/content/questions/ai-save";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) {
                headers.set("X-User-Id", userId.toString());
            }
            String body = objectMapper.writeValueAsString(questions);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.get("code").asInt() == 200 && root.get("data").isArray()) {
                return root.get("data").size();
            }
            log.warn("保存 AI 题目失败: {}", root.get("message").asText());
            return 0;
        } catch (Exception e) {
            log.error("调用 content-service 保存题目失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 批量保存知识点到 service-content
     */
    public int saveKnowledgePoints(List<Map<String, Object>> kpList, Long bankId, Long userId) {
        try {
            String url = contentServiceUrl + "/api/content/banks/" + bankId + "/kps/batch";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) {
                headers.set("X-User-Id", userId.toString());
            }
            String body = objectMapper.writeValueAsString(kpList);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.get("code").asInt() == 200 && root.get("data").isArray()) {
                return root.get("data").size();
            }
            log.warn("保存知识点失败: {}", root.get("message").asText());
            return 0;
        } catch (Exception e) {
            log.error("调用 content-service 保存知识点失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 调用 service-content 的模板出题引擎（降级用，返回第一道题的详情）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> templateGenerateOne(Long bankId, List<Long> kpIds, List<String> types) {
        try {
            String url = contentServiceUrl + "/api/content/questions/generate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("bankId", bankId);
            body.put("types", types);
            body.put("count", 1);
            if (kpIds != null) body.put("kpIds", kpIds);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());

            if (root.get("code").asInt() == 200 && root.get("data").isArray() && root.get("data").size() > 0) {
                return objectMapper.convertValue(root.get("data").get(0), Map.class);
            }
            return null;
        } catch (Exception e) {
            log.error("降级模板出题调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 调用模板出题并返回保存的数量（批量降级用）
     */
    public int templateGenerate(Long bankId, List<String> types, int count) {
        try {
            String url = contentServiceUrl + "/api/content/questions/generate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("bankId", bankId);
            body.put("types", types);
            body.put("count", count);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());

            if (root.get("code").asInt() == 200 && root.get("data").isArray()) {
                return root.get("data").size();
            }
            return 0;
        } catch (Exception e) {
            log.error("降级模板出题调用失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Phase 5a — 查询教学约束数据
     * @param path API 路径，如 /api/content/constraints/vocab/L7
     * @return data 节点内容，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchConstraintData(String path) {
        try {
            String url = contentServiceUrl + path;
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.get("code").asInt() != 200) {
                log.warn("查询约束数据失败: {} → {}", path, root.get("message").asText());
                return null;
            }
            return objectMapper.convertValue(root.get("data"), Map.class);
        } catch (Exception e) {
            log.error("调用 content-service 约束 API 失败: {} → {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Phase 5a: 通知 SRS 系统按内容复习（AI 对话反馈联动）
     */
    public void reviewSrsByContent(Long userId, String kpContent, boolean correct) {
        try {
            String url = contentServiceUrl + "/api/content/srs/review-by-content";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "userId", userId,
                    "content", kpContent,
                    "correct", correct);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.debug("SRS 反馈: userId={}, content={}, correct={}", userId, kpContent, correct);
        } catch (Exception e) {
            log.warn("SRS 反馈通知失败: {}", e.getMessage());
        }
    }
}
