package com.ll.user.service;

import com.ll.user.entity.DailyStats;
import com.ll.user.repository.DailyStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class StatsService {

    private final DailyStatsRepository statsRepo;

    public StatsService(DailyStatsRepository statsRepo) {
        this.statsRepo = statsRepo;
    }

    public Map<String, Object> getToday(Long userId) {
        LocalDate today = LocalDate.now();
        DailyStats stats = statsRepo.findByUserIdAndStatDate(userId, today)
                .orElse(emptyStats(userId, today));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today);
        result.put("tasksCompleted", stats.getTasksCompleted());
        result.put("correctCount", stats.getCorrectCount());
        result.put("wrongCount", stats.getWrongCount());
        result.put("wordsLearned", stats.getWordsLearned());
        result.put("studyMinutes", stats.getStudyMinutes());
        return result;
    }

    public List<Map<String, Object>> getRange(Long userId, String fromStr, String toStr) {
        LocalDate from = fromStr != null ? LocalDate.parse(fromStr) : LocalDate.now().minusDays(6);
        LocalDate to = toStr != null ? LocalDate.parse(toStr) : LocalDate.now();
        List<DailyStats> statsList = statsRepo.findByRange(userId, from, to);

        // 填充缺失日期
        Map<LocalDate, DailyStats> map = new LinkedHashMap<>();
        for (DailyStats s : statsList) {
            map.put(s.getStatDate(), s);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DailyStats s = map.getOrDefault(d, emptyStats(userId, d));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d);
            item.put("tasksCompleted", s.getTasksCompleted());
            item.put("correctCount", s.getCorrectCount());
            item.put("wrongCount", s.getWrongCount());
            item.put("wordsLearned", s.getWordsLearned());
            item.put("studyMinutes", s.getStudyMinutes());
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void record(Long userId, int correctCount, int wrongCount, int wordsLearned, int studyMinutes) {
        LocalDate today = LocalDate.now();
        DailyStats stats = statsRepo.findByUserIdAndStatDate(userId, today)
                .orElseGet(() -> {
                    DailyStats s = new DailyStats();
                    s.setUserId(userId);
                    s.setStatDate(today);
                    return s;
                });

        stats.setTasksCompleted(stats.getTasksCompleted() + 1);
        stats.setCorrectCount(stats.getCorrectCount() + correctCount);
        stats.setWrongCount(stats.getWrongCount() + wrongCount);
        stats.setWordsLearned(stats.getWordsLearned() + wordsLearned);
        stats.setStudyMinutes(stats.getStudyMinutes() + studyMinutes);
        statsRepo.save(stats);
    }

    public Map<String, Object> getStreak(Long userId) {
        List<LocalDate> activeDates = statsRepo.findActiveDates(userId);

        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (LocalDate d : activeDates) {
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (d.equals(expected.plusDays(1))) {
                // 今天还没学习，从昨天开始算
                expected = d.minusDays(1);
                streak++;
            } else {
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("streak", streak);
        result.put("totalDays", activeDates.size());
        return result;
    }

    private DailyStats emptyStats(Long userId, LocalDate date) {
        DailyStats s = new DailyStats();
        s.setUserId(userId);
        s.setStatDate(date);
        return s;
    }
}
