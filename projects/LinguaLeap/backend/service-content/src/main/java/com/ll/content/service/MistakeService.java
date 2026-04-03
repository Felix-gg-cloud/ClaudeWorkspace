package com.ll.content.service;

import com.ll.common.exception.BizException;
import com.ll.content.entity.MistakeRecord;
import com.ll.content.entity.Question;
import com.ll.content.repository.MistakeRecordRepository;
import com.ll.content.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MistakeService {

    private final MistakeRecordRepository mistakeRepo;
    private final QuestionRepository questionRepo;

    public MistakeService(MistakeRecordRepository mistakeRepo, QuestionRepository questionRepo) {
        this.mistakeRepo = mistakeRepo;
        this.questionRepo = questionRepo;
    }

    public Map<String, Object> list(Long userId, Long bankId, String questionType,
                                     Boolean reviewed, String dateFrom, String dateTo,
                                     int page, int size) {
        LocalDateTime from = dateFrom != null ? LocalDate.parse(dateFrom).atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? LocalDate.parse(dateTo).plusDays(1).atStartOfDay() : null;
        Page<MistakeRecord> pageResult = mistakeRepo.findByFilters(userId, bankId, questionType, reviewed, from, to,
                PageRequest.of(page, size));

        // 批量查询题目信息
        Set<Long> qIds = new HashSet<>();
        for (MistakeRecord m : pageResult.getContent()) {
            if (m.getQuestionId() != null) qIds.add(m.getQuestionId());
        }
        Map<Long, Question> questionMap = new HashMap<>();
        if (!qIds.isEmpty()) {
            questionRepo.findAllById(qIds).forEach(q -> questionMap.put(q.getId(), q));
        }

        // 构建带题干的响应
        List<Map<String, Object>> content = new ArrayList<>();
        for (MistakeRecord m : pageResult.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("questionId", m.getQuestionId());
            item.put("kpId", m.getKpId());
            item.put("userAnswer", m.getUserAnswer());
            item.put("correctAnswer", m.getCorrectAnswer());
            item.put("questionType", m.getQuestionType());
            item.put("reviewed", m.getReviewed());
            item.put("createdAt", m.getCreatedAt());
            Question q = questionMap.get(m.getQuestionId());
            if (q != null) {
                item.put("stem", q.getStem());
                item.put("explanation", q.getExplanation());
            }
            content.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("number", pageResult.getNumber());
        result.put("size", pageResult.getSize());
        return result;
    }

    public Map<String, Object> getDetail(Long userId, Long id) {
        MistakeRecord m = mistakeRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "错题不存在"));
        if (!m.getUserId().equals(userId)) {
            throw new BizException(403, "无权查看");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", m.getId());
        result.put("questionId", m.getQuestionId());
        result.put("kpId", m.getKpId());
        result.put("userAnswer", m.getUserAnswer());
        result.put("correctAnswer", m.getCorrectAnswer());
        result.put("questionType", m.getQuestionType());
        result.put("reviewed", m.getReviewed());
        result.put("createdAt", m.getCreatedAt());

        // 附带题目信息
        if (m.getQuestionId() != null) {
            questionRepo.findById(m.getQuestionId()).ifPresent(q -> {
                result.put("stem", q.getStem());
                result.put("options", q.getOptions());
                result.put("explanation", q.getExplanation());
            });
        }

        return result;
    }

    @Transactional
    public void markReviewed(Long userId, Long id) {
        MistakeRecord m = mistakeRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "错题不存在"));
        if (!m.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作");
        }
        m.setReviewed(true);
        mistakeRepo.save(m);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        MistakeRecord m = mistakeRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "错题不存在"));
        if (!m.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作");
        }
        mistakeRepo.delete(m);
    }

    /**
     * 从错题中抽取题目 ID 列表，用于错题专项练习
     */
    public List<Long> getMistakeQuestionIds(Long userId, int count) {
        List<MistakeRecord> unreviewed = mistakeRepo.findByUserIdAndReviewedFalseOrderByCreatedAtDesc(userId);
        List<Long> questionIds = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (MistakeRecord m : unreviewed) {
            if (m.getQuestionId() != null && seen.add(m.getQuestionId())) {
                questionIds.add(m.getQuestionId());
            }
            if (questionIds.size() >= count) break;
        }
        return questionIds;
    }
}
