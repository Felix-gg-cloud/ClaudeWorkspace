package com.ll.content.repository;

import com.ll.content.entity.SrsCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SrsCardRepository extends JpaRepository<SrsCard, Long> {

    Optional<SrsCard> findByUserIdAndKpId(Long userId, Long kpId);

    @Query("SELECT c FROM SrsCard c WHERE c.userId = :userId AND c.nextReviewAt <= :now ORDER BY c.nextReviewAt ASC")
    List<SrsCard> findDueCards(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    long countByUserIdAndNextReviewAtAfter(Long userId, LocalDateTime now);

    long countByUserIdAndCorrectStreakGreaterThanEqual(Long userId, int streak);

    long countByUserId(Long userId);

    List<SrsCard> findByUserIdOrderByNextReviewAtAsc(Long userId);

    Optional<SrsCard> findFirstByUserIdAndNextReviewAtAfterOrderByNextReviewAtAsc(Long userId, LocalDateTime now);
}
