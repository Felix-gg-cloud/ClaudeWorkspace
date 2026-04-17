package com.eqa.controller;

import com.eqa.entity.BossConfig;
import com.eqa.entity.BossPoolTask;
import com.eqa.repository.BossConfigRepository;
import com.eqa.repository.BossPoolTaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/boss")
public class BossController {

    private final BossConfigRepository bossConfigRepo;
    private final BossPoolTaskRepository bossPoolTaskRepo;

    public BossController(BossConfigRepository bossConfigRepo, BossPoolTaskRepository bossPoolTaskRepo) {
        this.bossConfigRepo = bossConfigRepo;
        this.bossPoolTaskRepo = bossPoolTaskRepo;
    }

    /**
     * 获取 Boss 配置 + 题池
     */
    @GetMapping("/{chapterCode}")
    public ResponseEntity<?> getBoss(@PathVariable String chapterCode) {
        Optional<BossConfig> configOpt = bossConfigRepo.findByChapterCode(chapterCode);
        if (configOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BossConfig config = configOpt.get();
        List<BossPoolTask> tasks = bossPoolTaskRepo.findByBossCode(config.getCode());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", config.getCode());
        result.put("chapterCode", config.getChapterCode());
        result.put("bossName", config.getBossName());
        result.put("bossTitle", config.getBossTitle());
        result.put("bossHp", config.getBossHp());
        result.put("playerHp", config.getPlayerHp());
        result.put("timeLimitSec", config.getTimeLimitSec());
        result.put("damageCorrect", config.getDamageCorrect());
        result.put("damageWrong", config.getDamageWrong());

        List<Map<String, Object>> taskList = new ArrayList<>();
        for (BossPoolTask t : tasks) {
            Map<String, Object> taskMap = new LinkedHashMap<>();
            taskMap.put("taskCode", t.getTaskCode());
            taskMap.put("taskType", t.getTaskType());
            taskMap.put("promptEn", t.getPromptEn());
            taskMap.put("promptZhHint", t.getPromptZhHint());
            taskMap.put("options", t.getOptions());
            taskMap.put("answer", t.getAnswer());
            taskMap.put("explanationEn", t.getExplanationEn());
            taskMap.put("explanationZh", t.getExplanationZh());
            taskMap.put("ttsEnabled", t.isTtsEnabled());
            taskMap.put("ttsTextEn", t.getTtsTextEn());
            taskMap.put("contentItemCodes", t.getContentItemCodes());
            taskList.add(taskMap);
        }
        result.put("tasks", taskList);

        return ResponseEntity.ok(result);
    }
}
