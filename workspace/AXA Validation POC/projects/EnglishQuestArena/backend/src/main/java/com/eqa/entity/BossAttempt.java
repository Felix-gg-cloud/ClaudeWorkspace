package com.eqa.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "boss_attempts", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "boss_code", "attempt_date"}))
public class BossAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "boss_code", nullable = false, length = 50)
    private String bossCode;

    @Column(name = "attempt_date", nullable = false)
    private LocalDate attemptDate;

    private boolean victory;

    @Column(name = "boss_hp_remaining")
    private int bossHpRemaining;

    @Column(name = "player_hp_remaining")
    private int playerHpRemaining;

    @Column(name = "max_combo")
    private int maxCombo;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBossCode() { return bossCode; }
    public void setBossCode(String bossCode) { this.bossCode = bossCode; }
    public LocalDate getAttemptDate() { return attemptDate; }
    public void setAttemptDate(LocalDate attemptDate) { this.attemptDate = attemptDate; }
    public boolean isVictory() { return victory; }
    public void setVictory(boolean victory) { this.victory = victory; }
    public int getBossHpRemaining() { return bossHpRemaining; }
    public void setBossHpRemaining(int bossHpRemaining) { this.bossHpRemaining = bossHpRemaining; }
    public int getPlayerHpRemaining() { return playerHpRemaining; }
    public void setPlayerHpRemaining(int playerHpRemaining) { this.playerHpRemaining = playerHpRemaining; }
    public int getMaxCombo() { return maxCombo; }
    public void setMaxCombo(int maxCombo) { this.maxCombo = maxCombo; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
}
