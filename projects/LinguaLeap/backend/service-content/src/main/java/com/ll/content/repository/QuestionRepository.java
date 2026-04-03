package com.ll.content.repository;

import com.ll.content.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findByBankId(Long bankId, Pageable pageable);

    List<Question> findByBankIdAndType(Long bankId, String type);

    List<Question> findByBankIdAndTypeAndGrade(Long bankId, String type, String grade);

    List<Question> findByBankIdAndTypeInAndGrade(Long bankId, Collection<String> types, String grade);

    List<Question> findByBankIdAndTypeIn(Long bankId, Collection<String> types);

    long countByBankId(Long bankId);

    long countByBankIdAndTypeAndGrade(Long bankId, String type, String grade);

    // 按知识点 ID 查询（知识库练习）
    List<Question> findByKpIdIn(Collection<Long> kpIds);

    List<Question> findByKpIdInAndTypeIn(Collection<Long> kpIds, Collection<String> types);

    List<Question> findByKpIdInAndTypeInAndGrade(Collection<Long> kpIds, Collection<String> types, String grade);
}
