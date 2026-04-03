package com.ll.content.service;

import com.ll.content.entity.KnowledgeLevel;
import com.ll.content.entity.KnowledgeUnit;
import com.ll.content.entity.LearningProgress;
import com.ll.content.repository.KnowledgeLevelRepository;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.KnowledgeUnitRepository;
import com.ll.content.repository.LearningProgressRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LevelService {

    private final KnowledgeLevelRepository levelRepo;
    private final KnowledgeUnitRepository unitRepo;
    private final KnowledgePointRepository kpRepo;
    private final LearningProgressRepository progressRepo;

    public LevelService(KnowledgeLevelRepository levelRepo,
                        KnowledgeUnitRepository unitRepo,
                        KnowledgePointRepository kpRepo,
                        LearningProgressRepository progressRepo) {
        this.levelRepo = levelRepo;
        this.unitRepo = unitRepo;
        this.kpRepo = kpRepo;
        this.progressRepo = progressRepo;
    }

    /**
     * 获取所有级别（含用户进度）
     */
    public List<Map<String, Object>> listLevelsWithProgress(Long userId) {
        List<KnowledgeLevel> levels = levelRepo.findAllByOrderBySortOrderAsc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (KnowledgeLevel level : levels) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", level.getId());
            item.put("code", level.getCode());
            item.put("name", level.getName());
            item.put("description", level.getDescription());
            item.put("gradeGroup", level.getGradeGroup());
            item.put("sortOrder", level.getSortOrder());

            // 单元数
            long unitCount = unitRepo.countByLevelId(level.getId());
            item.put("unitCount", unitCount);

            // 用户进度
            if (userId != null) {
                long totalKps = kpRepo.countByLevelId(level.getId());
                long mastered = progressRepo.countByUserIdAndLevelIdAndStatus(userId, level.getId(), "mastered");
                long learning = progressRepo.countByUserIdAndLevelIdAndStatus(userId, level.getId(), "learning");
                item.put("totalKps", totalKps);
                item.put("masteredKps", mastered);
                item.put("learningKps", learning);
                item.put("progress", totalKps > 0 ? Math.round((double) mastered / totalKps * 100) : 0);
            }

            result.add(item);
        }
        return result;
    }

    /**
     * 获取某级别的所有单元（含用户进度）
     */
    public Map<String, Object> getLevelDetail(Long levelId, Long userId) {
        KnowledgeLevel level = levelRepo.findById(levelId)
                .orElseThrow(() -> new com.ll.common.exception.BizException(404, "级别不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", level.getId());
        result.put("code", level.getCode());
        result.put("name", level.getName());
        result.put("description", level.getDescription());
        result.put("gradeGroup", level.getGradeGroup());

        List<KnowledgeUnit> units = unitRepo.findByLevelIdOrderBySortOrderAsc(levelId);
        List<Map<String, Object>> unitList = new ArrayList<>();

        for (KnowledgeUnit unit : units) {
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id", unit.getId());
            u.put("name", unit.getName());
            u.put("description", unit.getDescription());
            u.put("topic", unit.getTopic());
            u.put("sortOrder", unit.getSortOrder());
            u.put("kpCount", unit.getKpCount());

            if (userId != null) {
                long total = progressRepo.countByUserIdAndUnitId(userId, unit.getId());
                long mastered = progressRepo.countByUserIdAndUnitIdAndStatus(userId, unit.getId(), "mastered");
                u.put("totalProgress", total);
                u.put("masteredCount", mastered);
                u.put("progress", unit.getKpCount() > 0
                        ? Math.round((double) mastered / unit.getKpCount() * 100) : 0);
            }

            unitList.add(u);
        }
        result.put("units", unitList);
        return result;
    }
}
