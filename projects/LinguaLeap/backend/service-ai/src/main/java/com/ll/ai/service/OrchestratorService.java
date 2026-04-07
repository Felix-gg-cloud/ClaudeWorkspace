package com.ll.ai.service;

import com.ll.ai.client.ContentServiceClient;
import com.ll.ai.entity.StudentProfile;
import com.ll.ai.prompt.TeacherPrompts;
import com.ll.ai.prompt.TeachingProtocol;
import com.ll.ai.repository.StudentProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Phase 5a — 教学编排引擎（Orchestrator）
 * 
 * 职责：
 * 1. 根据学生 Learner Model 决定当前教学阶段（复习→新学→练习→总结）
 * 2. 查询约束数据（词汇/语法/样本），组装受约束的 System Prompt
 * 3. 管理教学会话状态（上次进度、本次计划）
 */
@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    private final StudentProfileRepository profileRepo;
    private final ContentServiceClient contentClient;
    private final ObjectMapper objectMapper;

    // 级别递进关系
    private static final List<String> LEVEL_ORDER = List.of(
            "L3", "L4", "L5", "L6", "L7", "L8", "L9", "L10", "L11", "L12"
    );

    public OrchestratorService(StudentProfileRepository profileRepo,
                               ContentServiceClient contentClient) {
        this.profileRepo = profileRepo;
        this.contentClient = contentClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 决定下一个教学阶段，返回完整的 System Prompt
     */
    public OrchestratedPrompt orchestrate(Long userId) {
        StudentProfile profile = profileRepo.findByUserId(userId).orElse(null);
        if (profile == null || profile.getLevelCode() == null) {
            // 没有画像或没有级别 → 需要先做入学评估
            return new OrchestratedPrompt("assessment", TeacherPrompts.ASSESSMENT_WELCOME, null);
        }

        String levelCode = profile.getLevelCode();
        String phase = determinePhase(profile);

        // 查询约束数据
        String vocabBlock = buildVocabConstraint(levelCode);
        String grammarBlock = buildGrammarConstraint(levelCode);
        String sampleBlock = buildSampleReference(levelCode);
        String profileSummary = buildProfileSummary(profile);

        // 根据阶段生成对应 prompt 片段
        String phaseBlock = switch (phase) {
            case "review" -> buildReviewBlock(profile);
            case "learn" -> buildLearnBlock(profile);
            case "practice" -> buildPracticeBlock(profile);
            case "summary" -> buildSummaryBlock(profile);
            default -> buildLearnBlock(profile);
        };

        String fullPrompt = TeachingProtocol.buildTeachingPrompt(
                TeacherPrompts.TEACHER_PERSONA,
                profileSummary,
                vocabBlock,
                grammarBlock,
                sampleBlock,
                phaseBlock
        );

        // 更新状态
        updateSessionState(profile, phase);

        return new OrchestratedPrompt(phase, fullPrompt, levelCode);
    }

    /**
     * 更新学生答题后的 Learner Model
     */
    public void updateAfterAnswer(Long userId, boolean correct, String kp) {
        profileRepo.findByUserId(userId).ifPresent(profile -> {
            // 更新统计
            profile.setTotalAnswered(
                    (profile.getTotalAnswered() != null ? profile.getTotalAnswered() : 0) + 1);
            if (correct) {
                profile.setTotalCorrect(
                        (profile.getTotalCorrect() != null ? profile.getTotalCorrect() : 0) + 1);
            }

            // 更新薄弱标签
            if (!correct && kp != null) {
                addWeakTag(profile, kp);
            } else if (correct && kp != null) {
                removeWeakTag(profile, kp);
            }

            profileRepo.save(profile);
        });
    }

    // ========== 内部方法 ==========

    /**
     * 决定当前教学阶段
     */
    private String determinePhase(StudentProfile profile) {
        String stateJson = profile.getLastSessionState();
        if (stateJson != null) {
            try {
                JsonNode state = objectMapper.readTree(stateJson);
                String lastPhase = state.path("phase").asText("");
                // 状态机：review → learn → practice → summary → review
                return switch (lastPhase) {
                    case "review" -> "learn";
                    case "learn" -> "practice";
                    case "practice" -> "summary";
                    default -> "review";
                };
            } catch (Exception e) {
                log.warn("解析 session state 失败", e);
            }
        }

        // 首次或状态丢失：有薄弱点先复习，否则学新知识
        String weakTags = profile.getWeakTags();
        if (weakTags != null && !weakTags.equals("[]") && !weakTags.isBlank()) {
            return "review";
        }
        return "learn";
    }

    /**
     * 构建词汇约束块
     */
    private String buildVocabConstraint(String levelCode) {
        try {
            Map<String, Object> data = contentClient.fetchConstraintData(
                    "/api/content/constraints/vocab/" + levelCode);
            if (data == null) return "";

            @SuppressWarnings("unchecked")
            List<String> words = (List<String>) data.get("words");
            String vocabList = words != null ? String.join(", ", words) : "";

            // i+1: 获取下一级词汇取少量
            String nextLevel = getNextLevel(levelCode);
            String nextVocabSample = "";
            if (nextLevel != null) {
                Map<String, Object> nextData = contentClient.fetchConstraintData(
                        "/api/content/constraints/vocab/" + nextLevel);
                if (nextData != null) {
                    @SuppressWarnings("unchecked")
                    List<String> nextWords = (List<String>) nextData.get("words");
                    if (nextWords != null && !nextWords.isEmpty()) {
                        Collections.shuffle(nextWords);
                        nextVocabSample = String.join(", ",
                                nextWords.subList(0, Math.min(15, nextWords.size())));
                    }
                }
            }

            return TeachingProtocol.vocabConstraint(levelCode, vocabList, nextVocabSample);
        } catch (Exception e) {
            log.error("构建词汇约束失败", e);
            return "";
        }
    }

    /**
     * 构建语法约束块
     */
    private String buildGrammarConstraint(String levelCode) {
        try {
            Map<String, Object> data = contentClient.fetchConstraintData(
                    "/api/content/constraints/grammar/" + levelCode);
            if (data == null) return "";

            @SuppressWarnings("unchecked")
            List<String> points = (List<String>) data.get("points");
            String grammarList = points != null ? String.join("\n• ", points) : "";

            return TeachingProtocol.grammarConstraint(levelCode, "• " + grammarList);
        } catch (Exception e) {
            log.error("构建语法约束失败", e);
            return "";
        }
    }

    /**
     * 构建样本参考块
     */
    private String buildSampleReference(String levelCode) {
        try {
            Map<String, Object> data = contentClient.fetchConstraintData(
                    "/api/content/constraints/samples/" + levelCode);
            if (data == null) return null;

            String excerpt = (String) data.get("excerpt");
            if (excerpt == null || excerpt.isBlank()) return null;

            return TeachingProtocol.sampleReference(levelCode, excerpt);
        } catch (Exception e) {
            log.error("构建样本参考失败", e);
            return null;
        }
    }

    /**
     * 构建学生画像摘要
     */
    private String buildProfileSummary(StudentProfile p) {
        StringBuilder sb = new StringBuilder("【当前学生画像】\n");
        sb.append("级别：").append(p.getLevelCode()).append("\n");
        if (p.getVocabularyLevel() != null) sb.append("词汇水平：").append(p.getVocabularyLevel()).append("\n");
        if (p.getGrammarLevel() != null) sb.append("语法水平：").append(p.getGrammarLevel()).append("\n");
        if (p.getInterests() != null) sb.append("兴趣：").append(p.getInterests()).append("\n");
        if (p.getWeakTags() != null) sb.append("薄弱标签：").append(p.getWeakTags()).append("\n");

        int answered = p.getTotalAnswered() != null ? p.getTotalAnswered() : 0;
        int correct = p.getTotalCorrect() != null ? p.getTotalCorrect() : 0;
        if (answered > 0) {
            sb.append(String.format("历史正确率：%d/%d (%.0f%%)\n", correct, answered,
                    100.0 * correct / answered));
        }
        return sb.toString();
    }

    private String buildReviewBlock(StudentProfile profile) {
        String weakTags = profile.getWeakTags() != null ? profile.getWeakTags() : "无";
        return TeachingProtocol.reviewPhase(weakTags, "（由 SRS 系统提供，暂无到期卡片）");
    }

    private String buildLearnBlock(StudentProfile profile) {
        // 简单策略：从语法点中选一个未掌握的作为教学目标
        String targetKp = "本级别课标要求的下一个知识点";
        return TeachingProtocol.learnPhase(targetKp, "请根据词汇约束和语法约束选择适当的教学内容");
    }

    private String buildPracticeBlock(StudentProfile profile) {
        return TeachingProtocol.practicePhase("本次已学/复习的知识点", "选择题或填空题");
    }

    private String buildSummaryBlock(StudentProfile profile) {
        int answered = profile.getTotalAnswered() != null ? profile.getTotalAnswered() : 0;
        int correct = profile.getTotalCorrect() != null ? profile.getTotalCorrect() : 0;
        String rate = answered > 0 ? String.format("%.0f%%", 100.0 * correct / answered) : "暂无数据";
        return TeachingProtocol.summaryPhase("本次学习内容由系统自动汇总", rate);
    }

    private void updateSessionState(StudentProfile profile, String phase) {
        try {
            String stateJson = objectMapper.writeValueAsString(Map.of("phase", phase));
            profile.setLastSessionState(stateJson);
            profileRepo.save(profile);
        } catch (Exception e) {
            log.error("更新 session state 失败", e);
        }
    }

    private void addWeakTag(StudentProfile profile, String tag) {
        try {
            List<String> tags = parseJsonArray(profile.getWeakTags());
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
            profile.setWeakTags(objectMapper.writeValueAsString(tags));
        } catch (Exception e) {
            log.warn("更新 weak_tags 失败", e);
        }
    }

    private void removeWeakTag(StudentProfile profile, String tag) {
        try {
            List<String> tags = parseJsonArray(profile.getWeakTags());
            tags.remove(tag);
            profile.setWeakTags(objectMapper.writeValueAsString(tags));
        } catch (Exception e) {
            log.warn("更新 weak_tags 失败", e);
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String getNextLevel(String currentLevel) {
        int idx = LEVEL_ORDER.indexOf(currentLevel);
        if (idx >= 0 && idx < LEVEL_ORDER.size() - 1) {
            return LEVEL_ORDER.get(idx + 1);
        }
        return null;
    }

    // ========== DTO ==========

    public record OrchestratedPrompt(String phase, String systemPrompt, String levelCode) {}
}
