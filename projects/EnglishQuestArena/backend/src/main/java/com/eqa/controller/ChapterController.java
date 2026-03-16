package com.eqa.controller;

import com.eqa.entity.*;
import com.eqa.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterRepository chapterRepo;
    private final ChapterProgressRepository progressRepo;
    private final UserRepository userRepo;
    private final CampDefeatedRepository campDefeatedRepo;
    private final QuestDayProgressRepository dayProgressRepo;
    private final LessonRepository lessonRepo;

    public ChapterController(ChapterRepository chapterRepo,
                             ChapterProgressRepository progressRepo,
                             UserRepository userRepo,
                             CampDefeatedRepository campDefeatedRepo,
                             QuestDayProgressRepository dayProgressRepo,
                             LessonRepository lessonRepo) {
        this.chapterRepo = chapterRepo;
        this.progressRepo = progressRepo;
        this.userRepo = userRepo;
        this.campDefeatedRepo = campDefeatedRepo;
        this.dayProgressRepo = dayProgressRepo;
        this.lessonRepo = lessonRepo;
    }

    @GetMapping
    public List<Map<String, Object>> list(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<Chapter> chapters = chapterRepo.findAllByOrderByOrderIndexAsc();
        Map<String, ChapterProgress> progressMap = progressRepo.findByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(ChapterProgress::getChapterCode, p -> p));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Chapter ch : chapters) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", ch.getCode());
            item.put("cefrLevel", ch.getCefrLevel());
            item.put("titleEn", ch.getTitleEn());
            item.put("titleZh", ch.getTitleZh());
            item.put("orderIndex", ch.getOrderIndex());
            item.put("days", ch.getDays());

            ChapterProgress cp = progressMap.get(ch.getCode());
            if (cp != null) {
                item.put("phase", cp.getPhase());
                item.put("bossDefeated", cp.isBossDefeated());
            } else {
                item.put("phase", "locked");
                item.put("bossDefeated", false);
            }

            // campDefeated encounter IDs
            List<String> campIds = campDefeatedRepo.findByUserIdAndChapterCode(user.getId(), ch.getCode())
                    .stream().map(CampDefeated::getEncounterId).toList();
            item.put("campDefeated", campIds);

            // questDaysCompleted dayIndex list
            List<Lesson> lessons = lessonRepo.findByChapterCodeOrderByDayIndexAsc(ch.getCode());
            List<String> lessonCodes = lessons.stream().map(Lesson::getCode).toList();
            List<Integer> completedDays = dayProgressRepo.findByUserIdAndLessonCodeIn(user.getId(), lessonCodes)
                    .stream()
                    .filter(QuestDayProgress::isCompleted)
                    .map(p -> lessons.stream()
                            .filter(l -> l.getCode().equals(p.getLessonCode()))
                            .findFirst().map(Lesson::getDayIndex).orElse(0))
                    .filter(d -> d > 0)
                    .toList();
            item.put("questDaysCompleted", completedDays);

            result.add(item);
        }
        return result;
    }
}
