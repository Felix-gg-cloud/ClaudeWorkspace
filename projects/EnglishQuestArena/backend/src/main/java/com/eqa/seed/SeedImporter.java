package com.eqa.seed;

import com.eqa.entity.*;
import com.eqa.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class SeedImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedImporter.class);

    private final ChapterRepository chapterRepo;
    private final LessonRepository lessonRepo;
    private final ContentItemRepository contentItemRepo;
    private final BossConfigRepository bossConfigRepo;
    private final BossPoolTaskRepository bossPoolTaskRepo;
    private final ObjectMapper objectMapper;

    public SeedImporter(ChapterRepository chapterRepo,
                        LessonRepository lessonRepo,
                        ContentItemRepository contentItemRepo,
                        BossConfigRepository bossConfigRepo,
                        BossPoolTaskRepository bossPoolTaskRepo,
                        ObjectMapper objectMapper) {
        this.chapterRepo = chapterRepo;
        this.lessonRepo = lessonRepo;
        this.contentItemRepo = contentItemRepo;
        this.bossConfigRepo = bossConfigRepo;
        this.bossPoolTaskRepo = bossPoolTaskRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        importChapters();
        importLessons();
        importContentItems();
        importBossConfigs();
        importBossPoolTasks();
        log.info("Seed import completed.");
    }

    private void importChapters() throws Exception {
        List<Chapter> items = readJson("seed/chapters.json", new TypeReference<>() {});
        int count = 0;
        for (Chapter item : items) {
            if (!chapterRepo.existsById(item.getCode())) {
                chapterRepo.save(item);
                count++;
            }
        }
        log.info("Chapters: {} new / {} total in file", count, items.size());
    }

    private void importLessons() throws Exception {
        List<Lesson> items = readJson("seed/lessons.json", new TypeReference<>() {});
        int count = 0;
        for (Lesson item : items) {
            if (!lessonRepo.existsById(item.getCode())) {
                lessonRepo.save(item);
                count++;
            }
        }
        log.info("Lessons: {} new / {} total in file", count, items.size());
    }

    private void importContentItems() throws Exception {
        List<ContentItem> items = readJson("seed/content_items.json", new TypeReference<>() {});
        int count = 0;
        for (ContentItem item : items) {
            if (!contentItemRepo.existsById(item.getCode())) {
                contentItemRepo.save(item);
                count++;
            }
        }
        log.info("ContentItems: {} new / {} total in file", count, items.size());
    }

    private void importBossConfigs() throws Exception {
        List<BossConfig> items = readJson("seed/boss_configs.json", new TypeReference<>() {});
        int count = 0;
        for (BossConfig item : items) {
            if (!bossConfigRepo.existsById(item.getCode())) {
                bossConfigRepo.save(item);
                count++;
            }
        }
        log.info("BossConfigs: {} new / {} total in file", count, items.size());
    }

    private void importBossPoolTasks() throws Exception {
        List<BossPoolTask> items = readJson("seed/boss_tasks.json", new TypeReference<>() {});
        int count = 0;
        for (BossPoolTask item : items) {
            if (!bossPoolTaskRepo.existsByTaskCode(item.getTaskCode())) {
                bossPoolTaskRepo.save(item);
                count++;
            }
        }
        log.info("BossPoolTasks: {} new / {} total in file", count, items.size());
    }

    private <T> T readJson(String path, TypeReference<T> typeRef) throws Exception {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(is, typeRef);
        }
    }
}
