package com.lingobuddy.repository;

import com.lingobuddy.entity.SeedImportLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedImportLogRepository extends JpaRepository<SeedImportLog, Long> {
    boolean existsByFileName(String fileName);
}
