package com.lingobuddy.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "level_config")
public class LevelConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private int level;

    private int requiredXp;
    private String title;
    private String description;

    public LevelConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getRequiredXp() { return requiredXp; }
    public void setRequiredXp(int requiredXp) { this.requiredXp = requiredXp; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
