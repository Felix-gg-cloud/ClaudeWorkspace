package com.ll.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.common.exception.BizException;
import com.ll.content.entity.KnowledgePoint;
import com.ll.content.entity.LearningItem;
import com.ll.content.entity.MistakeRecord;
import com.ll.content.entity.PracticeSession;
import com.ll.content.entity.Question;
import com.ll.content.entity.UserQuestionHistory;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.LearningItemRepository;
import com.ll.content.repository.MistakeRecordRepository;
import com.ll.content.repository.PracticeSessionRepository;
import com.ll.content.repository.QuestionRepository;
import com.ll.content.repository.UserQuestionHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PracticeService {

    private final PracticeSessionRepository sessionRepo;
    private final QuestionRepository questionRepo;
    private final QuestionService questionService;
    private final KnowledgePointRepository kpRepo;
    private final LearningItemRepository itemRepo;
    private final MistakeRecordRepository mistakeRepo;
    private final UserQuestionHistoryRepository historyRepo;
    private final SrsService srsService;
    private final ObjectMapper objectMapper;

    // 内存中维护每个 session 的题目队列和进度
    private final ConcurrentHashMap<Long, SessionState> sessionStates = new ConcurrentHashMap<>();

    public PracticeService(PracticeSessionRepository sessionRepo, QuestionRepository questionRepo,
                           QuestionService questionService, KnowledgePointRepository kpRepo,
                           LearningItemRepository itemRepo,
                           MistakeRecordRepository mistakeRepo, UserQuestionHistoryRepository historyRepo,
                           SrsService srsService, ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.questionRepo = questionRepo;
        this.questionService = questionService;
        this.kpRepo = kpRepo;
        this.itemRepo = itemRepo;
        this.mistakeRepo = mistakeRepo;
        this.historyRepo = historyRepo;
        this.srsService = srsService;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始练习：创建 session，从已有题目中抽取
     * 如果题目不足，提示用户先通过 AI 出题
     */
    @Transactional
    public Map<String, Object> start(Long userId, Long bankId, String questionType, String grade, int count) {
        // 支持多题型（逗号分隔）
        List<String> types = Arrays.stream(questionType.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        List<Question> questions;
        if (grade != null && !grade.isBlank()) {
            if (types.size() == 1) {
                questions = questionRepo.findByBankIdAndTypeAndGrade(bankId, types.get(0), grade);
            } else {
                questions = questionRepo.findByBankIdAndTypeInAndGrade(bankId, types, grade);
            }
            // 如果年级匹配不足，也纳入无年级标记的题目
            if (questions.size() < count) {
                List<Question> allTypeQuestions = types.size() == 1
                        ? questionRepo.findByBankIdAndType(bankId, types.get(0))
                        : questionRepo.findByBankIdAndTypeIn(bankId, types);
                Set<Long> existingIds = questions.stream().map(Question::getId).collect(java.util.stream.Collectors.toSet());
                for (Question q : allTypeQuestions) {
                    if (!existingIds.contains(q.getId())) {
                        questions.add(q);
                    }
                }
            }
        } else {
            if (types.size() == 1) {
                questions = questionRepo.findByBankIdAndType(bankId, types.get(0));
            } else {
                questions = questionRepo.findByBankIdAndTypeIn(bankId, types);
            }
        }

        if (questions.isEmpty()) {
            throw new BizException("该题库暂无此题型的题目，请先在「AI 出题」页面生成题目");
        }

        // 随机抽取 count 道题
        Collections.shuffle(questions);
        List<Question> selected = questions.subList(0, Math.min(count, questions.size()));

        // 创建 session
        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setBankId(bankId);
        session.setQuestionType(questionType);
        session.setTotalCount(selected.size());
        session = sessionRepo.save(session);

        // 记录内存状态
        SessionState state = new SessionState(selected);
        sessionStates.put(session.getId(), state);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("totalCount", selected.size());
        result.put("questionType", questionType);
        return result;
    }

    /**
     * 基于知识库单元开始练习（混合策略）：
     * 1. 查询该单元已有题目，排除该用户已做过的
     * 2. 未做过的题目够用 → 直接用（零延迟）
     * 3. 不够 → AI 补生成差额部分
     */
    @Transactional
    public Map<String, Object> startByUnit(Long userId, Long unitId, String questionType, String grade, int count) {
        List<String> types = Arrays.stream(questionType.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        // 查询该单元下的所有知识点 ID
        List<KnowledgePoint> kps = kpRepo.findByUnitIdOrderByDifficultyAsc(unitId);
        if (kps.isEmpty()) {
            throw new BizException("该单元暂无知识点");
        }
        List<Long> kpIds = kps.stream().map(KnowledgePoint::getId).toList();

        // 按知识点查询该单元所有匹配题目
        List<Question> allQuestions;
        if (grade != null && !grade.isBlank()) {
            allQuestions = new ArrayList<>(questionRepo.findByKpIdInAndTypeInAndGrade(kpIds, types, grade));
            if (allQuestions.size() < count) {
                List<Question> fallback = questionRepo.findByKpIdInAndTypeIn(kpIds, types);
                Set<Long> existingIds = allQuestions.stream().map(Question::getId).collect(java.util.stream.Collectors.toSet());
                for (Question q : fallback) {
                    if (!existingIds.contains(q.getId())) allQuestions.add(q);
                }
            }
        } else {
            allQuestions = new ArrayList<>(questionRepo.findByKpIdInAndTypeIn(kpIds, types));
        }

        // 排除该用户已做过的题目
        List<Question> fresh;
        if (!allQuestions.isEmpty()) {
            List<Long> allIds = allQuestions.stream().map(Question::getId).toList();
            Set<Long> doneIds = new HashSet<>(historyRepo.findDoneQuestionIds(userId, allIds));
            fresh = allQuestions.stream().filter(q -> !doneIds.contains(q.getId())).collect(java.util.stream.Collectors.toList());
        } else {
            fresh = new ArrayList<>();
        }

        // 不够则 AI 补生成
        if (fresh.size() < count) {
            int deficit = count - fresh.size();
            int genCount = Math.max(deficit, 10);
            List<Question> generated = questionService.generateForUnit(unitId, types, grade, genCount);
            fresh.addAll(generated);
        }

        if (fresh.isEmpty()) {
            throw new BizException("该单元知识点不足以生成练习题目");
        }

        Collections.shuffle(fresh);
        List<Question> selected = fresh.subList(0, Math.min(count, fresh.size()));

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setUnitId(unitId);
        session.setQuestionType(questionType);
        session.setTotalCount(selected.size());
        session = sessionRepo.save(session);

        SessionState state = new SessionState(selected);
        sessionStates.put(session.getId(), state);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("totalCount", selected.size());
        result.put("questionType", questionType);
        return result;
    }

    /**
     * 基于学习集开始练习：
     * 将 LearningItem 转换为 KnowledgePoint，用模板引擎生成题目
     */
    @Transactional
    public Map<String, Object> startByStudySet(Long userId, Long studySetId, String questionType, String grade, int count) {
        List<String> types = Arrays.stream(questionType.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        List<LearningItem> items = itemRepo.findByStudySetIdOrderByIdAsc(studySetId);
        if (items.isEmpty()) {
            throw new BizException("该学习集暂无学习项");
        }

        // 将 LearningItem 转换为临时 KnowledgePoint（复用出题逻辑）
        // 使用负数作为临时 ID，避免与数据库 ID 冲突，pickDistractors 需要 ID 做去重
        List<KnowledgePoint> tempKps = new ArrayList<>();
        long fakeId = -1;
        for (LearningItem item : items) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setId(fakeId--);
            kp.setContent(item.getContent());
            kp.setMeaningZh(item.getMeaningZh());
            kp.setPhonetic(item.getPhonetic());
            kp.setExampleSentence(item.getExampleSentence());
            kp.setExampleZh(item.getExampleZh());
            kp.setDifficulty(item.getDifficulty() != null ? item.getDifficulty() : 1);
            kp.setType(item.getCategory() != null ? item.getCategory() : "word");
            tempKps.add(kp);
        }

        // 生成题目但不持久化（学习集题目为临时题目，不入库）
        List<Question> generated = questionService.generateForKpsInMemory(tempKps, types, grade, count);

        if (generated.isEmpty()) {
            throw new BizException("无法根据学习集内容生成练习题目");
        }

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setQuestionType(questionType);
        session.setTotalCount(generated.size());
        session = sessionRepo.save(session);

        SessionState state = new SessionState(generated);
        sessionStates.put(session.getId(), state);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("totalCount", generated.size());
        result.put("questionType", questionType);
        return result;
    }

    /**
     * 获取下一题
     */
    public Map<String, Object> next(Long sessionId) {
        SessionState state = getState(sessionId);
        if (state.isFinished()) {
            throw new BizException("练习已完成，没有更多题目");
        }

        Question q = state.current();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questionId", q.getId());
        result.put("type", q.getType());
        result.put("stem", q.getStem());
        result.put("difficulty", q.getDifficulty());
        result.put("grade", q.getGrade());

        if (q.getOptions() != null && !q.getOptions().isEmpty()) {
            result.put("options", parseJsonArray(q.getOptions()));
        }

        // Phase 2b 新字段
        if (q.getWords() != null) result.put("words", parseJsonValue(q.getWords()));
        if (q.getKnowledgePoints() != null) result.put("knowledgePoints", q.getKnowledgePoints());
        if (q.getExampleSentence() != null) result.put("exampleSentence", q.getExampleSentence());
        if (q.getExampleZh() != null) result.put("exampleZh", q.getExampleZh());
        if (q.getExtraData() != null) result.put("extraData", parseJsonValue(q.getExtraData()));

        result.put("progress", Map.of(
                "current", state.currentIndex + 1,
                "total", state.questions.size(),
                "correctCount", state.correctCount
        ));

        return result;
    }

    /**
     * 提交答案，即时反馈
     */
    @Transactional
    public Map<String, Object> answer(Long sessionId, Long userId, Long questionId, String userAnswer) {
        SessionState state = getState(sessionId);
        Question q = state.current();

        if (q.getId() != null && !q.getId().equals(questionId)) {
            throw new BizException("答题顺序错误");
        }

        boolean correct = checkAnswer(q, userAnswer);
        if (correct) {
            state.correctCount++;
        } else if (q.getId() != null && q.getId() > 0) {
            // 仅对持久化题目记录错题（临时题目 ID 为负数，跳过）
            MistakeRecord mistake = new MistakeRecord();
            mistake.setUserId(userId);
            mistake.setQuestionId(q.getId());
            mistake.setKpId(q.getKpId());
            mistake.setUserAnswer(userAnswer);
            mistake.setCorrectAnswer(q.getAnswer());
            mistake.setQuestionType(q.getType());
            mistakeRepo.save(mistake);
        }

        // 记录做题历史（仅持久化题目，临时题目跳过）
        if (q.getId() != null && q.getId() > 0) {
            UserQuestionHistory history = new UserQuestionHistory();
            history.setUserId(userId);
            history.setQuestionId(q.getId());
            history.setCorrect(correct);
            historyRepo.save(history);
        }

        // 更新 SRS 卡片
        if (q.getKpId() != null) {
            srsService.review(userId, q.getKpId(), correct);
        }

        state.currentIndex++;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", correct);
        result.put("correctAnswer", q.getAnswer());
        result.put("explanation", q.getExplanation());
        if (q.getKnowledgePoints() != null) result.put("knowledgePoints", q.getKnowledgePoints());
        if (q.getExampleSentence() != null) result.put("exampleSentence", q.getExampleSentence());
        if (q.getExampleZh() != null) result.put("exampleZh", q.getExampleZh());
        if (q.getWords() != null) result.put("words", parseJsonValue(q.getWords()));
        result.put("sessionProgress", Map.of(
                "current", state.currentIndex,
                "total", state.questions.size(),
                "correctCount", state.correctCount
        ));

        return result;
    }

    /**
     * 结束练习
     */
    @Transactional
    public Map<String, Object> finish(Long sessionId) {
        SessionState state = sessionStates.get(sessionId);
        PracticeSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BizException(404, "练习会话不存在"));

        int correctCount = state != null ? state.correctCount : session.getCorrectCount();
        int totalCount = state != null ? state.questions.size() : session.getTotalCount();

        session.setCorrectCount(correctCount);
        session.setTotalCount(totalCount);
        session.setFinishedAt(LocalDateTime.now());
        sessionRepo.save(session);

        // 清理内存
        sessionStates.remove(sessionId);

        return buildResult(session);
    }

    /**
     * 获取练习结果
     */
    public Map<String, Object> result(Long sessionId) {
        PracticeSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BizException(404, "练习会话不存在"));
        return buildResult(session);
    }

    private Map<String, Object> buildResult(PracticeSession session) {
        int total = session.getTotalCount();
        int correct = session.getCorrectCount();
        double accuracy = total > 0 ? (double) correct / total : 0;

        long duration = 0;
        if (session.getStartedAt() != null && session.getFinishedAt() != null) {
            duration = Duration.between(session.getStartedAt(), session.getFinishedAt()).getSeconds();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("totalCount", total);
        result.put("correctCount", correct);
        result.put("accuracy", Math.round(accuracy * 100) / 100.0);
        result.put("duration", duration);
        return result;
    }

    private boolean checkAnswer(Question q, String userAnswer) {
        if (userAnswer == null) return false;
        String answer = q.getAnswer();
        String type = q.getType();

        // 选择题精确匹配
        if ("en2zh_choice".equals(type) || "zh2en_choice".equals(type)) {
            return answer.equals(userAnswer);
        }

        // 填空题
        if ("fill_blank".equals(type)) {
            // 不区分大小写对比
            return answer.trim().equalsIgnoreCase(userAnswer.trim());
        }

        // 翻译题 - 小学排序题自动判断
        if ("translate".equals(type)) {
            String grade = q.getGrade();
            if ("primary".equals(grade)) {
                // 小学排序题：比较单词顺序
                return answer.trim().equalsIgnoreCase(userAnswer.trim());
            }
            if ("junior".equals(grade)) {
                // 初中半填空：逐空比较（前端会发送逗号分隔的答案）
                return answer.trim().equalsIgnoreCase(userAnswer.trim());
            }
            // 高中翻译：需要 AI 评判，先做简单比较
            // 前端会单独调用 /api/ai/judge/translate，这里做宽松匹配
            return answer.trim().equalsIgnoreCase(userAnswer.trim());
        }

        return answer.trim().equalsIgnoreCase(userAnswer.trim());
    }

    private SessionState getState(Long sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            throw new BizException("练习会话已过期，请重新开始");
        }
        return state;
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Object parseJsonValue(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    /**
     * 内存中的练习会话状态
     */
    private static class SessionState {
        final List<Question> questions;
        int currentIndex = 0;
        int correctCount = 0;

        SessionState(List<Question> questions) {
            this.questions = new ArrayList<>(questions);
        }

        Question current() {
            return questions.get(currentIndex);
        }

        boolean isFinished() {
            return currentIndex >= questions.size();
        }
    }
}
