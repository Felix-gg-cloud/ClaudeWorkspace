package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "srs_card", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "kp_id"}))
public class SrsCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kp_id")
    private Long kpId;

    @Column(name = "interval_days")
    private Integer intervalDays = 1;

    @Column(name = "ease_factor")
    private Double easeFactor = 2.5;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "correct_streak")
    private Integer correctStreak = 0;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @Column(name = "last_reviewed")
    private LocalDateTime lastReviewed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getKpId() { return kpId; }
    public void setKpId(Long kpId) { this.kpId = kpId; }

    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }

    public Double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(Double easeFactor) { this.easeFactor = easeFactor; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Integer getCorrectStreak() { return correctStreak; }
    public void setCorrectStreak(Integer correctStreak) { this.correctStreak = correctStreak; }

    public LocalDateTime getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(LocalDateTime nextReviewAt) { this.nextReviewAt = nextReviewAt; }

    public LocalDateTime getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(LocalDateTime lastReviewed) { this.lastReviewed = lastReviewed; }
}
