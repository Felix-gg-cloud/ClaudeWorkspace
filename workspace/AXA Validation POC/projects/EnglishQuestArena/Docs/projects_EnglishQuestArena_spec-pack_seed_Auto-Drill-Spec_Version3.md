# Auto-Generated Drills Spec（必须实现）

每个 lesson 可配置：
- targetTaskCount：建议 30
- autoDrillEnabled：true/false（默认 true）

当 GET /api/today 返回 lessonTasks 时：
1) 先取 seed 的 tasks（按 orderIndex 排序）
2) 若 tasks.size < targetTaskCount 且 autoDrillEnabled=true：
   - 从本 lesson 所有 tasks.links.contentItemCodes 收集去重 contentItems
   - 只选 type=WORD 的 contentItems 作为自动出题候选（避免 pattern 太早乱）
   - 生成 auto tasks（code 以 AUTO_ 前缀）补足到 targetTaskCount
3) auto tasks 的类型按比例生成：
   - 50%: MCQ（词义选择：promptEn "Choose the meaning of 'word'"）
   - 30%: MCQ（听音选词：ttsTextEn=word，options 为 3-4 个同章节词）
   - 20%: SPELLING（ttsTextEn=word，acceptedTexts=[word]）
4) 自动题奖励较低：
   - xpReward=2~4，goldReward=0
5) 自动题也写入 task_progress（用 code 作为 taskKey），但不需要落库为 Task 实体（可用“虚拟任务”模型）