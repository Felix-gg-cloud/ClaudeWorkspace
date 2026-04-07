package com.ll.content.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.content.entity.GoldenSample;
import com.ll.content.repository.GoldenSampleRepository;
import com.ll.content.repository.GrammarPointRefRepository;
import com.ll.content.repository.VocabConstraintRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5a — 教学材料约束数据 API
 * 供 service-ai 的 OrchestratorService 查询
 */
@RestController
@RequestMapping("/api/content/constraints")
public class MaterialConstraintController {

    private final VocabConstraintRepository vocabRepo;
    private final GrammarPointRefRepository grammarRepo;
    private final GoldenSampleRepository sampleRepo;

    public MaterialConstraintController(VocabConstraintRepository vocabRepo,
                                        GrammarPointRefRepository grammarRepo,
                                        GoldenSampleRepository sampleRepo) {
        this.vocabRepo = vocabRepo;
        this.grammarRepo = grammarRepo;
        this.sampleRepo = sampleRepo;
    }

    /**
     * 获取指定级别的词汇列表（仅 word 字段，用于注入 prompt）
     */
    @GetMapping("/vocab/{levelCode}")
    public ApiResponse<?> getVocabWords(@PathVariable String levelCode) {
        List<String> words = vocabRepo.findWordsByLevelCode(levelCode);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("levelCode", levelCode);
        data.put("count", words.size());
        data.put("words", words);
        return ApiResponse.ok(data);
    }

    /**
     * 获取指定级别的语法点列表
     */
    @GetMapping("/grammar/{levelCode}")
    public ApiResponse<?> getGrammarPoints(@PathVariable String levelCode) {
        List<String> points = grammarRepo.findPointsByLevelCode(levelCode);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("levelCode", levelCode);
        data.put("count", points.size());
        data.put("points", points);
        return ApiResponse.ok(data);
    }

    /**
     * 获取指定级别的黄金样本（截取前 2000 字符作为参考）
     */
    @GetMapping("/samples/{levelCode}")
    public ApiResponse<?> getSampleExcerpt(@PathVariable String levelCode) {
        List<GoldenSample> samples = sampleRepo.findByLevelCode(levelCode);
        if (samples.isEmpty()) {
            return ApiResponse.ok(Map.of("levelCode", levelCode, "excerpt", ""));
        }
        // 取第一份样本的前 2000 字符
        GoldenSample sample = samples.get(0);
        String excerpt = sample.getContentText();
        if (excerpt.length() > 2000) {
            excerpt = excerpt.substring(0, 2000) + "\n...（节选）";
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("levelCode", levelCode);
        data.put("title", sample.getTitle());
        data.put("hasAnswer", sample.getHasAnswer());
        data.put("excerpt", excerpt);
        return ApiResponse.ok(data);
    }

    /**
     * 获取所有级别的约束数据统计
     */
    @GetMapping("/stats")
    public ApiResponse<?> getStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalVocab", vocabRepo.count());
        data.put("totalGrammar", grammarRepo.count());
        data.put("totalSamples", sampleRepo.count());
        return ApiResponse.ok(data);
    }
}
