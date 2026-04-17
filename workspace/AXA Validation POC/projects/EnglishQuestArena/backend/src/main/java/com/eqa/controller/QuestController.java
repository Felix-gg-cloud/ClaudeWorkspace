package com.eqa.controller;

import com.eqa.entity.Lesson;
import com.eqa.entity.QuestDayProgress;
import com.eqa.entity.User;
import com.eqa.repository.LessonRepository;
import com.eqa.repository.QuestDayProgressRepository;
import com.eqa.repository.UserRepository;
import com.eqa.service.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quest")
public class QuestController {

    private final LessonRepository lessonRepo;
    private final QuestDayProgressRepository dayProgressRepo;
    private final UserRepository userRepo;
    private final TaskService taskService;

    public QuestController(LessonRepository lessonRepo,
                           QuestDayProgressRepository dayProgressRepo,
                           UserRepository userRepo,
                           TaskService taskService) {
        this.lessonRepo = lessonRepo;
        this.dayProgressRepo = dayProgressRepo;
        this.userRepo = userRepo;
        this.taskService = taskService;
    }

    /**
     * 获取某章节的课时列表（含完成状态）
     */
    @GetMapping("/{chapterCode}/lessons")
    public List<Map<String, Object>> lessons(@PathVariable String chapterCode, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<Lesson> lessons = lessonRepo.findByChapterCodeOrderByDayIndexAsc(chapterCode);
        List<String> lessonCodes = lessons.stream().map(Lesson::getCode).collect(Collectors.toList());
        Map<String, QuestDayProgress> progressMap = dayProgressRepo
                .findByUserIdAndLessonCodeIn(user.getId(), lessonCodes)
                .stream()
                .collect(Collectors.toMap(QuestDayProgress::getLessonCode, p -> p));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Lesson l : lessons) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", l.getCode());
            item.put("chapterCode", l.getChapterCode());
            item.put("dayIndex", l.getDayIndex());
            item.put("titleEn", l.getTitleEn());
            item.put("titleZh", l.getTitleZh());
            item.put("estimatedMinutes", l.getEstimatedMinutes());
            QuestDayProgress p = progressMap.get(l.getCode());
            item.put("completed", p != null && p.isCompleted());
            result.add(item);
        }
        return result;
    }

    /**
     * 获取某天的动态生成任务
     */
    @GetMapping("/{chapterCode}/day/{dayIndex}/tasks")
    public List<Map<String, Object>> dayTasks(@PathVariable String chapterCode,
                                               @PathVariable int dayIndex) {
        return taskService.generateDayTasks(chapterCode, dayIndex);
    }
}
