package com.lingobuddy.repository;

import com.lingobuddy.entity.LevelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LevelConfigRepository extends JpaRepository<LevelConfig, Long> {
    Optional<LevelConfig> findByLevel(int level);
    List<LevelConfig> findAllByOrderByLevelAsc();
    boolean existsByLevel(int level);
}
