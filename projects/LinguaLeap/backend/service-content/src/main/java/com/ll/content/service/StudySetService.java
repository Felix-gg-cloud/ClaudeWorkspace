package com.ll.content.service;

import com.ll.content.client.AiServiceClient;
import com.ll.content.entity.LearningItem;
import com.ll.content.entity.StudySet;
import com.ll.content.repository.LearningItemRepository;
import com.ll.content.repository.StudySetRepository;
import com.ll.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.*;

@Service
public class StudySetService {

    private static final Logger log = LoggerFactory.getLogger(StudySetService.class);
    private final StudySetRepository studySetRepo;
    private final LearningItemRepository itemRepo;
    private final AiServiceClient aiClient;
    private final FileStorageService fileStorage;
    private final PdfExtractorService pdfExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudySetService(StudySetRepository studySetRepo,
                           LearningItemRepository itemRepo,
                           AiServiceClient aiClient,
                           FileStorageService fileStorage,
                           PdfExtractorService pdfExtractor) {
        this.studySetRepo = studySetRepo;
        this.itemRepo = itemRepo;
        this.aiClient = aiClient;
        this.fileStorage = fileStorage;
        this.pdfExtractor = pdfExtractor;
    }

    /**
     * 从文本创建学习集（完整管线）
     */
    @Transactional
    public StudySet createFromText(Long userId, String title, String text,
                                    String sourceType, String userNote, String grade, String token) {
        // 1. 创建学习集（processing 状态）
        StudySet set = new StudySet();
        set.setUserId(userId);
        set.setTitle(title);
        set.setDescription(userNote);
        set.setSourceType(sourceType != null ? sourceType : "text");
        set.setSourceText(text);
        set.setGrade(grade);
        set.setStatus("processing");
        set = studySetRepo.save(set);

        try {
            // 2. AI 提取 + 分类
            Map<String, Object> extractResult = aiClient.extractContent(text, grade, sourceType, userNote, token);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) extractResult.get("items");
            String summary = (String) extractResult.getOrDefault("summary", "");

            if (items == null || items.isEmpty()) {
                set.setStatus("failed");
                set.setAiSummary("AI 未能从文本中提取到有效内容");
                studySetRepo.save(set);
                return set;
            }

            // 3. 保存学习项
            Map<String, Integer> categoryCounts = new HashMap<>();
            for (Map<String, Object> item : items) {
                LearningItem li = new LearningItem();
                li.setStudySetId(set.getId());
                li.setUserId(userId);
                li.setCategory((String) item.getOrDefault("category", "vocabulary"));
                li.setContent((String) item.get("content"));
                li.setMeaningZh((String) item.get("meaningZh"));
                li.setPhonetic((String) item.get("phonetic"));
                li.setExampleSentence((String) item.get("exampleSentence"));
                li.setExampleZh((String) item.get("exampleZh"));
                li.setExtraData((String) item.get("extraData"));
                li.setDifficulty(item.get("difficulty") instanceof Number n ? n.intValue() : 1);
                li.setAiNote((String) item.get("aiNote"));
                itemRepo.save(li);

                categoryCounts.merge(li.getCategory(), 1, Integer::sum);
            }

            // 4. AI 生成出题策略
            Map<String, Object> strategy = aiClient.generateStrategy(
                    grade, userNote, categoryCounts, summary, token);

            // 5. 更新学习集
            set.setAiSummary(summary);
            set.setAiStrategy(objectMapper.writeValueAsString(strategy));
            set.setItemCount(items.size());
            set.setStatus("ready");
            studySetRepo.save(set);

            log.info("学习集创建完成: id={}, items={}, categories={}",
                    set.getId(), items.size(), categoryCounts);

        } catch (Exception e) {
            log.error("学习集创建失败: {}", e.getMessage(), e);
            set.setStatus("failed");
            set.setAiSummary("处理失败: " + e.getMessage());
            studySetRepo.save(set);
        }

        return set;
    }

    /**
     * 获取用户的学习集列表
     */
    public List<StudySet> listByUser(Long userId) {
        return studySetRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取学习集详情（含学习项）
     */
    public Map<String, Object> getDetail(Long id, Long userId) {
        StudySet set = studySetRepo.findById(id)
                .orElseThrow(() -> new BizException("学习集不存在"));
        if (!set.getUserId().equals(userId)) {
            throw new BizException("无权访问");
        }

        List<LearningItem> items = itemRepo.findByStudySetIdOrderByIdAsc(id);

        // 按分类分组
        Map<String, List<LearningItem>> grouped = new LinkedHashMap<>();
        for (LearningItem item : items) {
            grouped.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studySet", set);
        result.put("items", items);
        result.put("groupedItems", grouped);
        result.put("categoryCounts", new LinkedHashMap<>());
        grouped.forEach((k, v) -> ((Map<String, Object>) result.get("categoryCounts")).put(k, v.size()));

        return result;
    }

    /**
     * 从 PDF 文件创建学习集
     */
    @Transactional
    public StudySet createFromFile(Long userId, String title, MultipartFile file,
                                    String userNote, String grade, String token) {
        // 1. 保存文件
        String storedName = fileStorage.store(file);

        // 2. 提取 PDF 文本
        Path filePath = fileStorage.getFilePath(storedName);
        String extractedText;
        try {
            extractedText = pdfExtractor.extractText(filePath);
        } catch (Exception e) {
            fileStorage.delete(storedName);
            throw e;
        }

        // 3. 复用文本创建管线
        StudySet set = createFromText(userId, title, extractedText, "pdf", userNote, grade, token);
        set.setSourceFileUrl(storedName);
        set.setSourceText(extractedText); // 保留提取的文本
        studySetRepo.save(set);
        return set;
    }

    /**
     * 删除学习集（级联删除学习项）
     */
    @Transactional
    public void delete(Long id, Long userId) {
        StudySet set = studySetRepo.findById(id)
                .orElseThrow(() -> new BizException("学习集不存在"));
        if (!set.getUserId().equals(userId)) {
            throw new BizException("无权删除");
        }
        // 删除关联文件
        if (set.getSourceFileUrl() != null) {
            fileStorage.delete(set.getSourceFileUrl());
        }
        itemRepo.deleteByStudySetId(id);
        studySetRepo.delete(set);
    }
}
