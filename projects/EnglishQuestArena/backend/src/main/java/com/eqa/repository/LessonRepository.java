package com.eqa.repository;

import com.eqa.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, String> {
    List<Lesson> findByChapterCodeOrderByDayIndexAsc(String chapterCode);
}
