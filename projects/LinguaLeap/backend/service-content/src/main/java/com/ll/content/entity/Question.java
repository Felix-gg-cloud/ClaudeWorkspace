package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "kp_id")
    private Long kpId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stem;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private Integer difficulty = 1;

    @Column(name = "created_by", length = 20)
    private String createdBy = "template";

    @Column(name = "knowledge_points", columnDefinition = "TEXT")
    private String knowledgePoints;

    @Column(columnDefinition = "TEXT")
    private String words;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "example_zh", columnDefinition = "TEXT")
    private String exampleZh;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @Column(length = 20)
    private String grade;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public Long getKpId() { return kpId; }
    public void setKpId(Long kpId) { this.kpId = kpId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getKnowledgePoints() { return knowledgePoints; }
    public void setKnowledgePoints(String knowledgePoints) { this.knowledgePoints = knowledgePoints; }

    public String getWords() { return words; }
    public void setWords(String words) { this.words = words; }

    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }

    public String getExampleZh() { return exampleZh; }
    public void setExampleZh(String exampleZh) { this.exampleZh = exampleZh; }

    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}
