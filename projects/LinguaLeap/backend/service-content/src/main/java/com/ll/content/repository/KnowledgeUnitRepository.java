package com.ll.content.repository;

import com.ll.content.entity.KnowledgeUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeUnitRepository extends JpaRepository<KnowledgeUnit, Long> {

    List<KnowledgeUnit> findByLevelIdOrderBySortOrderAsc(Long levelId);

    long countByLevelId(Long levelId);
}
