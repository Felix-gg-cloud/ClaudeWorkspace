package com.ll.content.repository;

import com.ll.content.entity.QuestionBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {

    @Query("SELECT b FROM QuestionBank b WHERE " +
           "(:grade IS NULL OR b.grade = :grade) AND " +
           "(:type IS NULL OR b.type = :type) AND " +
           "(:keyword IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) AND " +
           "(b.userId IS NULL OR b.userId = :userId)")
    Page<QuestionBank> findByFilters(
            @Param("grade") String grade,
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            Pageable pageable);

    boolean existsByNameAndType(String name, String type);
}
