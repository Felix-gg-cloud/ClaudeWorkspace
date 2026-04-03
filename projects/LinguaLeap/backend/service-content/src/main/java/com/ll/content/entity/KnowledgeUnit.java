package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_unit")
public class KnowledgeUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(length = 50)
    private String topic;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "kp_count")
    private Integer kpCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getKpCount() { return kpCount; }
    public void setKpCount(Integer kpCount) { this.kpCount = kpCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
