package com.ll.content.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 调用 service-ai 的 HTTP 客户端
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    @Value("${app.ai-service.url:http://localhost:8083}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * AI 提取 + 分类内容
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractContent(String text, String grade, String sourceType, String userNote, String token) {
        String url = aiServiceUrl + "/api/ai/extract/content";
        Map<String, String> body = Map.of(
                "text", text,
                "grade", grade != null ? grade : "junior",
                "sourceType", sourceType != null ? sourceType : "text",
                "userNote", userNote != null ? userNote : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", token);
        }

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.has("data")) {
                return objectMapper.convertValue(root.get("data"), Map.class);
            }
            return Map.of();
        } catch (Exception e) {
            log.error("调用 AI 内容提取失败: {}", e.getMessage());
            throw new RuntimeException("AI 内容提取服务暂时不可用");
        }
    }

    /**
     * AI 生成出题策略
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateStrategy(String grade, String userNote,
                                                 Map<String, Integer> categoryCounts,
                                                 String summary, String token) {
        String url = aiServiceUrl + "/api/ai/extract/strategy";
        Map<String, Object> body = Map.of(
                "grade", grade != null ? grade : "junior",
                "userNote", userNote != null ? userNote : "",
                "categoryCounts", categoryCounts,
                "summary", summary != null ? summary : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", token);
        }

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.has("data")) {
                return objectMapper.convertValue(root.get("data"), Map.class);
            }
            return Map.of();
        } catch (Exception e) {
            log.error("调用 AI 策略生成失败: {}", e.getMessage());
            return Map.of("focus", "vocabulary", "reasoning", "默认策略(AI不可用)");
        }
    }
}
