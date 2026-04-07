package com.ll.content.repository;

import com.ll.content.entity.GrammarPointRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GrammarPointRefRepository extends JpaRepository<GrammarPointRef, Long> {

    List<GrammarPointRef> findByLevelCode(String levelCode);

    @Query("SELECT g.grammarPoint FROM GrammarPointRef g WHERE g.levelCode = :levelCode")
    List<String> findPointsByLevelCode(String levelCode);

    long countByLevelCode(String levelCode);
}
