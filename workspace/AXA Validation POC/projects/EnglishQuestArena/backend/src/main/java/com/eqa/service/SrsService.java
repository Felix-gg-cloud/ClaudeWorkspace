package com.eqa.service;

import com.eqa.entity.SrsRecord;
import com.eqa.repository.SrsRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class SrsService {

    private final SrsRecordRepository srsRepo;

    public SrsService(SrsRecordRepository srsRepo) {
        this.srsRepo = srsRepo;
    }

    /**
     * SM-2 算法复习一张卡：quality 0-5, >=3 算正确
     */
    @Transactional
    public SrsRecord review(Long userId, String contentItemCode, int quality) {
        SrsRecord record = srsRepo.findByUserIdAndContentItemCode(userId, contentItemCode)
                .orElseGet(() -> {
                    SrsRecord r = new SrsRecord();
                    r.setUserId(userId);
                    r.setContentItemCode(contentItemCode);
                    r.setNextReviewDate(LocalDate.now());
                    return r;
                });

        record.setTotalReviews(record.getTotalReviews() + 1);
        record.setLastReviewedAt(LocalDateTime.now());

        if (quality >= 3) {
            // 正确
            record.setTotalCorrect(record.getTotalCorrect() + 1);
            record.setRepetitions(record.getRepetitions() + 1);

            int reps = record.getRepetitions();
            if (reps == 1) {
                record.setIntervalDays(1);
            } else if (reps == 2) {
                record.setIntervalDays(6);
            } else {
                int newInterval = (int) Math.round(record.getIntervalDays() * record.getEaseFactor().doubleValue());
                record.setIntervalDays(newInterval);
            }
        } else {
            // 错误：重置
            record.setRepetitions(0);
            record.setIntervalDays(1);
        }

        // 更新 easeFactor: EF = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        double ef = record.getEaseFactor().doubleValue();
        double diff = 5.0 - quality;
        ef = ef + (0.1 - diff * (0.08 + diff * 0.02));
        if (ef < 1.3) ef = 1.3;
        record.setEaseFactor(BigDecimal.valueOf(ef).setScale(2, RoundingMode.HALF_UP));

        record.setNextReviewDate(LocalDate.now().plusDays(record.getIntervalDays()));
        srsRepo.save(record);

        return record;
    }

    /**
     * 获取今天到期需要复习的卡片
     */
    public List<SrsRecord> getDueCards(Long userId) {
        return srsRepo.findByUserIdAndNextReviewDateLessThanEqual(userId, LocalDate.now());
    }

    /**
     * 获取用户所有SRS记录
     */
    public List<SrsRecord> getAllRecords(Long userId) {
        return srsRepo.findByUserId(userId);
    }

    public Map<String, Object> toMap(SrsRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contentItemCode", r.getContentItemCode());
        m.put("easeFactor", r.getEaseFactor());
        m.put("intervalDays", r.getIntervalDays());
        m.put("repetitions", r.getRepetitions());
        m.put("nextReviewDate", r.getNextReviewDate().toString());
        m.put("totalReviews", r.getTotalReviews());
        m.put("totalCorrect", r.getTotalCorrect());
        return m;
    }
}
