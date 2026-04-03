package com.ll.user.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_stats", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stat_date"}))
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "tasks_completed")
    private Integer tasksCompleted = 0;

    @Column(name = "correct_count")
    private Integer correctCount = 0;

    @Column(name = "wrong_count")
    private Integer wrongCount = 0;

    @Column(name = "words_learned")
    private Integer wordsLearned = 0;

    @Column(name = "study_minutes")
    private Integer studyMinutes = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }

    public Integer getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(Integer tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public Integer getCorrectCount() { return correctCount; }
    public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }

    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }

    public Integer getWordsLearned() { return wordsLearned; }
    public void setWordsLearned(Integer wordsLearned) { this.wordsLearned = wordsLearned; }

    public Integer getStudyMinutes() { return studyMinutes; }
    public void setStudyMinutes(Integer studyMinutes) { this.studyMinutes = studyMinutes; }
}
