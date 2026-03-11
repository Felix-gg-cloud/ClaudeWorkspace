package com.lingobuddy.dto;

import java.time.LocalDate;

public class CheckinDto {
    private LocalDate date;
    private int xpEarned;
    private int coinsEarned;
    private int streak;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }
    public int getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(int coinsEarned) { this.coinsEarned = coinsEarned; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
}
