package com.ll.content.repository;

import com.ll.content.entity.UserQuestionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserQuestionHistoryRepository extends JpaRepository<UserQuestionHistory, Long> {

    @Query("SELECT h.questionId FROM UserQuestionHistory h WHERE h.userId = :userId AND h.questionId IN :questionIds")
    List<Long> findDoneQuestionIds(Long userId, List<Long> questionIds);
}
