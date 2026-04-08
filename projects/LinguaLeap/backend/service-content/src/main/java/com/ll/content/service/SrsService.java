package com.ll.content.service;

import com.ll.content.entity.KnowledgePoint;
import com.ll.content.entity.SrsCard;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.SrsCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SrsService {

    private final SrsCardRepository srsRepo;
    private final KnowledgePointRepository kpRepo;

    public SrsService(SrsCardRepository srsRepo, KnowledgePointRepository kpRepo) {
        this.srsRepo = srsRepo;
        this.kpRepo = kpRepo;
    }

    /**
     * 练习时自动创建或获取 SRS 卡片
     */
    @Transactional
    public SrsCard getOrCreate(Long userId, Long kpId) {
        return srsRepo.findByUserIdAndKpId(userId, kpId)
                .orElseGet(() -> {
                    SrsCard card = new SrsCard();
                    card.setUserId(userId);
                    card.setKpId(kpId);
                    card.setNextReviewAt(LocalDateTime.now().plusDays(1));
                    return srsRepo.save(card);
                });
    }

    /**
     * SM-2 算法：提交复习结果
     * @param correct 是否回答正确
     */
    @Transactional
    public Map<String, Object> review(Long userId, Long kpId, boolean correct) {
        SrsCard card = getOrCreate(userId, kpId);

        if (correct) {
            card.setCorrectStreak(card.getCorrectStreak() + 1);
            double ef = card.getEaseFactor();
            // SM-2: quality=4 for correct
            ef = Math.max(1.3, ef + 0.1 - (5 - 4) * 0.08);
            card.setEaseFactor(ef);

            int newInterval;
            if (card.getReviewCount() == 0) {
                newInterval = 1;
            } else if (card.getReviewCount() == 1) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.round(card.getIntervalDays() * ef);
            }
            card.setIntervalDays(newInterval);
        } else {
            card.setCorrectStreak(0);
            card.setIntervalDays(1);
            double ef = Math.max(1.3, card.getEaseFactor() - 0.2);
            card.setEaseFactor(ef);
        }

        card.setReviewCount(card.getReviewCount() + 1);
        card.setLastReviewed(LocalDateTime.now());
        card.setNextReviewAt(LocalDateTime.now().plusDays(card.getIntervalDays()));
        srsRepo.save(card);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpId", kpId);
        result.put("correct", correct);
        result.put("nextReviewAt", card.getNextReviewAt());
        result.put("intervalDays", card.getIntervalDays());
        result.put("easeFactor", card.getEaseFactor());
        result.put("correctStreak", card.getCorrectStreak());
        return result;
    }

    /**
     * 获取待复习卡片列表，附带知识点信息
     */
    public List<Map<String, Object>> getDueCards(Long userId) {
        List<SrsCard> dueCards = srsRepo.findDueCards(userId, LocalDateTime.now());
        List<Map<String, Object>> result = new ArrayList<>();

        for (SrsCard card : dueCards) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cardId", card.getId());
            item.put("kpId", card.getKpId());
            item.put("intervalDays", card.getIntervalDays());
            item.put("correctStreak", card.getCorrectStreak());
            item.put("nextReviewAt", card.getNextReviewAt());

            kpRepo.findById(card.getKpId()).ifPresent(kp -> {
                item.put("content", kp.getContent());
                item.put("meaningZh", kp.getMeaningZh());
                item.put("phonetic", kp.getPhonetic());
                item.put("bankId", kp.getBankId());
            });

            result.add(item);
        }
        return result;
    }

    /**
     * SRS 统计
     */
    public Map<String, Object> getStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        long total = srsRepo.countByUserId(userId);
        long dueCount = srsRepo.findDueCards(userId, now).size();
        long mastered = srsRepo.countByUserIdAndCorrectStreakGreaterThanEqual(userId, 5);
        long learning = total - mastered;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("dueCount", dueCount);
        stats.put("mastered", mastered);
        stats.put("learning", learning);

        // 下次最早复习时间
        srsRepo.findFirstByUserIdAndNextReviewAtAfterOrderByNextReviewAtAsc(userId, now)
                .ifPresent(card -> stats.put("nextDueAt", card.getNextReviewAt()));

        return stats;
    }

    /**
     * 获取用户所有 SRS 卡片（含知识点信息）
     */
    public List<Map<String, Object>> getAllCards(Long userId) {
        List<SrsCard> cards = srsRepo.findByUserIdOrderByNextReviewAtAsc(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Long> kpIds = cards.stream().map(SrsCard::getKpId).toList();
        Map<Long, KnowledgePoint> kpMap = new HashMap<>();
        if (!kpIds.isEmpty()) {
            kpRepo.findAllById(kpIds).forEach(kp -> kpMap.put(kp.getId(), kp));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (SrsCard card : cards) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cardId", card.getId());
            item.put("kpId", card.getKpId());
            item.put("intervalDays", card.getIntervalDays());
            item.put("correctStreak", card.getCorrectStreak());
            item.put("nextReviewAt", card.getNextReviewAt());
            item.put("reviewCount", card.getReviewCount());
            item.put("easeFactor", card.getEaseFactor());

            // 状态：due / upcoming / mastered
            String status;
            if (card.getCorrectStreak() >= 5) {
                status = "mastered";
            } else if (card.getNextReviewAt() != null && !card.getNextReviewAt().isAfter(now)) {
                status = "due";
            } else {
                status = "upcoming";
            }
            item.put("status", status);

            KnowledgePoint kp = kpMap.get(card.getKpId());
            if (kp != null) {
                item.put("content", kp.getContent());
                item.put("meaningZh", kp.getMeaningZh());
                item.put("phonetic", kp.getPhonetic());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * Phase 5a: 按知识点内容字符串复习 SRS 卡片（AI 对话反馈联动）
     * @param userId 用户 ID
     * @param content 知识点内容（如 "apple" 或 "present tense"）
     * @param correct 是否答对
     * @return 复习结果，找不到知识点则返回 null
     */
    @Transactional
    public Map<String, Object> reviewByContent(Long userId, String content, boolean correct) {
        if (content == null || content.isBlank()) return null;
        return kpRepo.findFirstByContentIgnoreCase(content.trim())
                .map(kp -> review(userId, kp.getId(), correct))
                .orElse(null);
    }
}
