package com.eqa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "camp_defeated", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "encounter_id"}))
public class CampDefeated {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chapter_code", nullable = false, length = 50)
    private String chapterCode;

    @Column(name = "encounter_id", nullable = false, length = 100)
    private String encounterId;

    @Column(name = "defeated_at")
    private LocalDateTime defeatedAt = LocalDateTime.now();

    private int attempts = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getChapterCode() { return chapterCode; }
    public void setChapterCode(String chapterCode) { this.chapterCode = chapterCode; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public LocalDateTime getDefeatedAt() { return defeatedAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
}
