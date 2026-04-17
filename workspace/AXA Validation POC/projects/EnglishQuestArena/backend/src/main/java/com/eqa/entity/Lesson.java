package com.eqa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @Column(length = 50)
    private String code;

    @Column(name = "chapter_code", nullable = false, length = 50)
    private String chapterCode;

    @Column(name = "day_index", nullable = false)
    private int dayIndex;

    @Column(name = "title_en", length = 200)
    private String titleEn;

    @Column(name = "title_zh", length = 200)
    private String titleZh;

    @Column(name = "estimated_minutes")
    private int estimatedMinutes = 30;

    @Column(name = "target_task_count")
    private int targetTaskCount = 30;

    @Column(name = "auto_drill_enabled")
    private boolean autoDrillEnabled = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getChapterCode() { return chapterCode; }
    public void setChapterCode(String chapterCode) { this.chapterCode = chapterCode; }
    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getTitleZh() { return titleZh; }
    public void setTitleZh(String titleZh) { this.titleZh = titleZh; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public int getTargetTaskCount() { return targetTaskCount; }
    public void setTargetTaskCount(int targetTaskCount) { this.targetTaskCount = targetTaskCount; }
    public boolean isAutoDrillEnabled() { return autoDrillEnabled; }
    public void setAutoDrillEnabled(boolean autoDrillEnabled) { this.autoDrillEnabled = autoDrillEnabled; }
}
