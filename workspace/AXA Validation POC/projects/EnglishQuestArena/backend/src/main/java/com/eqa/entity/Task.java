package com.eqa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @Column(length = 100)
    private String code;

    @Column(name = "lesson_code", length = 50)
    private String lessonCode;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false, length = 30)
    private String type;

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

    @Column(name = "xp_reward")
    private int xpReward = 5;

    @Column(name = "gold_reward")
    private int goldReward = 0;

    @Column(name = "tts_enabled")
    private boolean ttsEnabled = false;

    @Column(name = "tts_text_en", length = 500)
    private String ttsTextEn;

    @Column(name = "content_item_codes", columnDefinition = "TEXT")
    private String contentItemCodes;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLessonCode() { return lessonCode; }
    public void setLessonCode(String lessonCode) { this.lessonCode = lessonCode; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
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
    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    public int getGoldReward() { return goldReward; }
    public void setGoldReward(int goldReward) { this.goldReward = goldReward; }
    public boolean isTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
    public String getTtsTextEn() { return ttsTextEn; }
    public void setTtsTextEn(String ttsTextEn) { this.ttsTextEn = ttsTextEn; }
    public String getContentItemCodes() { return contentItemCodes; }
    public void setContentItemCodes(String contentItemCodes) { this.contentItemCodes = contentItemCodes; }
}
