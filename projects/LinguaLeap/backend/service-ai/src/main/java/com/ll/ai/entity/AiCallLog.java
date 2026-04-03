package com.ll.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_call_log")
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "api_provider", length = 50)
    private String apiProvider;

    @Column(name = "call_type", length = 50)
    private String callType;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getApiProvider() { return apiProvider; }
    public void setApiProvider(String apiProvider) { this.apiProvider = apiProvider; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public Integer getTokensIn() { return tokensIn; }
    public void setTokensIn(Integer tokensIn) { this.tokensIn = tokensIn; }

    public Integer getTokensOut() { return tokensOut; }
    public void setTokensOut(Integer tokensOut) { this.tokensOut = tokensOut; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
