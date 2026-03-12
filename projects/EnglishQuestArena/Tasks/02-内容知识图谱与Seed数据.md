# 阶段二：内容知识图谱 & Seed 数据

## 状态：⬜ 未开始 | 优先级：🔴 最高

## 目标
从权威来源整理 CEFR 各等级所需的词汇、语法、场景，编写 seed JSON 数据文件。
Seed 数据是整个系统的"燃料"——没有高质量的内容，前端和后端都是空壳。

---

## 为什么这是第一优先级

1. 后端 API 只是 CRUD + 算法，代码量相对固定，数据填充才是大头
2. 前端已经能渲染所有题型，但目前只有 63 个单词 (3 章硬编码)
3. Seed 数据的结构直接决定后端表设计，先定数据才能避免返工
4. Auto-Drill 自动补题依赖 content_items 的数量和质量

---

## 权威参考来源（全部免费）

| 来源 | 内容 | 用途 |
|------|------|------|
| English Profile (englishprofile.org) | CEFR 词汇表 (EVP) | A1/A2 词汇清单 |
| Cambridge A1 Movers / A2 Flyers 大纲 | 官方考试大纲 | 知识点清单的"标准答案" |
| CEFR Global Scale (Council of Europe) | 官方能力描述 | 验证覆盖度 |
| British Council LearnEnglish | 分级学习材料 | 参考课程设计 |
| Tatoeba (tatoeba.org, CC协议) | 多语言例句库 | 例句素材 |
| Oxford 3000 / 5000 | 按 CEFR 标注核心词 | 交叉验证 |

---

## 各等级内容规划

### PRE_A1（3-4 章，MVP 最小范围）
- **词汇**: 100-150 词
  - 字母 A-Z
  - 数字 1-10
  - 颜色 (red, blue, green, yellow, black, white, pink, purple...)
  - 家人 (mom, dad, brother, sister, family, friend...)
  - 动物 (cat, dog, fish, bird, rabbit, duck, bear, elephant, monkey...)
  - 食物 (apple, bread, water, milk...)
  - 身体 (head, hand, eye, mouth...)
  - 日常物品 (book, pen, phone, bag...)
  - 形容词 (big, small, long, short, happy, sad, hungry, thirsty, tired...)
- **语法**: I am, I have, I like, This is, a/an, 单复数
- **场景**: 问候、自我介绍、简单指物
- **预计**: 3-4 章 × 5 天/章 × 30 题/天 = 450-600 道题 (人写 ~150 + Auto-Drill ~400)
- **前端现状**: CH1(19词) + CH2(22词) + CH3(22词) = 63 词已有，还需 ~90 词

### A1（5-6 章）
- **词汇**: 500-700 词
  - 职业 (teacher, doctor, student...)
  - 地点 (school, hospital, park, shop...)
  - 时间 (today, yesterday, morning, evening...)
  - 天气 (sunny, rainy, cold, hot...)
  - 交通 (bus, car, walk, drive...)
  - 购物 (buy, price, cheap, expensive...)
  - 感受 (happy, sad, tired, hungry...)
- **语法**: 一般现在时、一般过去时、There is/are、can/can't、疑问句
- **场景**: 自我介绍、购物、问路、点餐、看病、打电话
- **预计**: ~800 道题

### A2（5-6 章，远期）
- **词汇**: 1000-1500 词
- **语法**: 现在进行时、现在完成时、比较级/最高级、连词、if 条件句
- **场景**: 旅行、投诉、预约、描述经历、表达意见
- **预计**: ~1000 道题

### B1（远期规划）
- **词汇**: 2000+ 词
- **语法**: 虚拟语态、被动语态、间接引语、定语从句
- **预计**: ~1200 道题

---

## Seed 文件结构

```
backend/src/main/resources/seed/
├── chapters.json              # 章节定义 (code, cefrLevel, titleEn/Zh, days, campUnlockRate)
├── lessons.json               # 每日课程 (code, chapterCode, dayIndex, targetTaskCount)
├── content_items/
│   ├── pre_a1_ch1.json        # 每章独立文件，便于维护
│   ├── pre_a1_ch2.json
│   ├── pre_a1_ch3.json
│   └── ...
├── tasks/
│   ├── pre_a1_ch1_d01.json    # 每天独立文件
│   ├── pre_a1_ch1_d02.json
│   └── ...
├── boss_configs.json          # Boss 配置
├── boss_pools/
│   ├── pre_a1_ch1.json        # 每章 Boss 题池
│   └── ...
├── skill_nodes.json           # 技能树节点
└── cefr_exam_pools/
    ├── pre_a1.json            # 每等级测验题池
    └── a1.json
```

### 单个 ContentItem 数据规范
```json
{
  "code": "W_HELLO",
  "type": "WORD",
  "cefrLevel": "PRE_A1",
  "chapterCode": "PRE_A1_CH1",
  "dayIndex": 1,
  "textEn": "hello",
  "textZh": "你好",
  "ipa": "həˈloʊ",
  "exampleEn": "Hello! How are you?",
  "exampleZh": "你好！你好吗？",
  "distractors": ["W_GOODBYE", "W_SORRY"],
  "tags": ["greeting"],
  "source": {
    "name": "Tatoeba",
    "url": "https://tatoeba.org/",
    "note": "CC BY 2.0"
  }
}
```

### 单个 Task 数据规范
```json
{
  "code": "PRE_A1_CH1_D01_T01",
  "lessonCode": "PRE_A1_CH1_D01",
  "orderIndex": 1,
  "type": "MCQ",
  "promptEn": "Choose the correct word: \"你好\"",
  "promptZhHint": "提示：选择对应的英文单词",
  "options": [
    { "key": "A", "textEn": "hello", "textZh": "你好" },
    { "key": "B", "textEn": "bye", "textZh": "再见" },
    { "key": "C", "textEn": "pen", "textZh": "笔" }
  ],
  "answer": { "correctOptionKey": "A" },
  "explanationEn": "\"hello\" is a greeting.",
  "explanationZh": "hello 用于打招呼。",
  "xpReward": 5,
  "goldReward": 0,
  "tts": { "enabled": false, "ttsTextEn": "" },
  "links": { "contentItemCodes": ["W_HELLO"] }
}
```

---

## 执行步骤 (MVP: PRE_A1 三章)

### 步骤 1：词汇整理
- [ ] 从 English Profile 下载 PRE_A1 词汇表
- [ ] 与 Oxford 3000 A1 部分交叉验证
- [ ] 整理成 content_items JSON (每词带 textEn/textZh/ipa/exampleEn/exampleZh/source)
- [ ] 为每个词填写 2 个 distractors (干扰项 code)
- [ ] 按章节分配词汇 (每章 30-40 词), 按天分配 (每天 6-8 新词)
- [ ] 验证: 现有 3 章 63 词 → 补充至 100-150 词

### 步骤 2：手写核心题目
- [ ] 每个知识点写 3-5 道核心题 (保证质量)
- [ ] 题型覆盖: MCQ + MCQ_REVERSE + FLASHCARD + SPELLING + WORD_ORDER + FILL_BLANK
- [ ] 听力题: LISTEN_PICK + LISTEN_FILL (需确认 TTS 文本)
- [ ] 情景题: DIALOGUE_REVIEW + SITUATION_PICK (需编写对话脚本)
- [ ] 纠错题: ERROR_FIX (需设计常见错误)
- [ ] 每天 seed 题约 10-15 道, 剩余由 Auto-Drill 补足至 30 道

### 步骤 3：Boss 题池编写
- [ ] 每章 Boss 题池: LISTEN_WORD 4 + MEANING 4 + PATTERN_ORDER 4 + SPELLING 2 + DIALOGUE 1
- [ ] Boss 题覆盖该章 70% 核心词汇
- [ ] 每次挑战随机抽题, 题目不重复

### 步骤 4：CEFR 等级测验题池
- [ ] PRE_A1 测验题池: 30-50 道精选题 (必须人工编写)
- [ ] 题型分布: MCQ 30% + FILL_BLANK 20% + SPELLING 15% + LISTEN_PICK 15% + WORD_ORDER 10% + SITUATION_PICK 10%
- [ ] 通过率目标: 60-80%

### 步骤 5：技能树节点
- [ ] 定义技能树分支和节点 (每分支 5-8 节点)
- [ ] 设置前置依赖关系
- [ ] 平衡技能点获取速度

### 步骤 6：数据验证
- [ ] 脚本检查所有 JSON 格式合法性
- [ ] 脚本检查 code 唯一性 (无重复)
- [ ] 脚本检查 distractors 引用有效
- [ ] 脚本检查 links.contentItemCodes 引用有效
- [ ] 人工走查代表性题目质量

---

## 工作量估算

| 内容 | 数量 | 工作 |
|------|------|------|
| ContentItems (PRE_A1) | ~150 个 | 词汇整理, 找例句, 填 IPA |
| 手写 Tasks (PRE_A1) | ~150 道 | 每题需构思题目+选项+解释 |
| Auto-Drill 生成 | ~400 道 | 后端自动, 无需人工 |
| Boss 题池 | ~45 道 (3章×15) | 挑选核心词+编写 |
| CEFR 测验 | ~40 道 | 精选, 质量最高 |
| **总计人工编写** | **~385 项** | — |
