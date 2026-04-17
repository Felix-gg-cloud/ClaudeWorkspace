package com.lingobuddy.service;

import com.lingobuddy.entity.DailyCheckin;
import com.lingobuddy.entity.User;
import com.lingobuddy.repository.DailyCheckinRepository;
import com.lingobuddy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CheckinService {

    private static final int DAILY_BONUS_XP = 20;
    private static final int STREAK_BONUS_XP = 10;
    private static final int STREAK_BONUS_THRESHOLD = 3;

    private final DailyCheckinRepository checkinRepository;
    private final UserRepository userRepository;

    public CheckinService(DailyCheckinRepository checkinRepository, UserRepository userRepository) {
        this.checkinRepository = checkinRepository;
        this.userRepository = userRepository;
    }

    public DailyCheckin performCheckin(User user) {
        LocalDate today = LocalDate.now();
        // Idempotent: return existing if already checked in
        var existing = checkinRepository.findByUserIdAndCheckinDate(user.getId(), today);
        if (existing.isPresent()) {
            return existing.get();
        }

        int streak = calculateStreak(user.getId(), today);
        int bonusXp = DAILY_BONUS_XP + (streak >= STREAK_BONUS_THRESHOLD ? STREAK_BONUS_XP : 0);
        int coins = ThreadLocalRandom.current().nextInt(1, 11);

        DailyCheckin checkin = new DailyCheckin(user.getId(), today);
        checkin.setXpEarned(bonusXp);
        checkin.setCoinsEarned(coins);
        checkin.setStreak(streak);

        // Update user stats
        user.setTotalXp(user.getTotalXp() + bonusXp);
        user.setCoins(user.getCoins() + coins);
        userRepository.save(user);

        return checkinRepository.save(checkin);
    }

    public int calculateStreak(Long userId, LocalDate today) {
        List<DailyCheckin> history = checkinRepository.findByUserIdOrderByCheckinDateDesc(userId);
        int streak = 1; // Today counts as 1
        LocalDate expected = today.minusDays(1);
        for (DailyCheckin c : history) {
            if (c.getCheckinDate().equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (c.getCheckinDate().isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    public int getCurrentStreak(Long userId) {
        LocalDate today = LocalDate.now();
        var todayCheckin = checkinRepository.findByUserIdAndCheckinDate(userId, today);
        if (todayCheckin.isPresent()) {
            return todayCheckin.get().getStreak();
        }
        // Check yesterday
        var yesterday = checkinRepository.findByUserIdAndCheckinDate(userId, today.minusDays(1));
        return yesterday.map(DailyCheckin::getStreak).orElse(0);
    }

    public List<DailyCheckin> getCalendar(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return checkinRepository.findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(userId, start, end);
    }
}
