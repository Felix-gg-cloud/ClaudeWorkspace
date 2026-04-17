package com.lingobuddy.dto;

public class UserInfo {
    private Long id;
    private String username;
    private int totalXp;
    private int coins;
    private int currentLevel;
    private String levelTitle;
    private int nextLevelXp;
    private int streak;
    private long totalCheckins;
    private long totalTasksCompleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public String getLevelTitle() { return levelTitle; }
    public void setLevelTitle(String levelTitle) { this.levelTitle = levelTitle; }
    public int getNextLevelXp() { return nextLevelXp; }
    public void setNextLevelXp(int nextLevelXp) { this.nextLevelXp = nextLevelXp; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    public long getTotalCheckins() { return totalCheckins; }
    public void setTotalCheckins(long totalCheckins) { this.totalCheckins = totalCheckins; }
    public long getTotalTasksCompleted() { return totalTasksCompleted; }
    public void setTotalTasksCompleted(long totalTasksCompleted) { this.totalTasksCompleted = totalTasksCompleted; }
}
