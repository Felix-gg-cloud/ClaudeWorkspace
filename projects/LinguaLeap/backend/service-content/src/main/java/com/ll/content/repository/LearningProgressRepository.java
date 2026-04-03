package com.ll.content.repository;

import com.ll.content.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {

    Optional<LearningProgress> findByUserIdAndKpId(Long userId, Long kpId);

    List<LearningProgress> findByUserIdAndUnitId(Long userId, Long unitId);

    List<LearningProgress> findByUserIdAndLevelId(Long userId, Long levelId);

    long countByUserIdAndLevelIdAndStatus(Long userId, Long levelId, String status);

    long countByUserIdAndUnitIdAndStatus(Long userId, Long unitId, String status);

    long countByUserIdAndLevelId(Long userId, Long levelId);

    long countByUserIdAndUnitId(Long userId, Long unitId);
}
