package com.lingobuddy.repository;

import com.lingobuddy.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByLevelIdOrderBySortOrderAsc(int levelId);
    List<Stage> findByLevelIdLessThanEqualOrderBySortOrderAsc(int levelId);
}
