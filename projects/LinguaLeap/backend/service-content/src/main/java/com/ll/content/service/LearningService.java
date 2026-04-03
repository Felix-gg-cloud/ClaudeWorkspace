package com.ll.content.service;

import com.ll.content.entity.KnowledgePoint;
import com.ll.content.entity.KnowledgeUnit;
import com.ll.content.entity.LearningProgress;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.KnowledgeUnitRepository;
import com.ll.content.repository.LearningProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LearningService {

    private final KnowledgePointRepository kpRepo;
    private final KnowledgeUnitRepository unitRepo;
    private final LearningProgressRepository progressRepo;

    public LearningService(KnowledgePointRepository kpRepo,
                           KnowledgeUnitRepository unitRepo,
                           LearningProgressRepository progressRepo) {
        this.kpRepo = kpRepo;
        this.unitRepo = unitRepo;
        this.progressRepo = progressRepo;
    }

    /**
     * 获取某单元的学习卡片列表（含用户进度）
     */
    public Map<String, Object> getUnitCards(Long unitId, Long userId) {
        KnowledgeUnit unit = unitRepo.findById(unitId)
                .orElseThrow(() -> new com.ll.common.exception.BizException(404, "单元不存在"));

        List<KnowledgePoint> kps = kpRepo.findByUnitIdOrderByDifficultyAsc(unitId);

        List<Map<String, Object>> cards = new ArrayList<>();
        for (KnowledgePoint kp : kps) {
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("id", kp.getId());
            card.put("type", kp.getType());
            card.put("content", kp.getContent());
            card.put("phonetic", kp.getPhonetic());
            card.put("meaningZh", kp.getMeaningZh());
            card.put("exampleSentence", kp.getExampleSentence());
            card.put("exampleZh", kp.getExampleZh());
            card.put("difficulty", kp.getDifficulty());

            // 用户对该知识点的学习状态
            if (userId != null) {
                Optional<LearningProgress> lp = progressRepo.findByUserIdAndKpId(userId, kp.getId());
                card.put("status", lp.map(LearningProgress::getStatus).orElse("new"));
                card.put("reviewCount", lp.map(LearningProgress::getReviewCount).orElse(0));
            }
            cards.add(card);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unitId", unit.getId());
        result.put("unitName", unit.getName());
        result.put("topic", unit.getTopic());
        result.put("cards", cards);
        result.put("totalCount", cards.size());
        return result;
    }

    /**
     * 标记知识点学习状态
     */
    @Transactional
    public void markProgress(Long userId, Long kpId, String status) {
        KnowledgePoint kp = kpRepo.findById(kpId)
                .orElseThrow(() -> new com.ll.common.exception.BizException(404, "知识点不存在"));

        LearningProgress lp = progressRepo.findByUserIdAndKpId(userId, kpId)
                .orElseGet(() -> {
                    LearningProgress p = new LearningProgress();
                    p.setUserId(userId);
                    p.setKpId(kpId);
                    p.setUnitId(kp.getUnitId());
                    p.setLevelId(kp.getLevelId());
                    return p;
                });

        lp.setStatus(status);
        lp.setReviewCount(lp.getReviewCount() + 1);
        lp.setLastReviewAt(LocalDateTime.now());
        progressRepo.save(lp);
    }

    /**
     * 完成单元学习（标记所有知识点为 learning）
     */
    @Transactional
    public void completeUnitStudy(Long userId, Long unitId) {
        KnowledgeUnit unit = unitRepo.findById(unitId)
                .orElseThrow(() -> new com.ll.common.exception.BizException(404, "单元不存在"));

        List<KnowledgePoint> kps = kpRepo.findByUnitIdOrderByDifficultyAsc(unitId);
        for (KnowledgePoint kp : kps) {
            LearningProgress lp = progressRepo.findByUserIdAndKpId(userId, kp.getId())
                    .orElseGet(() -> {
                        LearningProgress p = new LearningProgress();
                        p.setUserId(userId);
                        p.setKpId(kp.getId());
                        p.setUnitId(unitId);
                        p.setLevelId(unit.getLevelId());
                        return p;
                    });

            if ("new".equals(lp.getStatus())) {
                lp.setStatus("learning");
            }
            lp.setReviewCount(lp.getReviewCount() + 1);
            lp.setLastReviewAt(LocalDateTime.now());
            progressRepo.save(lp);
        }
    }
}
