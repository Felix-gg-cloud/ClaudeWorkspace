package com.ll.ai.repository;

import com.ll.ai.entity.AiAnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiAnalysisCacheRepository extends JpaRepository<AiAnalysisCache, Long> {

    Optional<AiAnalysisCache> findByContentHashAndAnalysisTypeAndExpiresAtAfter(
            String contentHash, String analysisType, LocalDateTime now);
}
