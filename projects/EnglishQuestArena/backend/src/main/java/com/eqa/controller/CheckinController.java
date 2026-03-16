package com.eqa.controller;

import com.eqa.entity.CheckinRecord;
import com.eqa.entity.User;
import com.eqa.repository.CheckinRecordRepository;
import com.eqa.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinRecordRepository checkinRepo;
    private final UserRepository userRepo;

    public CheckinController(CheckinRecordRepository checkinRepo, UserRepository userRepo) {
        this.checkinRepo = checkinRepo;
        this.userRepo = userRepo;
    }

    /**
     * 今日签到
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> checkin(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        LocalDate today = LocalDate.now();

        // 检查是否已签到
        if (checkinRepo.existsByUserIdAndCheckinDate(user.getId(), today)) {
            return ResponseEntity.ok(Map.of("message", "今天已签到", "streak", user.getStreak()));
        }

        // 计算连续签到
        LocalDate yesterday = today.minusDays(1);
        Optional<CheckinRecord> yesterdayRecord = checkinRepo.findByUserIdAndCheckinDate(user.getId(), yesterday);
        int newStreak = yesterdayRecord.map(r -> r.getStreak() + 1).orElse(1);

        // 签到奖励：基础10XP + 连续天数加成
        int xpBonus = 10 + Math.min(newStreak, 7) * 2;
        int coinBonus = 5 + Math.min(newStreak, 7);

        CheckinRecord record = new CheckinRecord();
        record.setUserId(user.getId());
        record.setCheckinDate(today);
        record.setStreak(newStreak);
        record.setXpEarned(xpBonus);
        record.setCoinsEarned(coinBonus);
        checkinRepo.save(record);

        // 更新用户数据
        user.setStreak(newStreak);
        user.setTotalCheckins(user.getTotalCheckins() + 1);
        user.setTotalXp(user.getTotalXp() + xpBonus);
        user.setCoins(user.getCoins() + coinBonus);
        userRepo.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("streak", newStreak);
        result.put("xpEarned", xpBonus);
        result.put("coinsEarned", coinBonus);
        result.put("totalXp", user.getTotalXp());
        result.put("coins", user.getCoins());
        return ResponseEntity.ok(result);
    }

    /**
     * 签到记录（最近30天）
     */
    @GetMapping("/history")
    public List<Map<String, Object>> history(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<CheckinRecord> records = checkinRepo.findByUserIdOrderByCheckinDateDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(records.size(), 30);
        for (int i = 0; i < limit; i++) {
            CheckinRecord r = records.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", r.getCheckinDate().toString());
            m.put("streak", r.getStreak());
            m.put("xpEarned", r.getXpEarned());
            m.put("coinsEarned", r.getCoinsEarned());
            result.add(m);
        }
        return result;
    }
}
