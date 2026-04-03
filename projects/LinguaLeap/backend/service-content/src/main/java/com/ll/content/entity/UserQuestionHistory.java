package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_question_history")
public class UserQuestionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private Boolean correct = false;

    @Column(name = "practiced_at", nullable = false)
    private LocalDateTime practicedAt;

    @PrePersist
    protected void onCreate() {
        if (practicedAt == null) practicedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    public LocalDateTime getPracticedAt() { return practicedAt; }
    public void setPracticedAt(LocalDateTime practicedAt) { this.practicedAt = practicedAt; }
}
