package com.ll.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.common.exception.BizException;
import com.ll.content.entity.KnowledgePoint;
import com.ll.content.entity.Question;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.QuestionBankRepository;
import com.ll.content.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepo;
    private final KnowledgePointRepository kpRepo;
    private final QuestionBankRepository bankRepo;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepository questionRepo, KnowledgePointRepository kpRepo,
                           QuestionBankRepository bankRepo, ObjectMapper objectMapper) {
        this.questionRepo = questionRepo;
        this.kpRepo = kpRepo;
        this.bankRepo = bankRepo;
        this.objectMapper = objectMapper;
    }

    public Page<Question> listByBank(Long bankId, int page, int size) {
        return questionRepo.findByBankId(bankId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public Question getById(Long id) {
        return questionRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "题目不存在"));
    }

    /**
     * 为指定题库的知识点批量生成题目
     */
    @Transactional
    public List<Question> generate(Long bankId, List<Long> kpIds, List<String> types, int count) {
        bankRepo.findById(bankId).orElseThrow(() -> new BizException(404, "题库不存在"));

        // 获取目标知识点
        List<KnowledgePoint> targetKps;
        if (kpIds != null && !kpIds.isEmpty()) {
            targetKps = kpRepo.findAllById(kpIds);
        } else {
            targetKps = kpRepo.findByBankId(bankId);
        }
        if (targetKps.isEmpty()) {
            throw new BizException("题库中没有知识点，无法生成题目");
        }

        // 获取同库所有知识点（用于干扰项）
        List<KnowledgePoint> allKps = kpRepo.findByBankId(bankId);

        List<String> questionTypes = (types != null && !types.isEmpty())
                ? types : List.of("en2zh_choice", "zh2en_choice", "fill_blank", "translate");

        List<Question> generated = new ArrayList<>();
        int perKp = Math.max(1, count / targetKps.size());
        int remaining = count;

        Collections.shuffle(targetKps);

        for (KnowledgePoint kp : targetKps) {
            if (remaining <= 0) break;
            int thisRound = Math.min(perKp, remaining);

            for (int i = 0; i < thisRound && remaining > 0; i++) {
                String qType = questionTypes.get(i % questionTypes.size());
                Question q = generateOne(kp, qType, allKps, null);
                if (q != null) {
                    q.setBankId(bankId);
                    generated.add(q);
                    remaining--;
                }
            }
        }

        List<Question> saved = questionRepo.saveAll(generated);
        updateBankQuestionCount(bankId);
        return saved;
    }

    private Question generateOne(KnowledgePoint kp, String type, List<KnowledgePoint> pool, String grade) {
        return switch (type) {
            case "en2zh_choice" -> generateEn2ZhChoice(kp, pool);
            case "zh2en_choice" -> generateZh2EnChoice(kp, pool);
            case "fill_blank" -> generateFillBlank(kp, pool, grade);
            case "translate" -> generateTranslate(kp, grade);
            default -> null;
        };
    }

    /**
     * 英译中选择题：给英文单词，选中文释义
     */
    private Question generateEn2ZhChoice(KnowledgePoint kp, List<KnowledgePoint> pool) {
        List<String> distractors = pickDistractors(kp, pool, true);
        if (distractors.size() < 3) return null;

        List<String> options = new ArrayList<>(distractors.subList(0, 3));
        options.add(kp.getMeaningZh());
        Collections.shuffle(options);

        Question q = new Question();
        q.setKpId(kp.getId());
        q.setType("en2zh_choice");
        q.setStem("\"" + kp.getContent() + "\" 的中文意思是？");
        q.setOptions(toJson(options));
        q.setAnswer(kp.getMeaningZh());
        q.setExplanation("\"" + kp.getContent() + "\" " + (kp.getPhonetic() != null ? kp.getPhonetic() + " " : "") + "的意思是「" + kp.getMeaningZh() + "」");
        q.setDifficulty(kp.getDifficulty());
        q.setCreatedBy("template");
        return q;
    }

    /**
     * 中译英选择题：给中文释义，选英文单词
     */
    private Question generateZh2EnChoice(KnowledgePoint kp, List<KnowledgePoint> pool) {
        List<String> distractors = pickDistractors(kp, pool, false);
        if (distractors.size() < 3) return null;

        List<String> options = new ArrayList<>(distractors.subList(0, 3));
        options.add(kp.getContent());
        Collections.shuffle(options);

        Question q = new Question();
        q.setKpId(kp.getId());
        q.setType("zh2en_choice");
        q.setStem("\"" + kp.getMeaningZh() + "\" 的英文是？");
        q.setOptions(toJson(options));
        q.setAnswer(kp.getContent());
        q.setExplanation("「" + kp.getMeaningZh() + "」对应的英文是 \"" + kp.getContent() + "\"");
        q.setDifficulty(kp.getDifficulty());
        q.setCreatedBy("template");
        return q;
    }

    /**
     * 填空题：小学=选词填空（带options），初中/高中=输入式填空
     */
    private Question generateFillBlank(KnowledgePoint kp, List<KnowledgePoint> pool, String grade) {
        if (kp.getExampleSentence() == null || kp.getExampleSentence().isEmpty()) {
            // 没有例句时，生成简单的“单词填空”：给中文意思，填英文
            if ("primary".equals(grade)) {
                List<String> distractors = pickDistractors(kp, pool, false);
                if (distractors.size() < 3) return null;
                List<String> options = new ArrayList<>(distractors.subList(0, 3));
                options.add(kp.getContent());
                Collections.shuffle(options);

                Question q = new Question();
                q.setKpId(kp.getId());
                q.setType("fill_blank");
                q.setStem("请选择正确的单词：\n“" + kp.getMeaningZh() + "”的英文是 ______");
                q.setOptions(toJson(options));
                q.setAnswer(kp.getContent());
                q.setExplanation("正确答案是 \"" + kp.getContent() + "\"（" + kp.getMeaningZh() + "）");
                q.setDifficulty(kp.getDifficulty());
                q.setCreatedBy("template");
                return q;
            }
            return null;
        }

        String sentence = kp.getExampleSentence();
        String blanked = sentence.replaceAll("(?i)\\b" + escapeRegex(kp.getContent()) + "\\b", "______");
        if (blanked.equals(sentence)) {
            blanked = sentence.replaceAll("(?i)" + escapeRegex(kp.getContent()), "______");
        }

        Question q = new Question();
        q.setKpId(kp.getId());
        q.setType("fill_blank");

        if ("primary".equals(grade)) {
            // 小学：选词填空
            List<String> distractors = pickDistractors(kp, pool, false);
            if (distractors.size() < 3) return null;
            List<String> options = new ArrayList<>(distractors.subList(0, 3));
            options.add(kp.getContent());
            Collections.shuffle(options);
            q.setOptions(toJson(options));
            q.setStem("请选择正确的单词填入空格：\n" + blanked);
        } else {
            // 初中/高中：输入式
            q.setStem("请在空格处填入正确的单词：\n" + blanked);
        }

        q.setAnswer(kp.getContent().toLowerCase());
        q.setExplanation("正确答案是 \"" + kp.getContent() + "\"（" + kp.getMeaningZh() + "）");
        q.setDifficulty(kp.getDifficulty());
        q.setCreatedBy("template");
        return q;
    }

    /**
     * 翻译题：小学=单词排序，初中=半填空，高中=完整翻译
     */
    private Question generateTranslate(KnowledgePoint kp, String grade) {
        if (kp.getExampleSentence() == null) return null;

        Question q = new Question();
        q.setKpId(kp.getId());
        q.setType("translate");
        q.setDifficulty(kp.getDifficulty());
        q.setCreatedBy("template");

        if ("primary".equals(grade)) {
            // 小学：排序题 — 给中文，把英文单词排序
            if (kp.getExampleZh() == null) return null;
            String sentence = kp.getExampleSentence().replaceAll("[.!?]+$", "").trim();
            String[] words = sentence.split("\\s+");
            if (words.length < 2) return null;

            List<String> shuffled = new ArrayList<>(Arrays.asList(words));
            Collections.shuffle(shuffled);
            // 确保打乱后不和原文一样
            int attempts = 0;
            while (String.join(" ", shuffled).equalsIgnoreCase(sentence) && attempts < 5) {
                Collections.shuffle(shuffled);
                attempts++;
            }

            Map<String, Object> extraData = new LinkedHashMap<>();
            extraData.put("shuffledWords", shuffled);
            extraData.put("correctOrder", Arrays.asList(words));

            q.setStem("请将以下单词排列成正确的句子：\n" + kp.getExampleZh());
            q.setAnswer(String.join(" ", words));
            q.setExtraData(toJson(extraData));
            q.setExplanation("正确句子：" + kp.getExampleSentence());
            return q;

        } else if ("junior".equals(grade)) {
            // 初中：半填空翻译 — 给中文，英文句子挖空关键词
            if (kp.getExampleZh() == null) return null;
            String sentence = kp.getExampleSentence();
            String word = kp.getContent();
            String template = sentence.replaceAll("(?i)\\b" + escapeRegex(word) + "\\b", "______");
            if (template.equals(sentence)) {
                template = sentence.replaceAll("(?i)" + escapeRegex(word), "______");
            }

            long blankCount = template.chars().filter(c -> c == '_').count() / 6;
            List<String> blanks = new ArrayList<>();
            for (int i = 0; i < blankCount; i++) blanks.add(word.toLowerCase());

            Map<String, Object> extraData = new LinkedHashMap<>();
            extraData.put("template", template);
            extraData.put("blanks", blanks);

            q.setStem("请根据中文意思填写空缺单词：\n" + kp.getExampleZh());
            q.setAnswer(word.toLowerCase());
            q.setExtraData(toJson(extraData));
            q.setExplanation("完整句子：" + kp.getExampleSentence());
            return q;

        } else {
            // 高中：完整翻译
            if (kp.getExampleZh() == null) return null;
            q.setStem("请将以下句子翻译成英文：\n" + kp.getExampleZh());
            q.setAnswer(kp.getExampleSentence());
            q.setExplanation("参考译文：" + kp.getExampleSentence());
            return q;
        }
    }

    /**
     * 从知识点池中选取干扰项
     * @param useMeaning true=返回中文释义（英译中题）, false=返回英文（中译英题）
     */
    private List<String> pickDistractors(KnowledgePoint target, List<KnowledgePoint> pool, boolean useMeaning) {
        // 优先同难度
        List<KnowledgePoint> sameDiff = pool.stream()
                .filter(kp -> !kp.getId().equals(target.getId()))
                .filter(kp -> kp.getDifficulty() != null && kp.getDifficulty().equals(target.getDifficulty()))
                .collect(Collectors.toList());

        List<KnowledgePoint> others = pool.stream()
                .filter(kp -> !kp.getId().equals(target.getId()))
                .filter(kp -> kp.getDifficulty() == null || !kp.getDifficulty().equals(target.getDifficulty()))
                .collect(Collectors.toList());

        Collections.shuffle(sameDiff);
        Collections.shuffle(others);

        List<String> result = new ArrayList<>();
        String targetVal = useMeaning ? target.getMeaningZh() : target.getContent();

        for (KnowledgePoint kp : sameDiff) {
            String val = useMeaning ? kp.getMeaningZh() : kp.getContent();
            if (val != null && !val.equals(targetVal) && !result.contains(val)) {
                result.add(val);
            }
            if (result.size() >= 3) break;
        }
        for (KnowledgePoint kp : others) {
            if (result.size() >= 3) break;
            String val = useMeaning ? kp.getMeaningZh() : kp.getContent();
            if (val != null && !val.equals(targetVal) && !result.contains(val)) {
                result.add(val);
            }
        }
        return result;
    }

    private void updateBankQuestionCount(Long bankId) {
        bankRepo.findById(bankId).ifPresent(bank -> {
            bank.setQuestionCount((int) questionRepo.countByBankId(bankId));
            bankRepo.save(bank);
        });
    }

    /**
     * 保存 AI 生成的题目（供 service-ai 内部调用）
     */
    @Transactional
    public List<Question> saveAiQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) return List.of();
        List<Question> saved = questionRepo.saveAll(questions);
        saved.stream().map(Question::getBankId).filter(Objects::nonNull).distinct()
                .forEach(this::updateBankQuestionCount);
        return saved;
    }

    /**
     * 为知识库单元的知识点生成题目（无需 bankId）
     */
    @Transactional
    public List<Question> generateForUnit(Long unitId, List<String> types, String grade, int count) {
        List<KnowledgePoint> unitKps = kpRepo.findByUnitIdOrderByDifficultyAsc(unitId);
        if (unitKps.isEmpty()) {
            throw new BizException("该单元暂无知识点");
        }
        return generateForKps(unitKps, types, grade, count);
    }

    /**
     * 从知识点列表生成题目（通用方法，供 unit / studySet 等场景复用）
     */
    @Transactional
    public List<Question> generateForKps(List<KnowledgePoint> kps, List<String> types, String grade, int count) {
        List<String> questionTypes = (types != null && !types.isEmpty())
                ? types : List.of("en2zh_choice", "zh2en_choice", "fill_blank", "translate");

        List<Question> generated = new ArrayList<>();
        int perKp = Math.max(1, count / kps.size());
        int remaining = count;

        List<KnowledgePoint> shuffled = new ArrayList<>(kps);
        Collections.shuffle(shuffled);

        for (KnowledgePoint kp : shuffled) {
            if (remaining <= 0) break;
            int thisRound = Math.min(perKp, remaining);

            for (int i = 0; i < thisRound && remaining > 0; i++) {
                String qType = questionTypes.get(i % questionTypes.size());
                Question q = generateOne(kp, qType, kps, grade);
                if (q != null) {
                    q.setGrade(grade);
                    generated.add(q);
                    remaining--;
                }
            }
        }

        return questionRepo.saveAll(generated);
    }

    /**
     * 从知识点列表生成题目但不持久化（用于学习集等临时场景）
     */
    public List<Question> generateForKpsInMemory(List<KnowledgePoint> kps, List<String> types, String grade, int count) {
        List<String> questionTypes = (types != null && !types.isEmpty())
                ? types : List.of("en2zh_choice", "zh2en_choice", "fill_blank", "translate");

        List<Question> generated = new ArrayList<>();
        int perKp = Math.max(1, count / kps.size());
        int remaining = count;

        List<KnowledgePoint> shuffled = new ArrayList<>(kps);
        Collections.shuffle(shuffled);

        long tempId = -1;
        for (KnowledgePoint kp : shuffled) {
            if (remaining <= 0) break;
            int thisRound = Math.min(perKp, remaining);

            for (int i = 0; i < thisRound && remaining > 0; i++) {
                String qType = questionTypes.get(i % questionTypes.size());
                Question q = generateOne(kp, qType, kps, grade);
                if (q != null) {
                    q.setId(tempId--);
                    q.setGrade(grade);
                    q.setKpId(null); // 无持久化 KP，清空外键
                    generated.add(q);
                    remaining--;
                }
            }
        }

        return generated;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String escapeRegex(String str) {
        return str.replaceAll("([\\\\\\[\\]{}()*+?.^$|])", "\\\\$1");
    }
}
