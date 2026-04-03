package com.ll.content.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_item")
public class LearningItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_set_id", nullable = false)
    private Long studySetId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "meaning_zh", length = 500)
    private String meaningZh;

    @Column(length = 100)
    private String phonetic;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "example_zh", columnDefinition = "TEXT")
    private String exampleZh;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data", columnDefinition = "jsonb")
    private String extraData;

    private Integer difficulty = 1;

    @Column(name = "ai_note", columnDefinition = "TEXT")
    private String aiNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- getters & setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudySetId() { return studySetId; }
    public void setStudySetId(Long studySetId) { this.studySetId = studySetId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMeaningZh() { return meaningZh; }
    public void setMeaningZh(String meaningZh) { this.meaningZh = meaningZh; }

    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }

    public String getExampleZh() { return exampleZh; }
    public void setExampleZh(String exampleZh) { this.exampleZh = exampleZh; }

    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public String getAiNote() { return aiNote; }
    public void setAiNote(String aiNote) { this.aiNote = aiNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
