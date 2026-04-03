package com.ll.content.repository;

import com.ll.content.entity.MistakeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MistakeRecordRepository extends JpaRepository<MistakeRecord, Long> {

    @Query("SELECT m FROM MistakeRecord m WHERE m.userId = :userId " +
            "AND (:bankId IS NULL OR m.questionId IN (SELECT q.id FROM Question q WHERE q.bankId = :bankId)) " +
            "AND (CAST(:questionType AS string) IS NULL OR m.questionType = :questionType) " +
            "AND (:reviewed IS NULL OR m.reviewed = :reviewed) " +
            "AND (CAST(:dateFrom AS timestamp) IS NULL OR m.createdAt >= :dateFrom) " +
            "AND (CAST(:dateTo AS timestamp) IS NULL OR m.createdAt <= :dateTo) " +
            "ORDER BY m.createdAt DESC")
    Page<MistakeRecord> findByFilters(@Param("userId") Long userId,
                                      @Param("bankId") Long bankId,
                                      @Param("questionType") String questionType,
                                      @Param("reviewed") Boolean reviewed,
                                      @Param("dateFrom") LocalDateTime dateFrom,
                                      @Param("dateTo") LocalDateTime dateTo,
                                      Pageable pageable);

    List<MistakeRecord> findByUserIdAndReviewedFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReviewedFalse(Long userId);
}
