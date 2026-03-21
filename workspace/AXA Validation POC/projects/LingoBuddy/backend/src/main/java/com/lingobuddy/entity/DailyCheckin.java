package com.lingobuddy.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_checkin", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "checkinDate"}))
public class DailyCheckin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate checkinDate;

    private int xpEarned;
    private int coinsEarned;
    private int streak;

    public DailyCheckin() {}

    public DailyCheckin(Long userId, LocalDate checkinDate) {
        this.userId = userId;
        this.checkinDate = checkinDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getCheckinDate() { return checkinDate; }
    public void setCheckinDate(LocalDate checkinDate) { this.checkinDate = checkinDate; }
    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }
    public int getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(int coinsEarned) { this.coinsEarned = coinsEarned; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
}
