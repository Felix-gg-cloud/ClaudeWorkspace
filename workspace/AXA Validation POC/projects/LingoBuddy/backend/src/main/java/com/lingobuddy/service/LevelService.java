package com.lingobuddy.service;

import com.lingobuddy.entity.LevelConfig;
import com.lingobuddy.entity.User;
import com.lingobuddy.repository.LevelConfigRepository;
import com.lingobuddy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LevelService {

    private final LevelConfigRepository levelConfigRepository;
    private final UserRepository userRepository;

    public LevelService(LevelConfigRepository levelConfigRepository, UserRepository userRepository) {
        this.levelConfigRepository = levelConfigRepository;
        this.userRepository = userRepository;
    }

    public int calculateLevel(int totalXp) {
        List<LevelConfig> configs = levelConfigRepository.findAllByOrderByLevelAsc();
        int level = 1;
        for (LevelConfig c : configs) {
            if (totalXp >= c.getRequiredXp()) {
                level = c.getLevel();
            } else {
                break;
            }
        }
        return level;
    }

    public LevelConfig getLevelConfig(int level) {
        return levelConfigRepository.findByLevel(level).orElse(null);
    }

    public int getNextLevelXp(int currentLevel) {
        LevelConfig next = levelConfigRepository.findByLevel(currentLevel + 1).orElse(null);
        return next != null ? next.getRequiredXp() : -1; // -1 means max level
    }

    /** Returns true if leveled up */
    public boolean checkAndUpgrade(User user) {
        int newLevel = calculateLevel(user.getTotalXp());
        if (newLevel > user.getCurrentLevel()) {
            user.setCurrentLevel(newLevel);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
