package com.eqa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "boss_pool_tasks")
public class BossPoolTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "boss_code", nullable = false, length = 50)
    private String bossCode;

    @Column(name = "task_code", nullable = false, length = 100)
    private String taskCode;

    @Column(name = "task_type", nullable = false, length = 30)
    private String taskType;

    @Column(name = "prompt_en", nullable = false, columnDefinition = "TEXT")
    private String promptEn;

    @Column(name = "prompt_zh_hint", columnDefinition = "TEXT")
    private String promptZhHint;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "explanation_en", columnDefinition = "TEXT")
    private String explanationEn;

    @Column(name = "explanation_zh", columnDefinition = "TEXT")
    private String explanationZh;

    @Column(name = "tts_enabled")
    private boolean ttsEnabled = false;

    @Column(name = "tts_text_en", length = 500)
    private String ttsTextEn;

    @Column(name = "content_item_codes", columnDefinition = "TEXT")
    private String contentItemCodes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBossCode() { return bossCode; }
    public void setBossCode(String bossCode) { this.bossCode = bossCode; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getPromptEn() { return promptEn; }
    public void setPromptEn(String promptEn) { this.promptEn = promptEn; }
    public String getPromptZhHint() { return promptZhHint; }
    public void setPromptZhHint(String promptZhHint) { this.promptZhHint = promptZhHint; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getExplanationEn() { return explanationEn; }
    public void setExplanationEn(String explanationEn) { this.explanationEn = explanationEn; }
    public String getExplanationZh() { return explanationZh; }
    public void setExplanationZh(String explanationZh) { this.explanationZh = explanationZh; }
    public boolean isTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
    public String getTtsTextEn() { return ttsTextEn; }
    public void setTtsTextEn(String ttsTextEn) { this.ttsTextEn = ttsTextEn; }
    public String getContentItemCodes() { return contentItemCodes; }
    public void setContentItemCodes(String contentItemCodes) { this.contentItemCodes = contentItemCodes; }
}
