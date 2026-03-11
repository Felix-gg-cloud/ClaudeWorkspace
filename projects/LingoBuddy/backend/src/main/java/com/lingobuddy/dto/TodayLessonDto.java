package com.lingobuddy.dto;

import java.util.List;

public class TodayLessonDto {
    private Long lessonId;
    private String title;
    private String description;
    private int dayIndex;
    private int completedCount;
    private int totalCount;
    private boolean allCompleted;
    private List<TaskDto> tasks;

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public boolean isAllCompleted() { return allCompleted; }
    public void setAllCompleted(boolean allCompleted) { this.allCompleted = allCompleted; }
    public List<TaskDto> getTasks() { return tasks; }
    public void setTasks(List<TaskDto> tasks) { this.tasks = tasks; }
}
