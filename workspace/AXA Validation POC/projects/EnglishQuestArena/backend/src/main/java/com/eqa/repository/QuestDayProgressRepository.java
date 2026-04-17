package com.eqa.repository;

import com.eqa.entity.QuestDayProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuestDayProgressRepository extends JpaRepository<QuestDayProgress, Long> {
    List<QuestDayProgress> findByUserIdAndLessonCodeIn(Long userId, List<String> lessonCodes);
    Optional<QuestDayProgress> findByUserIdAndLessonCode(Long userId, String lessonCode);
}
