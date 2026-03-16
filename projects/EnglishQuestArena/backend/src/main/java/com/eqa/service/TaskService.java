package com.eqa.service;

import com.eqa.entity.ContentItem;
import com.eqa.repository.ContentItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final ContentItemRepository contentItemRepo;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public TaskService(ContentItemRepository contentItemRepo, ObjectMapper objectMapper) {
        this.contentItemRepo = contentItemRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * 为指定章节 + 天数生成一批任务（模拟前端 generateQuestTasks 逻辑）
     */
    public List<Map<String, Object>> generateDayTasks(String chapterCode, int dayIndex) {
        List<ContentItem> dayItems = contentItemRepo.findByChapterCodeAndDayIndex(chapterCode, dayIndex);
        List<ContentItem> allChapterItems = contentItemRepo.findByChapterCode(chapterCode);

        if (dayItems.isEmpty()) return List.of();

        List<Map<String, Object>> tasks = new ArrayList<>();
        int order = 0;

        for (ContentItem item : dayItems) {
            if ("PHRASE".equals(item.getType())) continue; // 短语不单独出题

            // 1. MCQ: 选择中文含义
            tasks.add(buildMcq(item, allChapterItems, ++order, chapterCode, dayIndex));

            // 2. SPELLING: 拼写
            tasks.add(buildSpelling(item, ++order, chapterCode, dayIndex));

            // 3. MCQ_REVERSE: 中文选英文
            tasks.add(buildMcqReverse(item, allChapterItems, ++order, chapterCode, dayIndex));
        }

        // 打乱顺序
        Collections.shuffle(tasks, random);
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).put("orderIndex", i + 1);
        }

        return tasks;
    }

    private Map<String, Object> buildMcq(ContentItem item, List<ContentItem> pool, int order,
                                          String chapterCode, int dayIndex) {
        Map<String, Object> task = new LinkedHashMap<>();
        String lessonCode = chapterCode + "_D" + String.format("%02d", dayIndex);
        task.put("code", "GEN_MCQ_" + item.getCode() + "_" + order);
        task.put("lessonCode", lessonCode);
        task.put("type", "MCQ");
        task.put("promptEn", "What does \"" + item.getTextEn() + "\" mean?");
        task.put("orderIndex", order);
        task.put("xpReward", 5);
        task.put("ttsEnabled", true);

        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of("key", "A", "textEn", item.getTextEn(), "textZh", item.getTextZh()));

        List<ContentItem> distractors = pickDistractors(item, pool, 2);
        char key = 'B';
        for (ContentItem d : distractors) {
            options.add(Map.of("key", String.valueOf(key++), "textEn", item.getTextEn(), "textZh", d.getTextZh()));
        }
        Collections.shuffle(options, random);

        String correctKey = options.stream()
                .filter(o -> o.get("textZh").equals(item.getTextZh()))
                .map(o -> o.get("key"))
                .findFirst().orElse("A");

        task.put("options", options);
        task.put("answer", Map.of("correctOptionKey", correctKey));
        task.put("contentItemCodes", List.of(item.getCode()));
        return task;
    }

    private Map<String, Object> buildSpelling(ContentItem item, int order,
                                               String chapterCode, int dayIndex) {
        Map<String, Object> task = new LinkedHashMap<>();
        String lessonCode = chapterCode + "_D" + String.format("%02d", dayIndex);
        task.put("code", "GEN_SPELL_" + item.getCode() + "_" + order);
        task.put("lessonCode", lessonCode);
        task.put("type", "SPELLING");
        task.put("promptEn", "Type the word: " + item.getTextZh());
        task.put("orderIndex", order);
        task.put("xpReward", 5);
        task.put("ttsEnabled", true);
        task.put("answer", Map.of("acceptedTexts", List.of(item.getTextEn().toLowerCase())));
        task.put("contentItemCodes", List.of(item.getCode()));
        return task;
    }

    private Map<String, Object> buildMcqReverse(ContentItem item, List<ContentItem> pool, int order,
                                                 String chapterCode, int dayIndex) {
        Map<String, Object> task = new LinkedHashMap<>();
        String lessonCode = chapterCode + "_D" + String.format("%02d", dayIndex);
        task.put("code", "GEN_REV_" + item.getCode() + "_" + order);
        task.put("lessonCode", lessonCode);
        task.put("type", "MCQ_REVERSE");
        task.put("promptEn", "\"" + item.getTextZh() + "\" is ___ in English.");
        task.put("orderIndex", order);
        task.put("xpReward", 5);
        task.put("ttsEnabled", true);

        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of("key", "A", "textEn", item.getTextEn(), "textZh", item.getTextZh()));

        List<ContentItem> distractors = pickDistractors(item, pool, 2);
        char key = 'B';
        for (ContentItem d : distractors) {
            options.add(Map.of("key", String.valueOf(key++), "textEn", d.getTextEn(), "textZh", d.getTextZh()));
        }
        Collections.shuffle(options, random);

        String correctKey = options.stream()
                .filter(o -> o.get("textEn").equals(item.getTextEn()))
                .map(o -> o.get("key"))
                .findFirst().orElse("A");

        task.put("options", options);
        task.put("answer", Map.of("correctOptionKey", correctKey));
        task.put("contentItemCodes", List.of(item.getCode()));
        return task;
    }

    private List<ContentItem> pickDistractors(ContentItem target, List<ContentItem> pool, int count) {
        List<ContentItem> candidates = pool.stream()
                .filter(c -> !c.getCode().equals(target.getCode()) && "WORD".equals(c.getType()))
                .collect(Collectors.toList());
        Collections.shuffle(candidates, random);
        return candidates.subList(0, Math.min(count, candidates.size()));
    }
}
