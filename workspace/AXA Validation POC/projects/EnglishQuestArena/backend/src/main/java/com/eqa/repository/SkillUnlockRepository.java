package com.eqa.repository;

import com.eqa.entity.SkillUnlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillUnlockRepository extends JpaRepository<SkillUnlock, Long> {
    List<SkillUnlock> findByUserId(Long userId);
    boolean existsByUserIdAndSkillCode(Long userId, String skillCode);
}
