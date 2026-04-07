package com.ll.ai.prompt;

/**
 * Phase 5a — AI 教学协议（Teaching Protocol）
 * 
 * 核心规则：AI 老师出题/教学时必须遵守的约束
 * 1. 词汇约束：只使用 vocab_constraint 表中对应级别的词
 * 2. 语法约束：只涉及 grammar_point_ref 表中对应级别的语法点
 * 3. 难度约束：遵循 i+1 原则，当前级别 + 少量高一级内容
 * 4. 风格约束：参考 golden_sample 试卷的出题风格
 */
public final class TeachingProtocol {

    private TeachingProtocol() {}

    // ============================================================
    // 1. 出题约束 — 注入到所有出题相关的 System Prompt
    // ============================================================

    /**
     * 生成词汇约束指令，注入到 system prompt
     * @param levelCode 学生当前级别 L3-L12
     * @param vocabList 该级别的词汇列表（从 DB 查询，逗号分隔）
     * @param nextLevelVocabSample 高一级的少量词汇（i+1 扩展，10-20个）
     */
    public static String vocabConstraint(String levelCode, String vocabList, String nextLevelVocabSample) {
        return String.format("""
                【词汇约束 — 必须严格遵守】
                学生当前级别：%s
                本级别可用词汇范围：
                %s
                
                i+1 扩展词汇（可少量使用，但必须给出中文释义）：
                %s
                
                规则：
                • 题目中的英文单词必须来自上述词汇范围
                • 如果使用了扩展词汇，必须在题目中标注中文含义
                • 绝对不能出现超出范围的生僻词
                • 选项中的干扰项也必须来自可用词汇""", levelCode, vocabList, nextLevelVocabSample);
    }

    /**
     * 生成语法约束指令
     * @param levelCode 学生当前级别
     * @param grammarPoints 该级别已学语法点列表
     */
    public static String grammarConstraint(String levelCode, String grammarPoints) {
        return String.format("""
                【语法约束 — 必须严格遵守】
                学生当前级别：%s
                已学语法点：
                %s
                
                规则：
                • 题目涉及的语法结构限于上述已学语法点
                • 可以用简单句式组合，但不能出现未学语法
                • 如果考察某个语法点，请在反馈中解释该规则""", levelCode, grammarPoints);
    }

    /**
     * 生成样本参考指令（让 AI 参考真题风格出题）
     * @param levelCode 学生当前级别
     * @param sampleExcerpt 黄金样本的题目节选（截取部分，不是全文）
     */
    public static String sampleReference(String levelCode, String sampleExcerpt) {
        return String.format("""
                【出题风格参考】
                以下是 %s 级别的真实考试题目节选，请参考其难度和风格：
                ---
                %s
                ---
                注意：仅参考风格，不要照搬原题。""", levelCode, sampleExcerpt);
    }

    // ============================================================
    // 2. 教学循环协议 — 编排引擎使用的指令模板
    // ============================================================

    /**
     * 复习环节的 System Prompt 补充
     * @param weakKps 学生薄弱知识点列表
     * @param dueCards 到期的 SRS 卡片内容
     */
    public static String reviewPhase(String weakKps, String dueCards) {
        return String.format("""
                【当前环节：复习】
                以下是学生需要复习的内容：
                
                薄弱知识点：
                %s
                
                到期复习卡片：
                %s
                
                请围绕这些内容设计 1-2 道复习题，题型灵活（选择/填空/翻译均可）。
                如果学生全部答对，给予表扬并进入新知识学习。
                如果答错，温柔纠正并用不同方式再出一道同类型题。""", weakKps, dueCards);
    }

    /**
     * 新知识学习环节
     * @param targetKp 本次要教的知识点
     * @param context 相关例句或场景
     */
    public static String learnPhase(String targetKp, String context) {
        return String.format("""
                【当前环节：新知识学习】
                今天要教的知识点：%s
                
                教学策略：
                1. 先通过一个生活场景引入这个知识点
                2. 给出 2-3 个例句，由简到难
                3. 用中文解释关键规则
                4. 出一道简单的练习题检测理解
                
                相关上下文：
                %s""", targetKp, context);
    }

    /**
     * 练习巩固环节
     * @param practicedKps 本节课已学/复习的知识点
     * @param questionType 题目类型
     */
    public static String practicePhase(String practicedKps, String questionType) {
        return String.format("""
                【当前环节：练习巩固】
                本次已学/复习的知识点：
                %s
                
                请出一道「%s」题来巩固以上内容。
                规则：
                • 综合本次涉及的多个知识点
                • 如果学生之前有错题，优先覆盖薄弱点
                • 出题后等学生作答，不要直接给答案""", practicedKps, questionType);
    }

    /**
     * 总结环节
     * @param sessionSummary 本次学习内容摘要
     * @param correctRate 正确率
     */
    public static String summaryPhase(String sessionSummary, String correctRate) {
        return String.format("""
                【当前环节：学习总结】
                本次学习内容：
                %s
                
                正确率：%s
                
                请用温暖的语气总结本次学习：
                1. 肯定学生的努力和进步
                2. 列出今天学到的 2-3 个要点（用简洁的要点形式）
                3. 如果正确率低于 60%%，鼓励学生不要气馁
                4. 预告下次会复习今天薄弱的内容
                5. 用一句轻松的话结束，如"今天辛苦啦，明天见！ 🎉" """, sessionSummary, correctRate);
    }

    // ============================================================
    // 3. 反馈结构化协议 — AI 错误反馈必须遵循的格式
    // ============================================================

    /**
     * 答题反馈的结构化要求（追加到出题 prompt 后）
     */
    public static final String FEEDBACK_PROTOCOL = """
            【答题反馈协议】
            当学生作答后，请按以下格式给出反馈：
            
            如果答对：
            [FEEDBACK]{"correct":true,"kp":"涉及的知识点","praise":"个性化表扬语"}[/FEEDBACK]
            然后用一句鼓励的话
            
            如果答错：
            [FEEDBACK]{"correct":false,"kp":"涉及的知识点","errorType":"词汇混淆/语法错误/拼写错误/理解偏差","correction":"正确答案及简要解释","hint":"帮助记忆的小技巧"}[/FEEDBACK]
            然后用温柔的语气解释，不要让学生有挫败感
            
            重要：[FEEDBACK]...[/FEEDBACK] 标签必须完整，JSON 必须合法。""";

    // ============================================================
    // 4. 组装器 — 将各模块拼接为完整的 System Prompt
    // ============================================================

    /**
     * 组装教学出题的完整 System Prompt
     */
    public static String buildTeachingPrompt(
            String teacherPersona,
            String studentProfile,
            String vocabConstraintBlock,
            String grammarConstraintBlock,
            String sampleReferenceBlock,
            String phaseBlock) {

        StringBuilder sb = new StringBuilder();
        sb.append(teacherPersona).append("\n\n");
        sb.append(studentProfile).append("\n\n");
        sb.append(vocabConstraintBlock).append("\n\n");
        sb.append(grammarConstraintBlock).append("\n\n");
        if (sampleReferenceBlock != null) {
            sb.append(sampleReferenceBlock).append("\n\n");
        }
        sb.append(phaseBlock).append("\n\n");
        sb.append(FEEDBACK_PROTOCOL);
        return sb.toString();
    }
}
