package com.lingobuddy.service;

import com.lingobuddy.dto.AchievementDto;
import com.lingobuddy.entity.*;
import com.lingobuddy.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final DailyCheckinRepository checkinRepository;
    private final TaskProgressRepository progressRepository;
    private final CheckinService checkinService;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              DailyCheckinRepository checkinRepository,
                              TaskProgressRepository progressRepository,
                              CheckinService checkinService) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.checkinRepository = checkinRepository;
        this.progressRepository = progressRepository;
        this.checkinService = checkinService;
    }

    public List<AchievementDto> checkAndUnlock(User user) {
        List<Achievement> all = achievementRepository.findAll();
        Set<Long> unlocked = userAchievementRepository.findByUserId(user.getId())
            .stream().map(UserAchievement::getAchievementId).collect(Collectors.toSet());

        List<AchievementDto> newlyUnlocked = new ArrayList<>();

        for (Achievement a : all) {
            if (unlocked.contains(a.getId())) continue;

            boolean met = switch (a.getConditionType()) {
                case "FIRST_LOGIN" -> true;
                case "STREAK_N" -> checkinService.getCurrentStreak(user.getId()) >= a.getConditionValue();
                case "LEVEL_N" -> user.getCurrentLevel() >= a.getConditionValue();
                case "TOTAL_TASKS_N" -> progressRepository.countByUserIdAndCompletedTrue(user.getId()) >= a.getConditionValue();
                case "TOTAL_CHECKIN_N" -> checkinRepository.countByUserId(user.getId()) >= a.getConditionValue();
                default -> false;
            };

            if (met) {
                userAchievementRepository.save(new UserAchievement(user.getId(), a.getId()));
                newlyUnlocked.add(toDto(a, true));
            }
        }
        return newlyUnlocked;
    }

    public List<AchievementDto> getAllForUser(Long userId) {
        List<Achievement> all = achievementRepository.findAll();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);
        Set<Long> unlockedIds = userAchievements.stream()
            .map(UserAchievement::getAchievementId).collect(Collectors.toSet());

        return all.stream().map(a -> {
            AchievementDto dto = toDto(a, unlockedIds.contains(a.getId()));
            if (dto.isUnlocked()) {
                userAchievements.stream()
                    .filter(ua -> ua.getAchievementId().equals(a.getId()))
                    .findFirst()
                    .ifPresent(ua -> dto.setUnlockedAt(ua.getUnlockedAt().toString()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private AchievementDto toDto(Achievement a, boolean unlocked) {
        AchievementDto dto = new AchievementDto();
        dto.setId(a.getId());
        dto.setCode(a.getCode());
        dto.setName(a.getName());
        dto.setDescription(a.getDescription());
        dto.setIcon(a.getIcon());
        dto.setConditionType(a.getConditionType());
        dto.setConditionValue(a.getConditionValue());
        dto.setUnlocked(unlocked);
        return dto;
    }
}
