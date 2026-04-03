package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_bank")
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    private String description;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "source_file_url", length = 500)
    private String sourceFileUrl;

    @Column(length = 20)
    private String status = "active";

    @Column(name = "kp_count")
    private Integer kpCount = 0;

    @Column(name = "question_count")
    private Integer questionCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- getters & setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSourceFileUrl() { return sourceFileUrl; }
    public void setSourceFileUrl(String sourceFileUrl) { this.sourceFileUrl = sourceFileUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getKpCount() { return kpCount; }
    public void setKpCount(Integer kpCount) { this.kpCount = kpCount; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
