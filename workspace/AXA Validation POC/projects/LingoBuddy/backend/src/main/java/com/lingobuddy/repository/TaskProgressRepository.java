package com.lingobuddy.repository;

import com.lingobuddy.entity.TaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskProgressRepository extends JpaRepository<TaskProgress, Long> {
    Optional<TaskProgress> findByUserIdAndTaskId(Long userId, Long taskId);
    List<TaskProgress> findByUserIdAndTaskIdIn(Long userId, List<Long> taskIds);
    long countByUserIdAndCompletedTrue(Long userId);
}
