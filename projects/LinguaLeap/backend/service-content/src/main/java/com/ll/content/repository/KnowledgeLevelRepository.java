package com.ll.content.repository;

import com.ll.content.entity.KnowledgeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeLevelRepository extends JpaRepository<KnowledgeLevel, Long> {

    List<KnowledgeLevel> findAllByOrderBySortOrderAsc();

    Optional<KnowledgeLevel> findByCode(String code);

    List<KnowledgeLevel> findByGradeGroupOrderBySortOrderAsc(String gradeGroup);
}
