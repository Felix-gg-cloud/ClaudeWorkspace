package com.eqa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
    @Column(length = 50)
    private String code;

    @Column(name = "cefr_level", nullable = false, length = 10)
    private String cefrLevel;

    @Column(name = "title_en", nullable = false, length = 200)
    private String titleEn;

    @Column(name = "title_zh", nullable = false, length = 200)
    private String titleZh;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private int days;

    @Column(name = "camp_unlock_rate")
    private BigDecimal campUnlockRate = new BigDecimal("0.80");

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCefrLevel() { return cefrLevel; }
    public void setCefrLevel(String cefrLevel) { this.cefrLevel = cefrLevel; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getTitleZh() { return titleZh; }
    public void setTitleZh(String titleZh) { this.titleZh = titleZh; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
    public BigDecimal getCampUnlockRate() { return campUnlockRate; }
    public void setCampUnlockRate(BigDecimal campUnlockRate) { this.campUnlockRate = campUnlockRate; }
}
