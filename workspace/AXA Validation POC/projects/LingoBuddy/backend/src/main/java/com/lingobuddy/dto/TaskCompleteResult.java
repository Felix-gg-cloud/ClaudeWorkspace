package com.lingobuddy.dto;

import java.util.List;

public class TaskCompleteResult {
    private int xpGained;
    private int totalXp;
    private int coinsGained;
    private boolean lessonCompleted;
    private boolean levelUp;
    private int newLevel;
    private String newLevelTitle;
    private int checkinXp;
    private int checkinCoins;
    private int streak;
    private List<AchievementDto> newAchievements;

    public int getXpGained() { return xpGained; }
    public void setXpGained(int xpGained) { this.xpGained = xpGained; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
    public int getCoinsGained() { return coinsGained; }
    public void setCoinsGained(int coinsGained) { this.coinsGained = coinsGained; }
    public boolean isLessonCompleted() { return lessonCompleted; }
    public void setLessonCompleted(boolean lessonCompleted) { this.lessonCompleted = lessonCompleted; }
    public boolean isLevelUp() { return levelUp; }
    public void setLevelUp(boolean levelUp) { this.levelUp = levelUp; }
    public int getNewLevel() { return newLevel; }
    public void setNewLevel(int newLevel) { this.newLevel = newLevel; }
    public String getNewLevelTitle() { return newLevelTitle; }
    public void setNewLevelTitle(String newLevelTitle) { this.newLevelTitle = newLevelTitle; }
    public int getCheckinXp() { return checkinXp; }
    public void setCheckinXp(int checkinXp) { this.checkinXp = checkinXp; }
    public int getCheckinCoins() { return checkinCoins; }
    public void setCheckinCoins(int checkinCoins) { this.checkinCoins = checkinCoins; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    public List<AchievementDto> getNewAchievements() { return newAchievements; }
    public void setNewAchievements(List<AchievementDto> newAchievements) { this.newAchievements = newAchievements; }
}
