package com.ll.user.repository;

import com.ll.user.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {

    Optional<DailyStats> findByUserIdAndStatDate(Long userId, LocalDate statDate);

    @Query("SELECT d FROM DailyStats d WHERE d.userId = :userId AND d.statDate BETWEEN :from AND :to ORDER BY d.statDate ASC")
    List<DailyStats> findByRange(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT d.statDate FROM DailyStats d WHERE d.userId = :userId AND d.tasksCompleted > 0 ORDER BY d.statDate DESC")
    List<LocalDate> findActiveDates(@Param("userId") Long userId);
}
