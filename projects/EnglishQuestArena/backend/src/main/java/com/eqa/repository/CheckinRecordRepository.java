package com.eqa.repository;

import com.eqa.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {
    Optional<CheckinRecord> findByUserIdAndCheckinDate(Long userId, LocalDate date);
    List<CheckinRecord> findByUserIdOrderByCheckinDateDesc(Long userId);
    boolean existsByUserIdAndCheckinDate(Long userId, LocalDate date);
}
