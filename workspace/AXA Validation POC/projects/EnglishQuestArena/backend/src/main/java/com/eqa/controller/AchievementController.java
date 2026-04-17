package com.eqa.controller;

import com.eqa.entity.SkillUnlock;
import com.eqa.entity.User;
import com.eqa.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final UserRepository userRepo;
    private final SkillUnlockRepository skillUnlockRepo;
    private final SrsRecordRepository srsRecordRepo;
    // 成就定义（与前端 achievements.ts 对齐）
    private static final List<Map<String, Object>> ACHIEVEMENT_DEFS = List.of(
        achDef("first_word", "初识英语", "First Word", "学会第一个单词", "Learn your first word", "📖", "words_learned", 1, 10, 5),
        achDef("vocab_10", "小小词库", "Word Collector", "学会10个单词", "Learn 10 words", "📚", "words_learned", 10, 30, 15),
        achDef("vocab_50", "词汇达人", "Vocabulary Pro", "学会50个单词", "Learn 50 words", "🎓", "words_learned", 50, 100, 50),
        achDef("streak_3", "三日坚持", "3-Day Streak", "连续学习3天", "Study for 3 consecutive days", "🔥", "streak_days", 3, 30, 15),
        achDef("streak_7", "一周勇士", "Weekly Warrior", "连续学习7天", "Study for 7 consecutive days", "⚔️", "streak_days", 7, 50, 25),
        achDef("streak_30", "月度之星", "Monthly Star", "连续学习30天", "Study for 30 consecutive days", "⭐", "streak_days", 30, 200, 100),
        achDef("boss_1", "初战告捷", "First Victory", "击败第一个Boss", "Defeat your first boss", "👑", "boss_defeated", 1, 50, 25),
        achDef("boss_3", "Boss猎人", "Boss Hunter", "击败3个Boss", "Defeat 3 bosses", "🏆", "boss_defeated", 3, 100, 50),
        achDef("tasks_100", "百题斩", "Century Slayer", "完成100道题", "Complete 100 tasks", "💯", "tasks_completed", 100, 50, 25),
        achDef("tasks_500", "题海战术", "Task Master", "完成500道题", "Complete 500 tasks", "🌊", "tasks_completed", 500, 150, 75),
        achDef("combo_10", "连击达人", "Combo King", "单次连击10次", "Achieve a 10-hit combo", "🎯", "perfect_combo", 10, 80, 40),
        achDef("exam_pre_a1", "Pre-A1通关", "Pre-A1 Clear", "通过Pre-A1考试", "Pass the Pre-A1 exam", "🎖️", "exam_passed", 1, 100, 50),
        achDef("mistakes_50", "知错能改", "Error Fixer", "复习50个错题", "Review 50 mistakes", "🔄", "mistakes_reviewed", 50, 60, 30)
    );

    public AchievementController(UserRepository userRepo,
                                  SkillUnlockRepository skillUnlockRepo,
                                  SrsRecordRepository srsRecordRepo) {
        this.userRepo = userRepo;
        this.skillUnlockRepo = skillUnlockRepo;
        this.srsRecordRepo = srsRecordRepo;
    }

    /**
     * 获取成就列表（含当前进度）
     */
    @GetMapping
    public List<Map<String, Object>> list(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<SkillUnlock> unlocks = skillUnlockRepo.findByUserId(user.getId());
        Set<String> unlockedCodes = new HashSet<>();
        for (SkillUnlock u : unlocks) {
            unlockedCodes.add(u.getSkillCode());
        }

        // 收集当前统计（简化版本)
        long wordsLearned = srsRecordRepo.countByUserId(user.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> def : ACHIEVEMENT_DEFS) {
            Map<String, Object> item = new LinkedHashMap<>(def);
            String code = (String) def.get("code");
            item.put("unlocked", unlockedCodes.contains(code));

            // 计算当前值
            String condType = (String) def.get("conditionType");
            int currentValue = switch (condType) {
                case "words_learned" -> (int) wordsLearned;
                default -> 0;
            };
            item.put("currentValue", currentValue);
            result.add(item);
        }
        return result;
    }

    /**
     * 检查并解锁成就
     */
    @PostMapping("/check")
    public ResponseEntity<?> check(@RequestBody CheckRequest req, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();

        List<Map<String, Object>> newlyUnlocked = new ArrayList<>();
        for (Map<String, Object> def : ACHIEVEMENT_DEFS) {
            String code = (String) def.get("code");
            String condType = (String) def.get("conditionType");
            int threshold = (int) def.get("threshold");

            if (skillUnlockRepo.existsByUserIdAndSkillCode(user.getId(), code)) continue;
            if (!condType.equals(req.conditionType())) continue;
            if (req.value() < threshold) continue;

            // 解锁
            SkillUnlock unlock = new SkillUnlock();
            unlock.setUserId(user.getId());
            unlock.setSkillCode(code);
            skillUnlockRepo.save(unlock);

            int xpReward = (int) def.get("xpReward");
            int coinReward = (int) def.get("coinReward");
            user.setTotalXp(user.getTotalXp() + xpReward);
            user.setCoins(user.getCoins() + coinReward);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", code);
            item.put("nameZh", def.get("nameZh"));
            item.put("xpReward", xpReward);
            item.put("coinReward", coinReward);
            newlyUnlocked.add(item);
        }

        if (!newlyUnlocked.isEmpty()) {
            userRepo.save(user);
        }

        return ResponseEntity.ok(Map.of(
                "newlyUnlocked", newlyUnlocked,
                "totalXp", user.getTotalXp(),
                "coins", user.getCoins()
        ));
    }

    private static Map<String, Object> achDef(String code, String nameZh, String nameEn,
                                               String descZh, String descEn, String icon,
                                               String condType, int threshold, int xp, int coins) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("nameZh", nameZh);
        m.put("nameEn", nameEn);
        m.put("descZh", descZh);
        m.put("descEn", descEn);
        m.put("icon", icon);
        m.put("conditionType", condType);
        m.put("threshold", threshold);
        m.put("xpReward", xp);
        m.put("coinReward", coins);
        return m;
    }

    public record CheckRequest(String conditionType, int value) {}
}
