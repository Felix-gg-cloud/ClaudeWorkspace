package com.ll.ai.controller;

import com.ll.ai.prompt.PromptTemplates;
import com.ll.ai.prompt.PromptTemplates.PromptPair;
import com.ll.ai.service.AiService;
import com.ll.ai.service.TranslationJudgeService;
import com.ll.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final TranslationJudgeService translationJudgeService;

    public AiController(AiService aiService, TranslationJudgeService translationJudgeService) {
        this.aiService = aiService;
        this.translationJudgeService = translationJudgeService;
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ApiResponse.error(400, "message 不能为空");
        }
        String reply = aiService.chat(message, "chat");
        return ApiResponse.ok(Map.of("reply", reply));
    }

    /**
     * 测试单题 Prompt 模板 + AI 生成
     */
    @PostMapping("/test/generate")
    public ApiResponse<Map<String, String>> testGenerate(@RequestBody Map<String, String> request) {
        String type = request.getOrDefault("type", "en2zh_choice");
        String grade = request.getOrDefault("grade", "初中");
        String word = request.getOrDefault("word", "apple");
        String phonetic = request.get("phonetic");
        String meaning = request.getOrDefault("meaning", "苹果");
        String example = request.get("example");

        PromptPair prompt = PromptTemplates.forType(type, grade, word, phonetic, meaning, example);
        String reply = aiService.chatWithSystem(prompt.system(), prompt.user(), "test_generate");
        return ApiResponse.ok(Map.of("prompt_system", prompt.system(), "prompt_user", prompt.user(), "reply", reply));
    }

    /**
     * AI 翻译评判（初中/高中翻译题）
     */
    @PostMapping("/judge/translate")
    public ApiResponse<Map<String, Object>> judgeTranslate(@RequestBody Map<String, String> request) {
        String stem = request.get("stem");
        String referenceAnswer = request.get("referenceAnswer");
        String userAnswer = request.get("userAnswer");
        String grade = request.getOrDefault("grade", "junior");

        if (stem == null || referenceAnswer == null || userAnswer == null) {
            return ApiResponse.error(400, "缺少必要参数: stem, referenceAnswer, userAnswer");
        }

        Map<String, Object> result = translationJudgeService.judge(stem, referenceAnswer, userAnswer, grade);
        return ApiResponse.ok(result);
    }

    /**
     * AI 练习结果分析
     */
    @PostMapping("/analyze/practice")
    public ApiResponse<Map<String, String>> analyzePractice(@RequestBody Map<String, Object> request) {
        int totalCount = ((Number) request.getOrDefault("totalCount", 0)).intValue();
        int correctCount = ((Number) request.getOrDefault("correctCount", 0)).intValue();
        int accuracy = totalCount > 0 ? (correctCount * 100 / totalCount) : 0;
        String wrongDetails = (String) request.getOrDefault("wrongDetails", "");
        String grade = (String) request.getOrDefault("grade", "");

        String system = "你是一位专业且温暖的英语老师 Lily。请根据学生的练习结果给出简短的分析和建议。" +
                "回复要求：1）先鼓励学生 2）指出薄弱环节 3）给出针对性的学习建议。" +
                "用中文回复，语气亲切，适当使用 emoji，控制在 200 字以内。";

        String user = String.format("学生刚完成一次练习：\n- 年级：%s\n- 总题数：%d\n- 正确：%d，错误：%d\n- 正确率：%d%%\n",
                grade, totalCount, correctCount, totalCount - correctCount, accuracy);
        if (!wrongDetails.isBlank()) {
            user += "- 错题详情：\n" + wrongDetails;
        }

        String reply = aiService.chatWithSystem(system, user, "practice_analyze");
        return ApiResponse.ok(Map.of("analysis", reply));
    }
}
