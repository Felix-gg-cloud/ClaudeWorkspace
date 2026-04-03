package com.ll.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_profile")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "vocabulary_level", length = 20)
    private String vocabularyLevel;

    @Column(name = "grammar_level", length = 20)
    private String grammarLevel;

    @Column(name = "listening_level", length = 20)
    private String listeningLevel;

    @Column(columnDefinition = "TEXT")
    private String interests;

    @Column(name = "weak_points", columnDefinition = "TEXT")
    private String weakPoints;

    @Column(name = "strong_points", columnDefinition = "TEXT")
    private String strongPoints;

    @Column(name = "learning_style", length = 50)
    private String learningStyle;

    @Column(name = "self_description", columnDefinition = "TEXT")
    private String selfDescription;

    @Column(name = "ai_assessment", columnDefinition = "TEXT")
    private String aiAssessment;

    @Column(name = "assessed_at")
    private LocalDateTime assessedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getVocabularyLevel() { return vocabularyLevel; }
    public void setVocabularyLevel(String v) { this.vocabularyLevel = v; }
    public String getGrammarLevel() { return grammarLevel; }
    public void setGrammarLevel(String v) { this.grammarLevel = v; }
    public String getListeningLevel() { return listeningLevel; }
    public void setListeningLevel(String v) { this.listeningLevel = v; }
    public String getInterests() { return interests; }
    public void setInterests(String v) { this.interests = v; }
    public String getWeakPoints() { return weakPoints; }
    public void setWeakPoints(String v) { this.weakPoints = v; }
    public String getStrongPoints() { return strongPoints; }
    public void setStrongPoints(String v) { this.strongPoints = v; }
    public String getLearningStyle() { return learningStyle; }
    public void setLearningStyle(String v) { this.learningStyle = v; }
    public String getSelfDescription() { return selfDescription; }
    public void setSelfDescription(String v) { this.selfDescription = v; }
    public String getAiAssessment() { return aiAssessment; }
    public void setAiAssessment(String v) { this.aiAssessment = v; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime v) { this.assessedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
