package com.eqa.repository;

import com.eqa.entity.BossPoolTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BossPoolTaskRepository extends JpaRepository<BossPoolTask, Long> {
    List<BossPoolTask> findByBossCode(String bossCode);
    boolean existsByTaskCode(String taskCode);
}
