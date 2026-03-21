package com.lingobuddy.repository;

import com.lingobuddy.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByStageIdOrderByDayIndexAsc(Long stageId);
    List<Lesson> findByStageIdInOrderByDayIndexAsc(List<Long> stageIds);
    Optional<Lesson> findByStageIdAndDayIndex(Long stageId, int dayIndex);
}
