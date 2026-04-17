package com.eqa.repository;

import com.eqa.entity.CampDefeated;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CampDefeatedRepository extends JpaRepository<CampDefeated, Long> {
    List<CampDefeated> findByUserIdAndChapterCode(Long userId, String chapterCode);
    boolean existsByUserIdAndEncounterId(Long userId, String encounterId);
    long countByUserIdAndChapterCode(Long userId, String chapterCode);
}
