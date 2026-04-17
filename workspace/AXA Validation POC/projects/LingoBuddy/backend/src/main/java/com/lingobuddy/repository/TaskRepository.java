package com.lingobuddy.repository;

import com.lingobuddy.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByLessonIdOrderBySortOrderAsc(Long lessonId);
    Optional<Task> findByTaskCode(String taskCode);
    boolean existsByTaskCode(String taskCode);
}
