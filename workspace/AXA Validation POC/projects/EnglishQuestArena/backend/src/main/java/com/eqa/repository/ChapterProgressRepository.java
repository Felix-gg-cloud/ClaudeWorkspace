package com.eqa.repository;

import com.eqa.entity.ChapterProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChapterProgressRepository extends JpaRepository<ChapterProgress, Long> {
    List<ChapterProgress> findByUserId(Long userId);
    Optional<ChapterProgress> findByUserIdAndChapterCode(Long userId, String chapterCode);
}
