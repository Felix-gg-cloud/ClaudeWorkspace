package com.lingobuddy.service;

import com.lingobuddy.dto.*;
import com.lingobuddy.entity.*;
import com.lingobuddy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LessonService {

    private final StageRepository stageRepository;
    private final LessonRepository lessonRepository;
    private final TaskRepository taskRepository;
    private final TaskProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final CheckinService checkinService;
    private final LevelService levelService;
    private final AchievementService achievementService;

    public LessonService(StageRepository stageRepository, LessonRepository lessonRepository,
                         TaskRepository taskRepository, TaskProgressRepository progressRepository,
                         UserRepository userRepository, CheckinService checkinService,
                         LevelService levelService, AchievementService achievementService) {
        this.stageRepository = stageRepository;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.checkinService = checkinService;
        this.levelService = levelService;
        this.achievementService = achievementService;
    }

    public TodayLessonDto getTodayLesson(User user) {
        // Get all accessible stages for current level
        List<Stage> stages = stageRepository.findByLevelIdLessThanEqualOrderBySortOrderAsc(user.getCurrentLevel());
        if (stages.isEmpty()) return null;

        List<Long> stageIds = stages.stream().map(Stage::getId).collect(Collectors.toList());
        List<Lesson> allLessons = lessonRepository.findByStageIdInOrderByDayIndexAsc(stageIds);
        if (allLessons.isEmpty()) return null;

        // Calculate day index from startDate
        long daysSinceStart = ChronoUnit.DAYS.between(user.getStartDate(), LocalDate.now()) + 1;
        // Clamp to available lessons (cycle if exceeded)
        int lessonIndex = (int) ((daysSinceStart - 1) % allLessons.size());
        Lesson todayLesson = allLessons.get(lessonIndex);

        return buildLessonDto(todayLesson, user.getId(), (int) daysSinceStart);
    }

    private TodayLessonDto buildLessonDto(Lesson lesson, Long userId, int dayIndex) {
        List<Task> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lesson.getId());
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        List<TaskProgress> progress = progressRepository.findByUserIdAndTaskIdIn(userId, taskIds);
        Set<Long> completedIds = progress.stream()
            .filter(TaskProgress::isCompleted)
            .map(TaskProgress::getTaskId)
            .collect(Collectors.toSet());

        List<TaskDto> taskDtos = tasks.stream().map(t -> {
            TaskDto dto = new TaskDto();
            dto.setId(t.getId());
            dto.setTaskCode(t.getTaskCode());
            dto.setType(t.getType());
            dto.setQuestion(t.getQuestion());
            dto.setOptions(t.getOptions());
            dto.setCorrectAnswer(t.getCorrectAnswer());
            dto.setExplanation(t.getExplanation());
            dto.setXpReward(t.getXpReward());
            dto.setSortOrder(t.getSortOrder());
            dto.setCompleted(completedIds.contains(t.getId()));
            return dto;
        }).collect(Collectors.toList());

        TodayLessonDto dto = new TodayLessonDto();
        dto.setLessonId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setDescription(lesson.getDescription());
        dto.setDayIndex(dayIndex);
        dto.setTasks(taskDtos);
        dto.setTotalCount(tasks.size());
        dto.setCompletedCount((int) completedIds.size());
        dto.setAllCompleted(completedIds.size() == tasks.size());
        return dto;
    }

    @Transactional
    public TaskCompleteResult completeTask(User user, Long taskId) {
        TaskCompleteResult result = new TaskCompleteResult();

        // Idempotent check
        var existing = progressRepository.findByUserIdAndTaskId(user.getId(), taskId);
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));

        int xpGained = 0;
        if (existing.isEmpty()) {
            progressRepository.save(new TaskProgress(user.getId(), taskId));
            xpGained = task.getXpReward();
            user.setTotalXp(user.getTotalXp() + xpGained);
            userRepository.save(user);
        }

        result.setXpGained(xpGained);
        result.setTotalXp(user.getTotalXp());

        // Check level up
        int oldLevel = user.getCurrentLevel();
        boolean leveled = levelService.checkAndUpgrade(user);
        result.setLevelUp(leveled);
        if (leveled) {
            result.setNewLevel(user.getCurrentLevel());
            var config = levelService.getLevelConfig(user.getCurrentLevel());
            result.setNewLevelTitle(config != null ? config.getTitle() : "Level " + user.getCurrentLevel());
        }

        // Check if all today's lesson tasks are complete → auto checkin
        Lesson lesson = lessonRepository.findById(task.getLessonId()).orElse(null);
        if (lesson != null) {
            List<Task> lessonTasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lesson.getId());
            List<Long> taskIds = lessonTasks.stream().map(Task::getId).collect(Collectors.toList());
            List<TaskProgress> allProgress = progressRepository.findByUserIdAndTaskIdIn(user.getId(), taskIds);
            long completedCount = allProgress.stream().filter(TaskProgress::isCompleted).count();

            if (completedCount == lessonTasks.size()) {
                result.setLessonCompleted(true);
                DailyCheckin checkin = checkinService.performCheckin(user);
                result.setCheckinXp(checkin.getXpEarned());
                result.setCheckinCoins(checkin.getCoinsEarned());
                result.setStreak(checkin.getStreak());
                result.setCoinsGained(checkin.getCoinsEarned());

                // Re-check level after checkin XP
                if (!leveled) {
                    leveled = levelService.checkAndUpgrade(user);
                    result.setLevelUp(leveled);
                    if (leveled) {
                        result.setNewLevel(user.getCurrentLevel());
                        var config = levelService.getLevelConfig(user.getCurrentLevel());
                        result.setNewLevelTitle(config != null ? config.getTitle() : "Level " + user.getCurrentLevel());
                    }
                }
                result.setTotalXp(user.getTotalXp());

                // Check achievements
                List<AchievementDto> newAchievements = achievementService.checkAndUnlock(user);
                result.setNewAchievements(newAchievements);
            }
        }

        return result;
    }
}
