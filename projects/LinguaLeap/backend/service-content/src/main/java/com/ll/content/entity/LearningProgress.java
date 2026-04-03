package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "kp_id"})
})
public class LearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kp_id")
    private Long kpId;

    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "level_id")
    private Long levelId;

    @Column(nullable = false, length = 20)
    private String status = "new";

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "last_review_at")
    private LocalDateTime lastReviewAt;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getKpId() { return kpId; }
    public void setKpId(Long kpId) { this.kpId = kpId; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public LocalDateTime getLastReviewAt() { return lastReviewAt; }
    public void setLastReviewAt(LocalDateTime lastReviewAt) { this.lastReviewAt = lastReviewAt; }

    public LocalDateTime getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(LocalDateTime nextReviewAt) { this.nextReviewAt = nextReviewAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
