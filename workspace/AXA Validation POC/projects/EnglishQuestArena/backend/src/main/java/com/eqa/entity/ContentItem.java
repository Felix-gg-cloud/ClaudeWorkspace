package com.eqa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "content_items")
public class ContentItem {

    @Id
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "cefr_level", nullable = false, length = 10)
    private String cefrLevel;

    @Column(name = "chapter_code", nullable = false, length = 50)
    private String chapterCode;

    @Column(name = "day_index")
    private int dayIndex = 1;

    @Column(name = "text_en", nullable = false, length = 200)
    private String textEn;

    @Column(name = "text_zh", nullable = false, length = 200)
    private String textZh;

    @Column(length = 100)
    private String ipa;

    @Column(name = "example_en", columnDefinition = "TEXT")
    private String exampleEn;

    @Column(name = "example_zh", columnDefinition = "TEXT")
    private String exampleZh;

    @Column(columnDefinition = "TEXT")
    private String distractors;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_note", length = 200)
    private String sourceNote;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCefrLevel() { return cefrLevel; }
    public void setCefrLevel(String cefrLevel) { this.cefrLevel = cefrLevel; }
    public String getChapterCode() { return chapterCode; }
    public void setChapterCode(String chapterCode) { this.chapterCode = chapterCode; }
    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
    public String getTextEn() { return textEn; }
    public void setTextEn(String textEn) { this.textEn = textEn; }
    public String getTextZh() { return textZh; }
    public void setTextZh(String textZh) { this.textZh = textZh; }
    public String getIpa() { return ipa; }
    public void setIpa(String ipa) { this.ipa = ipa; }
    public String getExampleEn() { return exampleEn; }
    public void setExampleEn(String exampleEn) { this.exampleEn = exampleEn; }
    public String getExampleZh() { return exampleZh; }
    public void setExampleZh(String exampleZh) { this.exampleZh = exampleZh; }
    public String getDistractors() { return distractors; }
    public void setDistractors(String distractors) { this.distractors = distractors; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceNote() { return sourceNote; }
    public void setSourceNote(String sourceNote) { this.sourceNote = sourceNote; }
}
