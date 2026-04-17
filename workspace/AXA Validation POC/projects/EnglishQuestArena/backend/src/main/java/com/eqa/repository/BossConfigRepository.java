package com.eqa.repository;

import com.eqa.entity.BossConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BossConfigRepository extends JpaRepository<BossConfig, String> {
    Optional<BossConfig> findByChapterCode(String chapterCode);
}
