# Seed JSON Schema（MVP）— English RPG Learning

目标：后端启动读取 resources/seed/*.json 并幂等导入数据库。
要求：所有 seed 记录必须有稳定唯一键（code），避免重复导入。

## 1) chapters.json
```json
[
  {
    "code": "PRE_A1_CH1",
    "cefrLevel": "PRE_A1",
    "titleEn": "Chapter 1: First Steps",
    "titleZh": "第1章：从零开始",
    "orderIndex": 1,
    "days": 11,
    "bossDayIndex": 11
  }
]
```

## 2) lessons.json
每一天一个 lesson。`dayIndex` 从 1 开始。
```json
[
  {
    "code": "PRE_A1_CH1_D01",
    "chapterCode": "PRE_A1_CH1",
    "dayIndex": 1,
    "titleEn": "Day 1 — Greetings",
    "titleZh": "第1天—问候与 I am",
    "estimatedMinutes": 30,
    "unlockRule": { "type": "SEQUENTIAL" },
    "targetTaskCount": 30,
    "autoDrillEnabled": true
  }
]
```

## 3) content_items.json（可复习内容：单词/句型/句子）
> 这是“权威性与SRS”的关键。所有内容必须带 source。
```json
[
  {
    "code": "W_HELLO",
    "type": "WORD",
    "cefrLevel": "PRE_A1",
    "textEn": "hello",
    "textZh": "你好",
    "ipa": "həˈloʊ",
    "exampleEn": "Hello!",
    "exampleZh": "你好！",
    "tags": ["greeting"],
    "source": {
      "name": "Tatoeba",
      "url": "https://tatoeba.org/",
      "note": "Example sentence reference"
    }
  }
]
```

句型也作为 content item：
```json
{
  "code": "P_I_AM_NAME",
  "type": "PATTERN",
  "cefrLevel": "PRE_A1",
  "textEn": "I am {playerName}.",
  "textZh": "我是{playerName}。",
  "tags": ["pattern", "be-verb"],
  "source": { "name": "In-house", "url": "", "note": "Course pattern" }
}
```

## 4) tasks.json（主线任务）
每个 task 必须归属 lesson，并且可选关联一个 contentItem（用于SRS）。
```json
[
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
    "explanationEn": "“hello” is a greeting.",
    "explanationZh": "hello 用于打招呼。",
    "xpReward": 5,
    "goldReward": 0,
    "tts": { "enabled": false, "ttsTextEn": "" },
    "links": { "contentItemCodes": ["W_HELLO"] }
  }
]
```

FLASHCARD：
```json
{
  "code": "PRE_A1_CH1_D01_C01",
  "lessonCode": "PRE_A1_CH1_D01",
  "orderIndex": 3,
  "type": "FLASHCARD",
  "promptEn": "hello",
  "promptZhHint": "你好",
  "answer": { "action": "GOT_IT" },
  "explanationEn": "A common greeting.",
  "explanationZh": "常见问候语。",
  "xpReward": 2,
  "goldReward": 0,
  "tts": { "enabled": true, "ttsTextEn": "hello" },
  "links": { "contentItemCodes": ["W_HELLO"] }
}
```

SPELLING（容错规则写死：trim + lowerCase）：
```json
{
  "code": "PRE_A1_CH1_D01_S01",
  "lessonCode": "PRE_A1_CH1_D01",
  "orderIndex": 12,
  "type": "SPELLING",
  "promptEn": "Type the word you hear.",
  "promptZhHint": "提示：听音拼写（忽略大小写）",
  "answer": { "acceptedTexts": ["hello"] },
  "xpReward": 8,
  "goldReward": 0,
  "tts": { "enabled": true, "ttsTextEn": "hello" },
  "links": { "contentItemCodes": ["W_HELLO"] }
}
```

WORD_ORDER：
```json
{
  "code": "PRE_A1_CH1_D01_O01",
  "lessonCode": "PRE_A1_CH1_D01",
  "orderIndex": 20,
  "type": "WORD_ORDER",
  "promptEn": "Arrange the words to make a sentence.",
  "promptZhHint": "提示：把单词排成正确句子",
  "answer": { "correctTokens": ["I", "am", "{playerName}", "."] },
  "tokens": ["{playerName}", "am", "I", "."],
  "xpReward": 8,
  "tts": { "enabled": true, "ttsTextEn": "I am {playerName}." },
  "links": { "contentItemCodes": ["P_I_AM_NAME"] }
}
```

DIALOGUE_REVIEW：
```json
{
  "code": "PRE_A1_CH1_D01_DLG01",
  "lessonCode": "PRE_A1_CH1_D01",
  "orderIndex": 28,
  "type": "DIALOGUE_REVIEW",
  "dialogue": {
    "lines": [
      { "en": "Hi!", "zh": "嗨！", "ttsTextEn": "Hi!" },
      { "en": "Hello!", "zh": "你好！", "ttsTextEn": "Hello!" }
    ]
  },
  "followUpQuestions": [
    {
      "type": "MCQ",
      "promptEn": "Which word means \"你好\"?",
      "promptZhHint": "提示：选择“你好”的英文",
      "options": [
        { "key": "A", "textEn": "hello", "textZh": "你好" },
        { "key": "B", "textEn": "bye", "textZh": "再见" }
      ],
      "answer": { "correctOptionKey": "A" }
    }
  ],
  "xpReward": 10,
  "tts": { "enabled": true, "ttsTextEn": "" },
  "links": { "contentItemCodes": ["W_HELLO", "W_HI"] }
}
```

## 5) boss_pools.json（Boss 随机题库）
```json
[
  {
    "code": "PRE_A1_CH1_BOSS",
    "chapterCode": "PRE_A1_CH1",
    "bossDayIndex": 11,
    "rules": {
      "bossHp": 10,
      "playerHp": 5,
      "comboThreshold": 5,
      "comboBonusDamage": 1,
      "dailyRetryLimit": 1,
      "coverage": "CORE_70_PERCENT",
      "drawPlan": [
        { "pool": "LISTEN_WORD", "count": 4 },
        { "pool": "MEANING", "count": 4 },
        { "pool": "PATTERN_ORDER", "count": 4 },
        { "pool": "SPELLING", "count": 2 },
        { "pool": "DIALOGUE", "count": 1 }
      ]
    }
  }
]
```

题库题目可与 tasks 同结构，建议单独文件：
- boss_items_listen_word.json
- boss_items_meaning.json
- boss_items_pattern_order.json
- boss_items_spelling.json
- boss_items_dialogue.json

## 6) skill_nodes.json
```json
[
  {
    "code": "SK_GRAMMAR_I_AM",
    "branch": "GRAMMAR",
    "nameEn": "I am Pattern",
    "nameZh": "I am 句型",
    "descriptionEn": "Use \"I am ...\" to introduce yourself.",
    "descriptionZh": "用 I am ... 进行自我介绍。",
    "costSkillPoints": 1,
    "prerequisites": [],
    "unlockConditions": [
      { "type": "COMPLETE_LESSON", "lessonCode": "PRE_A1_CH1_D01" }
    ],
    "rewards": [
      { "type": "XP_MULTIPLIER_FOR_TAG", "tag": "pattern", "value": 0.1 }
    ]
  }
]
```

幂等要求
- 所有对象用 code 作为唯一键 upsert
- 记录 seed 版本：seed_meta.json -> backend 存一张 seed_import_log(version, importedAt)