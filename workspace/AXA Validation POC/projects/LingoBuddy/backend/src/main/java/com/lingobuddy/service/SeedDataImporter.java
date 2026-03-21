package com.lingobuddy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingobuddy.entity.*;
import com.lingobuddy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@Order(2)
public class SeedDataImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataImporter.class);
    private final ObjectMapper objectMapper;
    private final SeedImportLogRepository logRepo;
    private final LevelConfigRepository levelRepo;
    private final StageRepository stageRepo;
    private final LessonRepository lessonRepo;
    private final TaskRepository taskRepo;
    private final AchievementRepository achievementRepo;

    public SeedDataImporter(ObjectMapper objectMapper, SeedImportLogRepository logRepo,
                            LevelConfigRepository levelRepo, StageRepository stageRepo,
                            LessonRepository lessonRepo, TaskRepository taskRepo,
                            AchievementRepository achievementRepo) {
        this.objectMapper = objectMapper;
        this.logRepo = logRepo;
        this.levelRepo = levelRepo;
        this.stageRepo = stageRepo;
        this.lessonRepo = lessonRepo;
        this.taskRepo = taskRepo;
        this.achievementRepo = achievementRepo;
    }

    @Override
    public void run(String... args) {
        importLevels();
        importStages();
        importLessonsAndTasks();
        importAchievements();
    }

    private void importLevels() {
        if (logRepo.existsByFileName("levels.json")) return;
        try {
            InputStream is = new ClassPathResource("seed/levels.json").getInputStream();
            List<Map<String, Object>> levels = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> m : levels) {
                LevelConfig lc = new LevelConfig();
                lc.setLevel((int) m.get("level"));
                lc.setRequiredXp((int) m.get("requiredXp"));
                lc.setTitle((String) m.get("title"));
                lc.setDescription((String) m.get("description"));
                levelRepo.save(lc);
            }
            logRepo.save(new SeedImportLog("levels.json"));
            log.info("Imported levels.json");
        } catch (Exception e) {
            log.error("Failed to import levels.json", e);
        }
    }

    private void importStages() {
        if (logRepo.existsByFileName("stages.json")) return;
        try {
            InputStream is = new ClassPathResource("seed/stages.json").getInputStream();
            List<Map<String, Object>> stages = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> m : stages) {
                Stage s = new Stage();
                s.setLevelId((int) m.get("levelId"));
                s.setName((String) m.get("name"));
                s.setSortOrder((int) m.get("sortOrder"));
                stageRepo.save(s);
            }
            logRepo.save(new SeedImportLog("stages.json"));
            log.info("Imported stages.json");
        } catch (Exception e) {
            log.error("Failed to import stages.json", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void importLessonsAndTasks() {
        if (logRepo.existsByFileName("lessons.json")) return;
        try {
            InputStream is = new ClassPathResource("seed/lessons.json").getInputStream();
            List<Map<String, Object>> lessons = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> m : lessons) {
                Lesson lesson = new Lesson();
                lesson.setStageId(((Number) m.get("stageId")).longValue());
                lesson.setDayIndex((int) m.get("dayIndex"));
                lesson.setTitle((String) m.get("title"));
                lesson.setDescription((String) m.get("description"));
                lesson = lessonRepo.save(lesson);

                List<Map<String, Object>> tasks = (List<Map<String, Object>>) m.get("tasks");
                if (tasks != null) {
                    for (Map<String, Object> t : tasks) {
                        if (taskRepo.existsByTaskCode((String) t.get("taskCode"))) continue;
                        Task task = new Task();
                        task.setLessonId(lesson.getId());
                        task.setTaskCode((String) t.get("taskCode"));
                        task.setType(TaskType.valueOf((String) t.get("type")));
                        task.setQuestion((String) t.get("question"));
                        task.setOptions(t.get("options") != null ? objectMapper.writeValueAsString(t.get("options")) : null);
                        task.setCorrectAnswer((String) t.get("correctAnswer"));
                        task.setExplanation((String) t.get("explanation"));
                        task.setXpReward(t.containsKey("xpReward") ? (int) t.get("xpReward") : 10);
                        task.setSortOrder(t.containsKey("sortOrder") ? (int) t.get("sortOrder") : 0);
                        taskRepo.save(task);
                    }
                }
            }
            logRepo.save(new SeedImportLog("lessons.json"));
            log.info("Imported lessons.json with tasks");
        } catch (Exception e) {
            log.error("Failed to import lessons.json", e);
        }
    }

    private void importAchievements() {
        if (logRepo.existsByFileName("achievements.json")) return;
        try {
            InputStream is = new ClassPathResource("seed/achievements.json").getInputStream();
            List<Map<String, Object>> achievements = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> m : achievements) {
                if (achievementRepo.existsByCode((String) m.get("code"))) continue;
                Achievement a = new Achievement();
                a.setCode((String) m.get("code"));
                a.setName((String) m.get("name"));
                a.setDescription((String) m.get("description"));
                a.setIcon((String) m.get("icon"));
                a.setConditionType((String) m.get("conditionType"));
                a.setConditionValue((int) m.get("conditionValue"));
                achievementRepo.save(a);
            }
            logRepo.save(new SeedImportLog("achievements.json"));
            log.info("Imported achievements.json");
        } catch (Exception e) {
            log.error("Failed to import achievements.json", e);
        }
    }
}
