package com.eqa.controller;

import com.eqa.entity.*;
import com.eqa.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final UserRepository userRepo;
    private final ChapterProgressRepository chapterProgressRepo;
    private final CampDefeatedRepository campDefeatedRepo;
    private final QuestDayProgressRepository dayProgressRepo;
    private final BossAttemptRepository bossAttemptRepo;
    private final ChapterRepository chapterRepo;
    private final LessonRepository lessonRepo;

    public ProgressController(UserRepository userRepo,
                              ChapterProgressRepository chapterProgressRepo,
                              CampDefeatedRepository campDefeatedRepo,
                              QuestDayProgressRepository dayProgressRepo,
                              BossAttemptRepository bossAttemptRepo,
                              ChapterRepository chapterRepo,
                              LessonRepository lessonRepo) {
        this.userRepo = userRepo;
        this.chapterProgressRepo = chapterProgressRepo;
        this.campDefeatedRepo = campDefeatedRepo;
        this.dayProgressRepo = dayProgressRepo;
        this.bossAttemptRepo = bossAttemptRepo;
        this.chapterRepo = chapterRepo;
        this.lessonRepo = lessonRepo;
    }

    /**
     * 提交营地怪物击败
     */
    @PostMapping("/camp")
    @Transactional
    public ResponseEntity<?> campDefeat(@RequestBody CampDefeatRequest req, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();

        if (!campDefeatedRepo.existsByUserIdAndEncounterId(user.getId(), req.encounterId())) {
            CampDefeated cd = new CampDefeated();
            cd.setUserId(user.getId());
            cd.setChapterCode(req.chapterCode());
            cd.setEncounterId(req.encounterId());
            campDefeatedRepo.save(cd);
        }

        // 检查是否达到解锁率 → 推进到 quest 阶段
        long defeated = campDefeatedRepo.countByUserIdAndChapterCode(user.getId(), req.chapterCode());
        Chapter chapter = chapterRepo.findById(req.chapterCode()).orElse(null);
        if (chapter != null) {
            int totalMonsters = req.totalMonsters();
            double rate = totalMonsters > 0 ? (double) defeated / totalMonsters : 0;
            if (rate >= chapter.getCampUnlockRate().doubleValue()) {
                chapterProgressRepo.findByUserIdAndChapterCode(user.getId(), req.chapterCode())
                        .ifPresent(cp -> {
                            if ("camp".equals(cp.getPhase())) {
                                cp.setPhase("quest");
                                chapterProgressRepo.save(cp);
                            }
                        });
            }
        }

        return ResponseEntity.ok(Map.of("defeated", defeated));
    }

    /**
     * 提交每日课时完成
     */
    @PostMapping("/quest-day")
    @Transactional
    public ResponseEntity<?> questDayComplete(@RequestBody QuestDayRequest req, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();

        QuestDayProgress progress = dayProgressRepo
                .findByUserIdAndLessonCode(user.getId(), req.lessonCode())
                .orElseGet(() -> {
                    QuestDayProgress p = new QuestDayProgress();
                    p.setUserId(user.getId());
                    p.setLessonCode(req.lessonCode());
                    return p;
                });
        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        dayProgressRepo.save(progress);

        // 加经验
        user.setTotalXp(user.getTotalXp() + req.xpEarned());
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);

        // 检查该章节所有课时是否完成 → 推进到 boss 阶段
        String chapterCode = req.lessonCode().substring(0, req.lessonCode().lastIndexOf("_D"));
        List<Lesson> lessons = lessonRepo.findByChapterCodeOrderByDayIndexAsc(chapterCode);
        List<String> lessonCodes = lessons.stream().map(Lesson::getCode).toList();
        long completedCount = dayProgressRepo.findByUserIdAndLessonCodeIn(user.getId(), lessonCodes)
                .stream().filter(QuestDayProgress::isCompleted).count();

        if (completedCount >= lessons.size()) {
            chapterProgressRepo.findByUserIdAndChapterCode(user.getId(), chapterCode)
                    .ifPresent(cp -> {
                        if ("quest".equals(cp.getPhase())) {
                            cp.setPhase("boss");
                            chapterProgressRepo.save(cp);
                        }
                    });
        }

        return ResponseEntity.ok(Map.of(
                "totalXp", user.getTotalXp(),
                "allDaysCompleted", completedCount >= lessons.size()
        ));
    }

    /**
     * 提交 Boss 战结果
     */
    @PostMapping("/boss")
    @Transactional
    public ResponseEntity<?> bossResult(@RequestBody BossResultRequest req, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();

        BossAttempt attempt = new BossAttempt();
        attempt.setUserId(user.getId());
        attempt.setBossCode(req.bossCode());
        attempt.setAttemptDate(LocalDate.now());
        attempt.setVictory(req.victory());
        attempt.setBossHpRemaining(req.bossHpRemaining());
        attempt.setPlayerHpRemaining(req.playerHpRemaining());
        attempt.setMaxCombo(req.maxCombo());
        bossAttemptRepo.save(attempt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("victory", req.victory());

        if (req.victory()) {
            // 更新章节进度
            String chapterCode = req.chapterCode();
            chapterProgressRepo.findByUserIdAndChapterCode(user.getId(), chapterCode)
                    .ifPresent(cp -> {
                        cp.setBossDefeated(true);
                        cp.setPhase("completed");
                        chapterProgressRepo.save(cp);
                    });

            // 解锁下一章
            List<Chapter> allChapters = chapterRepo.findAllByOrderByOrderIndexAsc();
            for (int i = 0; i < allChapters.size() - 1; i++) {
                if (allChapters.get(i).getCode().equals(chapterCode)) {
                    String nextCode = allChapters.get(i + 1).getCode();
                    if (chapterProgressRepo.findByUserIdAndChapterCode(user.getId(), nextCode).isEmpty()) {
                        ChapterProgress nextCp = new ChapterProgress();
                        nextCp.setUserId(user.getId());
                        nextCp.setChapterCode(nextCode);
                        nextCp.setPhase("camp");
                        chapterProgressRepo.save(nextCp);
                    }
                    result.put("unlockedChapter", nextCode);
                    break;
                }
            }

            // 加战胜经验
            user.setTotalXp(user.getTotalXp() + req.xpEarned());
            user.setUpdatedAt(LocalDateTime.now());
            userRepo.save(user);
        }

        result.put("totalXp", user.getTotalXp());
        return ResponseEntity.ok(result);
    }

    /**
     * 加经验值（通用）
     */
    @PostMapping("/xp")
    public ResponseEntity<?> addXp(@RequestBody XpRequest req, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        user.setTotalXp(user.getTotalXp() + req.xp());
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("totalXp", user.getTotalXp()));
    }

    // DTO Records
    public record CampDefeatRequest(String chapterCode, String encounterId, int totalMonsters) {}
    public record QuestDayRequest(String lessonCode, int xpEarned) {}
    public record BossResultRequest(String bossCode, String chapterCode, boolean victory,
                                     int bossHpRemaining, int playerHpRemaining, int maxCombo, int xpEarned) {}
    public record XpRequest(int xp) {}
}
