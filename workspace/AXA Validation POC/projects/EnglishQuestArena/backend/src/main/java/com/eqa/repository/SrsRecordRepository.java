package com.eqa.repository;

import com.eqa.entity.SrsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SrsRecordRepository extends JpaRepository<SrsRecord, Long> {
    Optional<SrsRecord> findByUserIdAndContentItemCode(Long userId, String contentItemCode);
    List<SrsRecord> findByUserIdAndNextReviewDateLessThanEqual(Long userId, LocalDate date);
    List<SrsRecord> findByUserId(Long userId);
    long countByUserId(Long userId);
}
