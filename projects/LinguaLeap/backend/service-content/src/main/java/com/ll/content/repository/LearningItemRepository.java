package com.ll.content.repository;

import com.ll.content.entity.LearningItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningItemRepository extends JpaRepository<LearningItem, Long> {

    List<LearningItem> findByStudySetIdOrderByIdAsc(Long studySetId);

    List<LearningItem> findByStudySetIdAndCategory(Long studySetId, String category);

    long countByStudySetId(Long studySetId);

    long countByStudySetIdAndCategory(Long studySetId, String category);

    void deleteByStudySetId(Long studySetId);
}
