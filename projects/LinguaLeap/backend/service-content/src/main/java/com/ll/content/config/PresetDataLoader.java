package com.ll.content.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.content.entity.KnowledgePoint;
import com.ll.content.entity.QuestionBank;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.QuestionBankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class PresetDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PresetDataLoader.class);

    private final QuestionBankRepository bankRepo;
    private final KnowledgePointRepository kpRepo;
    private final ObjectMapper objectMapper;

    public PresetDataLoader(QuestionBankRepository bankRepo, KnowledgePointRepository kpRepo, ObjectMapper objectMapper) {
        this.bankRepo = bankRepo;
        this.kpRepo = kpRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        loadPresetBank("小学核心词汇", "primary", "data/primary_words.json");
        loadPresetBank("初中核心词汇", "junior", "data/junior_words.json");
        loadPresetBank("高中核心词汇", "senior", "data/senior_words.json");
    }

    private void loadPresetBank(String name, String grade, String resourcePath) {
        if (bankRepo.existsByNameAndType(name, "preset")) {
            log.info("预制题库已存在，跳过: {}", name);
            return;
        }

        try {
            InputStream is = new ClassPathResource(resourcePath).getInputStream();
            List<Map<String, Object>> words = objectMapper.readValue(is, new TypeReference<>() {});

            QuestionBank bank = new QuestionBank();
            bank.setName(name);
            bank.setDescription(grade + "阶段英语核心词汇，共 " + words.size() + " 词");
            bank.setGrade(grade);
            bank.setType("preset");
            bank.setStatus("active");
            bank = bankRepo.save(bank);

            for (Map<String, Object> w : words) {
                KnowledgePoint kp = new KnowledgePoint();
                kp.setBankId(bank.getId());
                kp.setType("word");
                kp.setContent((String) w.get("content"));
                kp.setPhonetic((String) w.get("phonetic"));
                kp.setMeaningZh((String) w.get("meaning_zh"));
                kp.setExampleSentence((String) w.get("example_sentence"));
                kp.setExampleZh((String) w.get("example_zh"));
                kp.setDifficulty((Integer) w.get("difficulty"));
                Object tags = w.get("tags");
                if (tags != null) {
                    kp.setTags(objectMapper.writeValueAsString(tags));
                }
                kpRepo.save(kp);
            }

            bank.setKpCount(words.size());
            bankRepo.save(bank);

            log.info("预制题库导入完成: {} ({} 词)", name, words.size());
        } catch (Exception e) {
            log.error("预制题库导入失败: {}", name, e);
        }
    }
}
