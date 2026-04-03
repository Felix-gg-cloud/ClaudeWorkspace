package com.ll.content.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.common.util.UserContext;
import com.ll.content.entity.KnowledgePoint;
import com.ll.content.entity.KnowledgeUnit;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.KnowledgeUnitRepository;
import com.ll.content.service.LearningService;
import com.ll.content.service.LevelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content/levels")
public class LevelController {

    private final LevelService levelService;
    private final LearningService learningService;
    private final KnowledgeUnitRepository unitRepo;
    private final KnowledgePointRepository kpRepo;

    public LevelController(LevelService levelService, LearningService learningService,
                           KnowledgeUnitRepository unitRepo, KnowledgePointRepository kpRepo) {
        this.levelService = levelService;
        this.learningService = learningService;
        this.unitRepo = unitRepo;
        this.kpRepo = kpRepo;
    }

    /**
     * 获取所有级别列表（含用户进度）
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listLevels() {
        return ApiResponse.ok(levelService.listLevelsWithProgress(getUserId()));
    }

    /**
     * 获取指定级别的详情（含单元列表+进度）
     */
    @GetMapping("/{levelId}")
    public ApiResponse<Map<String, Object>> getLevelDetail(@PathVariable Long levelId) {
        return ApiResponse.ok(levelService.getLevelDetail(levelId, getUserId()));
    }

    /**
     * 获取单元学习卡片
     */
    @GetMapping("/units/{unitId}/cards")
    public ApiResponse<Map<String, Object>> getUnitCards(@PathVariable Long unitId) {
        return ApiResponse.ok(learningService.getUnitCards(unitId, getUserId()));
    }

    /**
     * 标记单个知识点学习状态
     */
    @PostMapping("/progress")
    public ApiResponse<Void> markProgress(@RequestBody Map<String, Object> body) {
        Long kpId = ((Number) body.get("kpId")).longValue();
        String status = (String) body.getOrDefault("status", "learning");
        learningService.markProgress(getUserId(), kpId, status);
        return ApiResponse.ok(null);
    }

    /**
     * 完成单元学习
     */
    @PostMapping("/units/{unitId}/complete")
    public ApiResponse<Void> completeUnit(@PathVariable Long unitId) {
        learningService.completeUnitStudy(getUserId(), unitId);
        return ApiResponse.ok(null);
    }

    /**
     * 将 AI 生成的知识点导入到指定单元
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/units/{unitId}/import-kps")
    public ApiResponse<Map<String, Object>> importKps(
            @PathVariable Long unitId,
            @RequestBody List<Map<String, Object>> kpList) {
        KnowledgeUnit unit = unitRepo.findById(unitId)
                .orElseThrow(() -> new com.ll.common.exception.BizException(404, "单元不存在"));

        List<KnowledgePoint> kps = kpList.stream().map(m -> {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setLevelId(unit.getLevelId());
            kp.setUnitId(unitId);
            kp.setType((String) m.getOrDefault("type", "word"));
            kp.setContent((String) m.get("content"));
            kp.setPhonetic((String) m.get("phonetic"));
            kp.setMeaningZh((String) m.get("meaningZh"));
            kp.setExampleSentence((String) m.get("exampleSentence"));
            kp.setExampleZh((String) m.get("exampleZh"));
            Object diff = m.get("difficulty");
            if (diff instanceof Number) kp.setDifficulty(((Number) diff).intValue());
            return kp;
        }).toList();

        List<KnowledgePoint> saved = kpRepo.saveAll(kps);

        // 更新单元的知识点数量
        unit.setKpCount((int) kpRepo.countByUnitId(unitId));
        unitRepo.save(unit);

        return ApiResponse.ok(Map.of("imported", saved.size()));
    }

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new com.ll.common.exception.BizException(401, "未登录");
        return userId;
    }
}
