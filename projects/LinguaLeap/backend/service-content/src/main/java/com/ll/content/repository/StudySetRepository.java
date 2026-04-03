package com.ll.content.repository;

import com.ll.content.entity.StudySet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudySetRepository extends JpaRepository<StudySet, Long> {

    List<StudySet> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    List<StudySet> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}
