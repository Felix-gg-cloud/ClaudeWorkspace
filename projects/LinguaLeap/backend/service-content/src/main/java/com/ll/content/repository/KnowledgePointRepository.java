package com.ll.content.repository;

import com.ll.content.entity.KnowledgePoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, Long> {

    @Query("SELECT kp FROM KnowledgePoint kp WHERE kp.bankId = :bankId " +
           "AND (:type IS NULL OR kp.type = :type) " +
           "AND (:keyword IS NULL OR LOWER(kp.content) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) " +
           "     OR LOWER(kp.meaningZh) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) " +
           "AND (:difficulty IS NULL OR kp.difficulty = :difficulty)")
    Page<KnowledgePoint> findByBankIdAndFilters(
            @Param("bankId") Long bankId,
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("difficulty") Integer difficulty,
            Pageable pageable);

    List<KnowledgePoint> findByBankId(Long bankId);

    long countByBankId(Long bankId);

    List<KnowledgePoint> findByUnitIdOrderByDifficultyAsc(Long unitId);

    List<KnowledgePoint> findByLevelId(Long levelId);

    long countByUnitId(Long unitId);

    long countByLevelId(Long levelId);
}
