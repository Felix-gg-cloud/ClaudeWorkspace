package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grammar_point_ref")
public class GrammarPointRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grammar_point", nullable = false, length = 200)
    private String grammarPoint;

    @Column(name = "level_code", nullable = false, length = 10)
    private String levelCode;

    @Column(nullable = false, length = 20)
    private String stage;

    @Column(name = "level_title", length = 50)
    private String levelTitle;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- getters & setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGrammarPoint() { return grammarPoint; }
    public void setGrammarPoint(String grammarPoint) { this.grammarPoint = grammarPoint; }

    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getLevelTitle() { return levelTitle; }
    public void setLevelTitle(String levelTitle) { this.levelTitle = levelTitle; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
