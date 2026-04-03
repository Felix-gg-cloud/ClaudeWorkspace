package com.ll.ai.prompt;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Prompt 模板集合 (Phase 2b)
 * 4 题型 × 3 年级 = 12 套出题 Prompt + 1 套翻译评判 Prompt + 1 套知识点解析 Prompt
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    // ========================================================================
    // 英译中选择题 (en2zh_choice) - 3 个年级
    // ========================================================================

    private static final String EN2ZH_CHOICE_PRIMARY_SYSTEM = """
            你是一位小学英语教师，为小学生出英译中选择题。
            规则：
            1. 题干直接给英文单词，问中文意思
            2. 4个中文选项，1个正确+3个干扰项
            3. 干扰项用明显不同类别的词（如水果vs动物vs颜色），让小学生容易区分
            4. 严格按 JSON 格式输出，不要输出任何其他内容""";

    private static final String EN2ZH_CHOICE_PRIMARY_USER = """
            为以下单词出一道英译中选择题（小学难度）：
            单词：{word}
            音标：{phonetic}
            释义：{meaning}

            JSON 格式：
            {"stem":"\\"{word}\\" 的中文意思是？","options":["正确释义","干扰项1","干扰项2","干扰项3"],"answer":"正确释义","explanation":"简要解析","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"包含该单词的简单例句","exampleZh":"例句中文翻译"}""";

    private static final String EN2ZH_CHOICE_JUNIOR_SYSTEM = """
            你是一位初中英语教师，为初中生出英译中选择题。
            规则：
            1. 题干给英文单词，问中文意思
            2. 4个中文选项，干扰项必须是同词性、同类别的真实单词释义（如都是动物/都是动词）
            3. 干扰项有一定迷惑性但不能过于相近
            4. 严格按 JSON 格式输出""";

    private static final String EN2ZH_CHOICE_JUNIOR_USER = """
            为以下单词出一道英译中选择题（初中难度）：
            单词：{word}
            音标：{phonetic}
            释义：{meaning}

            JSON 格式：
            {"stem":"\\"{word}\\" 的中文意思是？","options":["正确释义","同类干扰1","同类干扰2","同类干扰3"],"answer":"正确释义","explanation":"解析+助记","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"包含该单词的例句","exampleZh":"例句中文翻译"}""";

    private static final String EN2ZH_CHOICE_SENIOR_SYSTEM = """
            你是一位高中英语教师，为高中生出英译中选择题。
            规则：
            1. 题干是包含目标单词的英文语境句子，根据语境选择正确释义
            2. 4个选项是该单词的不同含义或近义词的释义，需要结合语境辨析
            3. 干扰项有较强迷惑性
            4. 严格按 JSON 格式输出""";

    private static final String EN2ZH_CHOICE_SENIOR_USER = """
            为以下单词出一道英译中语境选择题（高中难度）：
            单词：{word}
            音标：{phonetic}
            释义：{meaning}

            要求：题干是一个包含该单词的英语句子，让学生根据语境选择正确释义。
            JSON 格式：
            {"stem":"He decided to accept the invitation.\\n句中 \\"accept\\" 的意思是？","options":["正确语境义","近义干扰1","近义干扰2","近义干扰3"],"answer":"正确语境义","explanation":"语境辨析+用法","knowledgePoints":"考察该单词在特定语境下的含义","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"另一个例句","exampleZh":"例句翻译"}""";

    // ========================================================================
    // 中译英 (zh2en) - 3 个年级
    // ========================================================================

    private static final String ZH2EN_PRIMARY_SYSTEM = """
            你是一位小学英语教师，为小学生出中译英选择题。
            规则：
            1. 题干给中文释义，让学生选对应的英文单词
            2. 4个英文选项，干扰项是拼写或含义不同的常见单词
            3. 干扰项要是小学生认识的词，不能太生僻
            4. 严格按 JSON 格式输出""";

    private static final String ZH2EN_PRIMARY_USER = """
            为以下单词出一道中译英选择题（小学难度）：
            单词：{word}
            音标：{phonetic}
            释义：{meaning}

            JSON 格式：
            {"stem":"\\"{meaning}\\" 的英文是？","options":["{word}","干扰词1","干扰词2","干扰词3"],"answer":"{word}","explanation":"解析","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"例句","exampleZh":"例句翻译"}""";

    private static final String ZH2EN_JUNIOR_SYSTEM = """
            你是一位初中英语教师，为初中生出中译英选择题。
            规则：
            1. 题干给中文释义，让学生选对应的英文单词
            2. 4个英文选项，干扰项必须是拼写或发音相近的真实英文单词
            3. 干扰项有一定迷惑性
            4. 严格按 JSON 格式输出""";

    private static final String ZH2EN_JUNIOR_USER = """
            为以下单词出一道中译英选择题（初中难度）：
            单词：{word}
            音标：{phonetic}
            释义：{meaning}

            JSON 格式：
            {"stem":"\\"{meaning}\\" 的英文是？","options":["{word}","相近干扰1","相近干扰2","相近干扰3"],"answer":"{word}","explanation":"解析+拼写注意点","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"例句","exampleZh":"例句翻译"}""";

    private static final String ZH2EN_SENIOR_SYSTEM = """
            你是一位高中英语教师，为高中生出中译英拼写题。
            规则：
            1. 题干给中文释义，让学生自己拼写出英文单词（不提供选项）
            2. answer 填正确的英文单词
            3. explanation 要包含音标、词根词缀、记忆技巧
            4. 严格按 JSON 格式输出，options 设为 null""";

    private static final String ZH2EN_SENIOR_USER = """
            为以下单词出一道中译英拼写题（高中难度）：
            单词：{word}
            音标：{phonetic}
            释义：{meaning}

            JSON 格式（注意 options 为 null，学生需自己拼写）：
            {"stem":"请拼写出 \\"{meaning}\\" 对应的英文单词","options":null,"answer":"{word}","explanation":"音标{phonetic}，记忆技巧...","knowledgePoints":"词根词缀/派生词","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"例句","exampleZh":"例句翻译"}""";

    // ========================================================================
    // 填空题 (fill_blank) - 3 个年级
    // ========================================================================

    private static final String FILL_PRIMARY_SYSTEM = """
            你是一位小学英语教师，为小学生出选择填空题。
            规则：
            1. 造一个简单的英文句子，挖掉目标单词变成 ______
            2. 提供句子的完整中文翻译（stemZh字段）
            3. 提供 3 个选项（3个不同的单词），只有1个正确
            4. 选项用明显不同的词，难度适合小学生
            5. 严格按 JSON 格式输出""";

    private static final String FILL_PRIMARY_USER = """
            为以下单词出一道选择填空题（小学难度）：
            单词：{word}
            释义：{meaning}
            参考例句：{example}

            要求：造一个新的简短英文句子，将目标词替换为 ______，提供3个选词选项。
            JSON 格式：
            {"stem":"The ______ is very delicious.","stemZh":"这个______非常好吃。","options":["{word}","干扰词1","干扰词2"],"answer":"{word}","explanation":"解析","knowledgePoints":"考察单词: {word}({meaning})","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"例句","exampleZh":"例句翻译"}""";

    private static final String FILL_JUNIOR_SYSTEM = """
            你是一位初中英语教师，为初中生出形态选择填空题。
            规则：
            1. 造一个英文句子，挖掉目标单词变成 ______
            2. 句子的语法要求使用某种特定形态（如复数、过去式、进行时等）
            3. 提供 4 个选项：目标词的不同形态（如 apple/apples/an apple/the apple），只有1个语法正确
            4. explanation 要详细解释为什么选这个形态（语法规则）
            5. 严格按 JSON 格式输出""";

    private static final String FILL_JUNIOR_USER = """
            为以下单词出一道形态选择填空题（初中难度）：
            单词：{word}
            释义：{meaning}
            参考例句：{example}

            要求：造一个需要特定词形的句子，选项是同一个词的不同形态。
            JSON 格式：
            {"stem":"I like eating ______ every day.","options":["{word}","形态变体1","形态变体2","形态变体3"],"answer":"正确形态","explanation":"语法解析：为什么用这个形态","knowledgePoints":"语法点：单复数/时态/冠词用法","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"例句","exampleZh":"例句翻译"}""";

    private static final String FILL_SENIOR_SYSTEM = """
            你是一位高中英语教师，为高中生出开放填空题。
            规则：
            1. 造一个有语境的英文句子，挖掉目标单词变成 ______
            2. 在括号中给出动词原形或中文提示
            3. 学生需要输入正确的词形（可能需要变形：时态/语态/比较级等）
            4. options 设为 null（开放输入）
            5. explanation 详细解释语法变形规则
            6. 严格按 JSON 格式输出""";

    private static final String FILL_SENIOR_USER = """
            为以下单词出一道开放填空题（高中难度）：
            单词：{word}
            释义：{meaning}
            参考例句：{example}

            要求：造一个需要词形变化的句子，给出原形提示，学生自己填写正确形态。
            JSON 格式：
            {"stem":"The children ______ (play) in the garden when it started to rain.","options":null,"answer":"were playing","explanation":"语法解析：过去进行时...","knowledgePoints":"时态：过去进行时 was/were + doing","words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"例句","exampleZh":"例句翻译"}""";

    // ========================================================================
    // 翻译题 (translate) 中→英 - 3 个年级
    // ========================================================================

    private static final String TRANSLATE_PRIMARY_SYSTEM = """
            你是一位小学英语教师，为小学生出句子排序题。
            规则：
            1. 给出一个简单的中文句子和正确的英文翻译
            2. 将英文翻译拆成单词列表（含标点），打乱顺序
            3. 每个英文单词附上中文释义
            4. 句子简短（3~6个词），适合小学生
            5. 严格按 JSON 格式输出""";

    private static final String TRANSLATE_PRIMARY_USER = """
            为以下单词出一道句子排序题（小学难度）：
            单词：{word}
            释义：{meaning}

            要求：构造一个包含该词的简单中文句子，给出英文翻译，拆成打乱的单词。
            JSON 格式：
            {"stem":"我喜欢苹果。","answer":"I like apples.","explanation":"I=我, like=喜欢, apples=苹果","words":[{"word":"I","phonetic":"","meaning":"我"},{"word":"like","phonetic":"","meaning":"喜欢"},{"word":"apples","phonetic":"","meaning":"苹果"}],"exampleSentence":"I like apples.","exampleZh":"我喜欢苹果。","extraData":{"wordOrder":["I","like","apples","."],"shuffledWords":["apples",".","I","like"],"wordMeanings":{"I":"我","like":"喜欢","apples":"苹果",".":"句号"}}}""";

    private static final String TRANSLATE_JUNIOR_SYSTEM = """
            你是一位初中英语教师，为初中生出半填空翻译题。
            规则：
            1. 给出一个中文句子
            2. 给出英文翻译的框架，关键词挖空用 ______ 代替
            3. 挖空 1~2 个关键词（含目标单词）
            4. answer 填完整的英文句子
            5. explanation 解析每个空应填什么、为什么
            6. extraData 中 blanks 列出每个空的答案
            7. 严格按 JSON 格式输出""";

    private static final String TRANSLATE_JUNIOR_USER = """
            为以下单词出一道半填空翻译题（初中难度）：
            单词：{word}
            释义：{meaning}

            要求：构造包含该词的中文句子，英文翻译挖空关键词。
            JSON 格式：
            {"stem":"他每天吃苹果。","answer":"He eats apples every day.","explanation":"eats: eat的第三人称单数; every day: 每天","options":null,"words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"He eats apples every day.","exampleZh":"他每天吃苹果。","extraData":{"template":"He ______ apples every ______.","blanks":["eats","day"]}}""";

    private static final String TRANSLATE_SENIOR_SYSTEM = """
            你是一位高中英语教师，为高中生出完整中译英翻译题。
            规则：
            1. 给出一个有一定复杂度的中文句子（含目标单词的含义）
            2. answer 填标准英文翻译
            3. explanation 解析重点词汇和语法点
            4. 句子涉及从句、时态或固定搭配等语法
            5. 严格按 JSON 格式输出""";

    private static final String TRANSLATE_SENIOR_USER = """
            为以下单词出一道中译英翻译题（高中难度）：
            单词：{word}
            释义：{meaning}

            要求：构造有语法考点的中文句子，提供标准英文翻译。
            JSON 格式：
            {"stem":"如果明天不下雨，我们就去公园。","answer":"If it doesn't rain tomorrow, we will go to the park.","explanation":"条件状语从句：if引导，主将从现","knowledgePoints":"语法：条件状语从句，主将从现","options":null,"words":[{"word":"{word}","phonetic":"{phonetic}","meaning":"{meaning}"}],"exampleSentence":"例句","exampleZh":"例句翻译"}""";

    // ========================================================================
    // 翻译评判 Prompt（初中/高中共用）
    // ========================================================================

    private static final String JUDGE_TRANSLATE_SYSTEM = """
            你是一位专业英语教师，负责评判学生的中译英翻译。
            规则：
            1. 对比学生答案和参考答案，判断语义是否正确
            2. 不要求逐字匹配，只要语义正确、语法基本无误即可判对
            3. 如果有小错误但不影响理解，给 correct:true 但在 feedback 中指出
            4. 详细列出错误点和改进建议
            5. 严格按 JSON 格式输出""";

    private static final String JUDGE_TRANSLATE_USER = """
            请评判以下翻译：

            原中文句子：{stem}
            参考答案：{referenceAnswer}
            学生答案：{userAnswer}
            学生年级：{grade}

            请按以下 JSON 格式输出：
            {"correct":true,"score":85,"feedback":"总体评价...","corrections":["具体问题1","建议2"]}""";

    // ========================================================================
    // 知识点解析 Prompt（保留原有）
    // ========================================================================

    private static final String KP_ANALYZE_SYSTEM = """
            你是一位专业英语教师，擅长分析英文文本中的关键词汇。
            规则：
            1. 提取文本中{grade}学生需要掌握的重要单词和短语
            2. 只提取实义词（名词、动词、形容词、副词），忽略 a/the/is 等功能词
            3. 为每个词提供准确的中文释义
            4. difficulty 值 1-5：1=最简单 5=最难
            5. 严格按指定 JSON 格式输出，不要输出任何其他内容""";

    private static final String KP_ANALYZE_USER = """
            请分析以下英文文本，提取关键词汇：

            {text}

            请严格按以下 JSON 格式输出（不要输出其他内容）：
            {"knowledgePoints":[{"content":"英文单词","meaningZh":"中文释义","type":"word","difficulty":1}]}""";

    // ========================================================================
    // 批量出题 Prompt（年级感知版）
    // ========================================================================

    private static final String BATCH_SYSTEM = """
            你是一位专业英语教师，为{grade}学生批量出题。
            规则：
            1. 每道题严格遵循指定的题型和年级要求
            2. 选择题干扰项必须是真实单词/释义，不能编造
            3. 填空题句子自然通顺
            4. 题目之间不要重复
            5. 每道题都要包含 words、exampleSentence、exampleZh 字段
            6. 严格按 JSON 数组格式输出""";

    private static final String BATCH_USER = """
            请为以下单词列表出题，每个单词出一道指定类型的题：

            {wordList}

            请严格按以下 JSON 数组格式输出（不要输出其他内容）：
            [
              {"kpContent":"原始单词","type":"题型","grade":"年级","stem":"题干","options":["A","B","C","D"],"answer":"正确答案","explanation":"解析","knowledgePoints":"知识点","words":[{"word":"单词","phonetic":"音标","meaning":"释义"}],"exampleSentence":"例句","exampleZh":"例句翻译","extraData":null}
            ]

            注意：
            - 选择题必须有 options 数组
            - 小学填空题有 3 个选项，初中有 4 个形态选项
            - 高中填空题和翻译题 options 设为 null
            - 小学翻译题额外需要 extraData 含 wordOrder/shuffledWords/wordMeanings
            - 初中翻译题需要 extraData 含 template/blanks""";

    // ========================================================================
    // 公共方法
    // ========================================================================

    /**
     * 根据题型+年级获取对应的 Prompt
     */
    public static PromptPair forType(String type, String grade, String word,
                                     String phonetic, String meaning, String example) {
        String g = grade != null ? grade : "junior";
        Map<String, String> vars = Map.of(
                "grade", gradeLabel(g), "word", word,
                "phonetic", phonetic != null ? phonetic : "",
                "meaning", meaning != null ? meaning : "",
                "example", example != null ? example : "无");

        return switch (type) {
            case "en2zh_choice" -> switch (g) {
                case "primary" -> pair(EN2ZH_CHOICE_PRIMARY_SYSTEM, EN2ZH_CHOICE_PRIMARY_USER, vars);
                case "senior" -> pair(EN2ZH_CHOICE_SENIOR_SYSTEM, EN2ZH_CHOICE_SENIOR_USER, vars);
                default -> pair(EN2ZH_CHOICE_JUNIOR_SYSTEM, EN2ZH_CHOICE_JUNIOR_USER, vars);
            };
            case "zh2en_choice" -> switch (g) {
                case "primary" -> pair(ZH2EN_PRIMARY_SYSTEM, ZH2EN_PRIMARY_USER, vars);
                case "senior" -> pair(ZH2EN_SENIOR_SYSTEM, ZH2EN_SENIOR_USER, vars);
                default -> pair(ZH2EN_JUNIOR_SYSTEM, ZH2EN_JUNIOR_USER, vars);
            };
            case "fill_blank" -> switch (g) {
                case "primary" -> pair(FILL_PRIMARY_SYSTEM, FILL_PRIMARY_USER, vars);
                case "senior" -> pair(FILL_SENIOR_SYSTEM, FILL_SENIOR_USER, vars);
                default -> pair(FILL_JUNIOR_SYSTEM, FILL_JUNIOR_USER, vars);
            };
            case "translate" -> switch (g) {
                case "primary" -> pair(TRANSLATE_PRIMARY_SYSTEM, TRANSLATE_PRIMARY_USER, vars);
                case "senior" -> pair(TRANSLATE_SENIOR_SYSTEM, TRANSLATE_SENIOR_USER, vars);
                default -> pair(TRANSLATE_JUNIOR_SYSTEM, TRANSLATE_JUNIOR_USER, vars);
            };
            default -> throw new IllegalArgumentException("不支持的题型: " + type);
        };
    }

    /**
     * 翻译评判 Prompt
     */
    public static PromptPair judgeTranslate(String stem, String referenceAnswer,
                                             String userAnswer, String grade) {
        Map<String, String> vars = Map.of(
                "stem", stem, "referenceAnswer", referenceAnswer,
                "userAnswer", userAnswer, "grade", gradeLabel(grade));
        return pair(JUDGE_TRANSLATE_SYSTEM, JUDGE_TRANSLATE_USER, vars);
    }

    /**
     * 知识点解析 Prompt
     */
    public static PromptPair kpAnalyze(String grade, String text) {
        Map<String, String> vars = Map.of("grade", gradeLabel(grade), "text", text);
        return pair(KP_ANALYZE_SYSTEM, KP_ANALYZE_USER, vars);
    }

    /**
     * 批量出题 Prompt
     */
    public static PromptPair batch(String grade, List<WordItem> words) {
        String wordList = words.stream()
                .map(w -> String.format("- 单词: %s | 释义: %s | 音标: %s | 题型: %s | 年级: %s",
                        w.word(), w.meaning(), w.phonetic() != null ? w.phonetic() : "无",
                        w.type(), gradeLabel(grade)))
                .collect(Collectors.joining("\n"));
        Map<String, String> vars = Map.of("grade", gradeLabel(grade), "wordList", wordList);
        return pair(BATCH_SYSTEM, BATCH_USER, vars);
    }

    // ========================================================================
    // 知识库生成 Prompt（Phase 3a）
    // ========================================================================

    private static final String UNIT_GENERATE_SYSTEM = """
            你是一位资深英语教育专家，精通中国英语课程标准。
            你需要为指定级别和主题生成一个学习单元的知识点列表。
            规则：
            1. 知识点类型包括：word（单词）、phrase（短语）、sentence（日常用语）
            2. 每个知识点必须有：type、content（英文）、meaningZh（中文释义）、exampleSentence（例句）、exampleZh（例句翻译）、difficulty（1~5）
            3. word 类型还需要 phonetic（音标）
            4. 严格按 JSON 数组格式输出，不要输出任何其他内容

            **关键：不同级别的词汇和句型必须有明显难度区分！**

            小学(L1~L4) difficulty=1~2：
            - 只用最基础词汇：hello, apple, dog, red, one, big 等
            - 句子3~5个词，只用 I/You/This is 等基本句型
            - 例：I like cats. / This is a book.

            初中(L5~L7) difficulty=2~3：
            - 必须用初中核心词汇，禁止用 hello/name/friend/teacher 等小学词
            - 要求有语法变化：时态、从句、情态动词
            - word 示例：experience, achieve, suggestion, environment, communicate
            - phrase 示例：look forward to, be good at, make a decision
            - 句子8~15个词，含复合句
            - 例：She suggested that we should try a different approach.

            高中(L8~L9) difficulty=3~5：
            - 必须用高考核心/学术词汇，禁止用初中常见词
            - word 示例：sophisticated, simultaneously, unprecedented, comprehensive
            - 要求长难句、从句嵌套、虚拟语气等高级语法
            - 例：Had it not been for his perseverance, the project would have failed.""";

    private static final String UNIT_GENERATE_USER = """
            请为以下级别和主题生成知识点列表：

            级别：{levelName}（{levelCode}）
            级别描述：{levelDesc}
            主题：{topic}
            单元名称：{unitName}

            要求生成 {count} 个知识点，类型分配：{typeHint}

            **重要：词汇难度必须匹配该级别，不要生成低于该级别的简单词！**

            JSON 格式：
            [
              {"type":"word","content":"apple","phonetic":"/ˈæpl/","meaningZh":"苹果","exampleSentence":"I like apples.","exampleZh":"我喜欢苹果。","difficulty":1},
              {"type":"phrase","content":"good morning","phonetic":null,"meaningZh":"早上好","exampleSentence":"Good morning, teacher!","exampleZh":"早上好，老师！","difficulty":1},
              {"type":"sentence","content":"How are you?","phonetic":null,"meaningZh":"你好吗？","exampleSentence":"Hi! How are you? I'm fine, thank you.","exampleZh":"嗨！你好吗？我很好，谢谢你。","difficulty":1}
            ]""";

    /**
     * 知识库单元内容生成 Prompt
     */
    public static PromptPair unitGenerate(String levelCode, String levelName, String levelDesc,
                                           String topic, String unitName, int count, String typeHint) {
        Map<String, String> vars = Map.of(
                "levelCode", levelCode, "levelName", levelName,
                "levelDesc", levelDesc != null ? levelDesc : "",
                "topic", topic, "unitName", unitName,
                "count", String.valueOf(count), "typeHint", typeHint);
        return pair(UNIT_GENERATE_SYSTEM, UNIT_GENERATE_USER, vars);
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    private static String gradeLabel(String grade) {
        return switch (grade != null ? grade : "junior") {
            case "primary" -> "小学";
            case "senior" -> "高中";
            default -> "初中";
        };
    }

    private static PromptPair pair(String system, String user, Map<String, String> vars) {
        return new PromptPair(render(system, vars), render(user, vars));
    }

    private static String render(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public record PromptPair(String system, String user) {}

    public record WordItem(String word, String meaning, String phonetic, String type) {}

    // ========================================================================
    // Phase 4: 用户内容引擎 — 提取 + 分类 + 策略
    // ========================================================================

    private static final String CONTENT_EXTRACT_SYSTEM = """
            你是一位资深英语教育专家，擅长从各类英文材料中提取和分类学习内容。
            你需要根据用户上传的文本，自动提取并分类为以下类别：

            1. **vocabulary** — 单词/词组：需要记忆的词汇
            2. **grammar** — 语法点：文本中体现的语法规则
            3. **sentence_pattern** — 句型：值得学习的典型句式
            4. **passage** — 阅读段落：适合做理解训练的完整段落

            规则：
            - 目标年级为{grade}
            - {sourceTypeRule}
            - vocabulary 类必须有 content, meaningZh, phonetic, exampleSentence, exampleZh
            - grammar 类的 content 是语法规则名称，meaningZh 是中文解释，extraData 包含详细规则
            - sentence_pattern 类的 content 是英文句型，meaningZh 是中文说明
            - passage 类的 content 是英文段落，meaningZh 是中文翻译
            - 每项都有 difficulty(1-5) 和 aiNote（为什么提取此项的简短说明）
            - 用户备注会影响提取侧重：如果用户说"重点背单词"，则多提取词汇；如果说"语法薄弱"，则多提取语法点
            - 严格按 JSON 格式输出""";

    private static final String CONTENT_EXTRACT_USER = """
            请分析以下{grade}学生上传的英文材料，提取学习内容：

            === 原文 ===
            {text}

            === 用户备注 ===
            {userNote}

            请严格按以下 JSON 格式输出（不要输出其他内容）：
            {"items":[{"category":"vocabulary","content":"英文内容","meaningZh":"中文释义","phonetic":"/音标/","exampleSentence":"例句","exampleZh":"例句翻译","extraData":null,"difficulty":2,"aiNote":"提取原因"}],"summary":"对这份材料的整体评价（一两句话）"}""";

    private static final String STUDY_STRATEGY_SYSTEM = """
            你是一位英语学习策略专家，根据学生上传材料的分析结果和学习诉求，制定个性化出题策略。

            策略要素：
            1. focus — 核心聚焦方向（vocabulary/grammar/sentence_pattern/mixed）
            2. weights — 各分类的出题权重（加总=1.0）
            3. questionTypePreference — 推荐的题型权重（en2zh_choice, zh2en_choice, fill_blank, translate 加总=1.0）
            4. totalRecommended — 建议总练习题数
            5. dailyTarget — 建议每日练习数
            6. reasoning — 策略制定理由（中文一两句话）

            规则：
            - 用户备注是第一优先级，用户说"背单词"就加大词汇权重
            - 如果没有某个分类的内容，该分类权重设为 0
            - 小学生侧重选择题，高中生侧重填空和翻译
            - 严格按 JSON 格式输出""";

    private static final String STUDY_STRATEGY_USER = """
            学生年级：{grade}
            用户备注：{userNote}

            提取结果统计：
            - 词汇(vocabulary): {vocabCount} 个
            - 语法(grammar): {grammarCount} 个
            - 句型(sentence_pattern): {sentenceCount} 个
            - 阅读(passage): {passageCount} 个
            - 总计: {totalCount} 个

            材料摘要：{summary}

            请为该学生制定出题策略，严格按以下 JSON 格式输出（不要输出其他内容）：
            {"focus":"vocabulary","weights":{"vocabulary":0.6,"grammar":0.2,"sentence_pattern":0.15,"passage":0.05},"questionTypePreference":{"en2zh_choice":0.3,"zh2en_choice":0.3,"fill_blank":0.2,"translate":0.2},"totalRecommended":30,"dailyTarget":10,"reasoning":"策略理由"}""";

    /**
     * 用户上传内容提取 + 分类 Prompt
     */
    public static PromptPair contentExtract(String grade, String text, String sourceType, String userNote) {
        String sourceTypeRule = "wordlist".equals(sourceType)
                ? "这是用户手动输入的词表，每一行是一个单词/词组。\n"
                  + "❗❗❗ 最高优先级规则：必须为词表中的每一个词都生成一条 vocabulary 类型的知识点。\n"
                  + "绝对不允许跳过任何一个词，即使它看起来很简单。items 数量必须 >= 词表行数。\n"
                  + "exampleSentence 可以简短（不超过8个词），aiNote 设为null以节省空间"
                : "从文章中提取该年级需要掌握的重要内容，数量不限，尽量全面";
        Map<String, String> vars = Map.of(
                "grade", gradeLabel(grade),
                "text", text,
                "sourceTypeRule", sourceTypeRule,
                "userNote", userNote != null ? userNote : "无特殊要求");
        return pair(CONTENT_EXTRACT_SYSTEM, CONTENT_EXTRACT_USER, vars);
    }

    /**
     * 出题策略生成 Prompt
     */
    public static PromptPair studyStrategy(String grade, String userNote,
                                            int vocabCount, int grammarCount,
                                            int sentenceCount, int passageCount,
                                            String summary) {
        int totalCount = vocabCount + grammarCount + sentenceCount + passageCount;
        Map<String, String> vars = Map.of(
                "grade", gradeLabel(grade),
                "userNote", userNote != null ? userNote : "无特殊要求",
                "vocabCount", String.valueOf(vocabCount),
                "grammarCount", String.valueOf(grammarCount),
                "sentenceCount", String.valueOf(sentenceCount),
                "passageCount", String.valueOf(passageCount),
                "totalCount", String.valueOf(totalCount),
                "summary", summary != null ? summary : "");
        return pair(STUDY_STRATEGY_SYSTEM, STUDY_STRATEGY_USER, vars);
    }
}
