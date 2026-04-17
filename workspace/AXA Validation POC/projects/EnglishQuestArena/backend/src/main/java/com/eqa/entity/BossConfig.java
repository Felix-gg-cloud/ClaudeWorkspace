package com.eqa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "boss_configs")
public class BossConfig {

    @Id
    @Column(length = 50)
    private String code;

    @Column(name = "chapter_code", nullable = false, length = 50)
    private String chapterCode;

    @Column(name = "boss_name", nullable = false, length = 100)
    private String bossName;

    @Column(name = "boss_title", length = 200)
    private String bossTitle;

    @Column(name = "boss_hp")
    private int bossHp = 100;

    @Column(name = "player_hp")
    private int playerHp = 100;

    @Column(name = "time_limit_sec")
    private int timeLimitSec = 300;

    @Column(name = "damage_correct")
    private int damageCorrect = 15;

    @Column(name = "damage_wrong")
    private int damageWrong = 20;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getChapterCode() { return chapterCode; }
    public void setChapterCode(String chapterCode) { this.chapterCode = chapterCode; }
    public String getBossName() { return bossName; }
    public void setBossName(String bossName) { this.bossName = bossName; }
    public String getBossTitle() { return bossTitle; }
    public void setBossTitle(String bossTitle) { this.bossTitle = bossTitle; }
    public int getBossHp() { return bossHp; }
    public void setBossHp(int bossHp) { this.bossHp = bossHp; }
    public int getPlayerHp() { return playerHp; }
    public void setPlayerHp(int playerHp) { this.playerHp = playerHp; }
    public int getTimeLimitSec() { return timeLimitSec; }
    public void setTimeLimitSec(int timeLimitSec) { this.timeLimitSec = timeLimitSec; }
    public int getDamageCorrect() { return damageCorrect; }
    public void setDamageCorrect(int damageCorrect) { this.damageCorrect = damageCorrect; }
    public int getDamageWrong() { return damageWrong; }
    public void setDamageWrong(int damageWrong) { this.damageWrong = damageWrong; }
}
