package com.ll.content.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_set")
public class StudySet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType = "text";

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "source_file_url", length = 500)
    private String sourceFileUrl;

    @Column(length = 20)
    private String grade;

    @Column(nullable = false, length = 20)
    private String status = "processing";

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_strategy", columnDefinition = "jsonb")
    private String aiStrategy;

    @Column(name = "item_count")
    private Integer itemCount = 0;

    @Column(name = "question_count")
    private Integer questionCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- getters & setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    public String getSourceFileUrl() { return sourceFileUrl; }
    public void setSourceFileUrl(String sourceFileUrl) { this.sourceFileUrl = sourceFileUrl; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiStrategy() { return aiStrategy; }
    public void setAiStrategy(String aiStrategy) { this.aiStrategy = aiStrategy; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
