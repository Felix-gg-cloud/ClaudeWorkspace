package com.ll.content.repository;

import com.ll.content.entity.VocabConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VocabConstraintRepository extends JpaRepository<VocabConstraint, Long> {

    List<VocabConstraint> findByLevelCode(String levelCode);

    @Query("SELECT v.word FROM VocabConstraint v WHERE v.levelCode = :levelCode")
    List<String> findWordsByLevelCode(String levelCode);

    long countByLevelCode(String levelCode);
}
