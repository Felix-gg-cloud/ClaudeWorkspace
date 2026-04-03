package com.ll.ai.service;

import com.ll.ai.entity.AiCallLog;
import com.ll.ai.repository.AiCallLogRepository;
import com.ll.common.util.UserContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final AiCallLogRepository callLogRepository;
    private final RateLimiter rateLimiter;

    public AiService(ChatModel chatModel, AiCallLogRepository callLogRepository, RateLimiter rateLimiter) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.callLogRepository = callLogRepository;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 发送 prompt 到 Groq，返回 AI 回复文本，同时记录调用日志
     */
    public String chat(String userMessage, String callType) {
        Long userId = UserContext.getUserId();
        rateLimiter.check(userId);

        long start = System.currentTimeMillis();
        String status = "success";
        Integer tokensIn = null;
        Integer tokensOut = null;
        String result;

        try {
            ChatResponse response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .chatResponse();

            result = response.getResult().getOutput().getText();

            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                tokensIn = (int) response.getMetadata().getUsage().getPromptTokens();
                tokensOut = (int) response.getMetadata().getUsage().getCompletionTokens();
            }
        } catch (Exception e) {
            status = "error";
            result = null;
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        } finally {
            long latency = System.currentTimeMillis() - start;
            logCall(callType, tokensIn, tokensOut, (int) latency, status);
            rateLimiter.record(userId);
        }

        return result;
    }

    /**
     * 带系统提示的 AI 调用
     */
    public String chatWithSystem(String systemMessage, String userMessage, String callType) {
        Long userId = UserContext.getUserId();
        rateLimiter.check(userId);

        long start = System.currentTimeMillis();
        String status = "success";
        Integer tokensIn = null;
        Integer tokensOut = null;
        String result;

        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemMessage)
                    .user(userMessage)
                    .call()
                    .chatResponse();

            result = response.getResult().getOutput().getText();

            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                tokensIn = (int) response.getMetadata().getUsage().getPromptTokens();
                tokensOut = (int) response.getMetadata().getUsage().getCompletionTokens();
            }
        } catch (Exception e) {
            status = "error";
            result = null;
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        } finally {
            long latency = System.currentTimeMillis() - start;
            logCall(callType, tokensIn, tokensOut, (int) latency, status);
            rateLimiter.record(userId);
        }

        return result;
    }

    private void logCall(String callType, Integer tokensIn, Integer tokensOut,
                         int latencyMs, String status) {
        AiCallLog log = new AiCallLog();
        log.setUserId(UserContext.getUserId());
        log.setApiProvider("github-models");
        log.setCallType(callType);
        log.setTokensIn(tokensIn);
        log.setTokensOut(tokensOut);
        log.setLatencyMs(latencyMs);
        log.setStatus(status);
        callLogRepository.save(log);
    }
}
