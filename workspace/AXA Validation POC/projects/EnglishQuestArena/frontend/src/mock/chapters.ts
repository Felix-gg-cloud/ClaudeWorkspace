// 多章节数据：ContentItem 驱动的章节配置
// Camp Encounters 从 ContentItem[] 自动生成，确保 Camp ↔ Quest ↔ Boss 词汇覆盖
import type { ChapterConfig, ContentItem, Lesson, BossConfig, Task } from '@/types'
import type { CampMapData, MapEncounter } from '@/game/config/mapData'
import { generateMonsterEncounters, extractMonsterSlots } from '@/data/campGenerator'

// ============================================================
// Chapter 1: 初次接触 — Greetings & I am
// ============================================================

const ch1Words: ContentItem[] = [
  { code: 'W_HELLO', type: 'WORD', en: 'hello', zh: '你好', phonetic: '/həˈloʊ/', sentence: 'Hello, my name is Tom.', sentenceZh: '你好，我叫汤姆。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_GOODBYE', 'W_SORRY'] },
  { code: 'W_HI', type: 'WORD', en: 'hi', zh: '嗨', phonetic: '/haɪ/', sentence: 'Hi! How are you?', sentenceZh: '嗨！你好吗？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_GOODBYE', 'W_HELLO'] },
  { code: 'W_GOODBYE', type: 'WORD', en: 'goodbye', zh: '再见', phonetic: '/ɡʊdˈbaɪ/', sentence: 'Goodbye! See you tomorrow.', sentenceZh: '再见！明天见。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_HELLO', 'W_SORRY'] },
  { code: 'W_THANK_YOU', type: 'WORD', en: 'thank you', zh: '谢谢你', phonetic: '/θæŋk juː/', sentence: 'Thank you very much!', sentenceZh: '非常感谢！', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_SORRY', 'W_PLEASE'] },
  { code: 'W_SORRY', type: 'WORD', en: 'sorry', zh: '对不起', phonetic: '/ˈsɒri/', sentence: "I'm sorry about that.", sentenceZh: '对此我很抱歉。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_THANK_YOU', 'W_PLEASE'] },
  { code: 'W_PLEASE', type: 'WORD', en: 'please', zh: '请', phonetic: '/pliːz/', sentence: 'Please sit down.', sentenceZh: '请坐下。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_SORRY', 'W_THANK_YOU'] },
  { code: 'W_YES', type: 'WORD', en: 'yes', zh: '是', phonetic: '/jes/', sentence: 'Yes, I am.', sentenceZh: '是的，我是。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_NO', 'W_SORRY'] },
  { code: 'W_NO', type: 'WORD', en: 'no', zh: '不', phonetic: '/noʊ/', sentence: 'No, thank you.', sentenceZh: '不，谢谢。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_YES', 'W_PLEASE'] },
  { code: 'W_NAME', type: 'WORD', en: 'name', zh: '名字', phonetic: '/neɪm/', sentence: 'What is your name?', sentenceZh: '你叫什么名字？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_MY', 'W_YOUR'] },
  { code: 'W_MY', type: 'WORD', en: 'my', zh: '我的', phonetic: '/maɪ/', sentence: 'My name is Amy.', sentenceZh: '我的名字是艾米。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_YOUR', 'W_NAME'] },
  { code: 'W_YOUR', type: 'WORD', en: 'your', zh: '你的', phonetic: '/jɔːr/', sentence: 'What is your name?', sentenceZh: '你叫什么名字？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_MY', 'W_NAME'] },
  { code: 'W_I', type: 'WORD', en: 'I', zh: '我', phonetic: '/aɪ/', sentence: 'I am a student.', sentenceZh: '我是一个学生。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_MY', 'W_YOUR'] },
  { code: 'W_AM', type: 'WORD', en: 'am', zh: '是(I am)', phonetic: '/æm/', sentence: 'I am happy.', sentenceZh: '我很开心。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_IS', 'W_NAME'] },
  { code: 'W_IS', type: 'WORD', en: 'is', zh: '是(it is)', phonetic: '/ɪz/', sentence: 'She is my friend.', sentenceZh: '她是我的朋友。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_AM', 'W_MY'] },
  { code: 'W_WHAT', type: 'WORD', en: 'what', zh: '什么', phonetic: '/wɒt/', sentence: 'What is this?', sentenceZh: '这是什么？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_NAME', 'W_YOUR'] },
  // Day 1 扩充: 问候相关
  { code: 'W_GOOD', type: 'WORD', en: 'good', zh: '好的', phonetic: '/ɡʊd/', sentence: 'Good morning!', sentenceZh: '早上好！', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_NICE', 'W_FINE'] },
  { code: 'W_MORNING', type: 'WORD', en: 'morning', zh: '早上', phonetic: '/ˈmɔːrnɪŋ/', sentence: 'Good morning, teacher!', sentenceZh: '早上好，老师！', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_NIGHT', 'W_GOOD'] },
  { code: 'W_NIGHT', type: 'WORD', en: 'night', zh: '晚上', phonetic: '/naɪt/', sentence: 'Good night! Sleep well.', sentenceZh: '晚安！睡个好觉。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_MORNING', 'W_GOOD'] },
  { code: 'W_FINE', type: 'WORD', en: 'fine', zh: '很好', phonetic: '/faɪn/', sentence: 'I am fine, thank you.', sentenceZh: '我很好，谢谢。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_GOOD', 'W_OK'] },
  { code: 'W_OK', type: 'WORD', en: 'OK', zh: '好的', phonetic: '/oʊˈkeɪ/', sentence: 'OK, let us go.', sentenceZh: '好的，我们走吧。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_YES', 'W_FINE'] },
  { code: 'W_NICE', type: 'WORD', en: 'nice', zh: '好的/棒的', phonetic: '/naɪs/', sentence: 'Nice to meet you!', sentenceZh: '很高兴认识你！', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: ['W_GOOD', 'W_FINE'] },
  // Day 2 扩充: 人物身份
  { code: 'W_BOY', type: 'WORD', en: 'boy', zh: '男孩', phonetic: '/bɔɪ/', sentence: 'The boy is tall.', sentenceZh: '这个男孩很高。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_GIRL', 'W_TEACHER'] },
  { code: 'W_GIRL', type: 'WORD', en: 'girl', zh: '女孩', phonetic: '/ɡɜːrl/', sentence: 'The girl is smart.', sentenceZh: '这个女孩很聪明。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_BOY', 'W_STUDENT'] },
  { code: 'W_TEACHER', type: 'WORD', en: 'teacher', zh: '老师', phonetic: '/ˈtiːtʃər/', sentence: 'She is a teacher.', sentenceZh: '她是一位老师。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_STUDENT', 'W_BOY'] },
  { code: 'W_STUDENT', type: 'WORD', en: 'student', zh: '学生', phonetic: '/ˈstuːdənt/', sentence: 'I am a student.', sentenceZh: '我是一个学生。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: ['W_TEACHER', 'W_GIRL'] },
  // Day 3: I have 句型 & 日常物品
  { code: 'W_HAVE', type: 'WORD', en: 'have', zh: '有', phonetic: '/hæv/', sentence: 'I have a pen.', sentenceZh: '我有一支笔。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_AM', 'W_IS'] },
  { code: 'W_A', type: 'WORD', en: 'a', zh: '一个', phonetic: '/ə/', sentence: 'I have a book.', sentenceZh: '我有一本书。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_THE', 'W_THIS'] },
  { code: 'W_THE', type: 'WORD', en: 'the', zh: '这/那个(特指)', phonetic: '/ðə/', sentence: 'The book is on the desk.', sentenceZh: '书在桌子上。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_A', 'W_THIS'] },
  { code: 'W_BOOK', type: 'WORD', en: 'book', zh: '书', phonetic: '/bʊk/', sentence: 'I have a book.', sentenceZh: '我有一本书。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_PEN', 'W_BAG'] },
  { code: 'W_PEN', type: 'WORD', en: 'pen', zh: '笔', phonetic: '/pen/', sentence: 'This is my pen.', sentenceZh: '这是我的笔。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_BOOK', 'W_BAG'] },
  { code: 'W_BAG', type: 'WORD', en: 'bag', zh: '包/书包', phonetic: '/bæɡ/', sentence: 'My bag is big.', sentenceZh: '我的包很大。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_BOOK', 'W_PEN'] },
  { code: 'W_THIS', type: 'WORD', en: 'this', zh: '这个', phonetic: '/ðɪs/', sentence: 'This is a pen.', sentenceZh: '这是一支笔。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_THAT', 'W_THE'] },
  { code: 'W_THAT', type: 'WORD', en: 'that', zh: '那个', phonetic: '/ðæt/', sentence: 'That is a book.', sentenceZh: '那是一本书。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_THIS', 'W_THE'] },
  { code: 'W_IT', type: 'WORD', en: 'it', zh: '它', phonetic: '/ɪt/', sentence: 'It is a cat.', sentenceZh: '它是一只猫。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: ['W_THIS', 'W_THAT'] },
  // Day 4: 提问与回答
  { code: 'W_WHO', type: 'WORD', en: 'who', zh: '谁', phonetic: '/huː/', sentence: 'Who is he?', sentenceZh: '他是谁？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: ['W_WHAT', 'W_WHERE'] },
  { code: 'W_HOW', type: 'WORD', en: 'how', zh: '怎样/如何', phonetic: '/haʊ/', sentence: 'How are you?', sentenceZh: '你好吗？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: ['W_WHAT', 'W_WHO'] },
  { code: 'W_WHERE', type: 'WORD', en: 'where', zh: '哪里', phonetic: '/wer/', sentence: 'Where is the book?', sentenceZh: '书在哪里？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: ['W_WHO', 'W_HOW'] },
  { code: 'W_HERE', type: 'WORD', en: 'here', zh: '这里', phonetic: '/hɪr/', sentence: 'Come here, please.', sentenceZh: '请到这里来。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: ['W_THERE', 'W_WHERE'] },
  { code: 'W_THERE', type: 'WORD', en: 'there', zh: '那里', phonetic: '/ðer/', sentence: 'The pen is over there.', sentenceZh: '笔在那边。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: ['W_HERE', 'W_WHERE'] },
  { code: 'W_CAN', type: 'WORD', en: 'can', zh: '能/会', phonetic: '/kæn/', sentence: 'I can read.', sentenceZh: '我会读。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: ['W_HAVE', 'W_AM'] },
  // 句型/短语（不会生成 camp 怪物，但会出现在 Quest 和 Boss 题目中）
  { code: 'P_I_AM_NAME', type: 'PHRASE', en: 'I am ...', zh: '我是...', phonetic: '', sentence: 'I am Tom.', sentenceZh: '我是汤姆。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: [] },
  { code: 'P_MY_NAME_IS', type: 'PHRASE', en: 'My name is ...', zh: '我的名字是...', phonetic: '', sentence: 'My name is Amy.', sentenceZh: '我的名字是艾米。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: [] },
  { code: 'P_WHAT_IS_YOUR_NAME', type: 'PHRASE', en: 'What is your name?', zh: '你叫什么名字？', phonetic: '', sentence: 'What is your name?', sentenceZh: '你叫什么名字？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 2, distractors: [] },
  { code: 'P_HOW_ARE_YOU', type: 'PHRASE', en: 'How are you?', zh: '你好吗？', phonetic: '', sentence: 'How are you? I am fine.', sentenceZh: '你好吗？我很好。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 1, distractors: [] },
  // Day 3 新增短语
  { code: 'P_I_HAVE', type: 'PHRASE', en: 'I have ...', zh: '我有...', phonetic: '', sentence: 'I have a pen.', sentenceZh: '我有一支笔。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: [] },
  { code: 'P_THIS_IS', type: 'PHRASE', en: 'This is ...', zh: '这是...', phonetic: '', sentence: 'This is my book.', sentenceZh: '这是我的书。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 3, distractors: [] },
  // Day 4 新增短语
  { code: 'P_WHERE_IS', type: 'PHRASE', en: 'Where is ...?', zh: '...在哪里？', phonetic: '', sentence: 'Where is the pen?', sentenceZh: '笔在哪里？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: [] },
  { code: 'P_CAN_YOU', type: 'PHRASE', en: 'Can you ...?', zh: '你能...吗？', phonetic: '', sentence: 'Can you read this?', sentenceZh: '你能读这个吗？', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH1', dayIndex: 4, distractors: [] },
]

const ch1Boss: BossConfig = {
  code: 'PRE_A1_CH1_BOSS',
  chapterCode: 'PRE_A1_CH1',
  bossHp: 15,
  playerHp: 5,
  comboThreshold: 5,
  comboBonusDamage: 1,
  bossDamage: 1,
  dailyRetryLimit: 3,
  bossName: '守门人 Gatekeeper',
  bossType: 'gatekeeper',
  tier: 1,
}

const ch1Lessons: Lesson[] = [
  { code: 'PRE_A1_CH1_D01', chapterCode: 'PRE_A1_CH1', dayIndex: 1, titleEn: 'Day 1 — Greetings', titleZh: '第1天—问候与打招呼', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: true },
  { code: 'PRE_A1_CH1_D02', chapterCode: 'PRE_A1_CH1', dayIndex: 2, titleEn: 'Day 2 — Name & Introduction', titleZh: '第2天—名字与介绍', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH1_D03', chapterCode: 'PRE_A1_CH1', dayIndex: 3, titleEn: 'Day 3 — I am / I have', titleZh: '第3天—I am / I have 句型', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH1_D04', chapterCode: 'PRE_A1_CH1', dayIndex: 4, titleEn: 'Day 4 — Questions', titleZh: '第4天—提问与回答', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH1_D05', chapterCode: 'PRE_A1_CH1', dayIndex: 5, titleEn: 'Day 5 — Review & Practice', titleZh: '第5天—综合复习', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
]

// 地图瓦片 — 8 的位置会放怪物，由 generateMonsterEncounters 自动填充
// 24x18 扩大地图，40+ 个怪物格容纳全部单词
const ch1Tiles: number[][] = [
  [3, 3, 9, 3, 3, 0, 0, 2, 2, 2, 2, 0, 0, 3, 3, 9, 3, 3, 3, 3, 0, 0, 3, 3],
  [3, 0, 8, 0, 3, 0, 8, 0, 2, 2, 0, 8, 0, 0, 3, 0, 8, 0, 0, 3, 0, 8, 0, 3],
  [9, 0, 4, 0, 0, 1, 1, 0, 2, 0, 0, 1, 1, 0, 0, 0, 4, 0, 8, 3, 0, 0, 4, 9],
  [3, 0, 8, 0, 0, 1, 3, 0, 0, 0, 3, 1, 3, 0, 8, 0, 0, 0, 0, 9, 0, 8, 0, 3],
  [3, 0, 0, 5, 0, 1, 0, 8, 0, 0, 0, 1, 0, 0, 0, 8, 0, 7, 0, 3, 0, 0, 8, 3],
  [0, 8, 0, 0, 0, 1, 0, 0, 0, 8, 0, 1, 0, 8, 3, 0, 0, 0, 0, 0, 8, 0, 0, 0],
  [0, 0, 6, 0, 0, 1, 0, 8, 0, 5, 0, 1, 0, 0, 0, 8, 0, 0, 8, 0, 0, 0, 6, 0],
  [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0],
  [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0],
  [0, 0, 8, 0, 0, 1, 0, 0, 8, 0, 0, 1, 0, 0, 8, 0, 0, 6, 0, 0, 8, 0, 0, 0],
  [0, 8, 0, 0, 0, 1, 0, 0, 7, 0, 0, 1, 0, 8, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0],
  [3, 0, 0, 8, 0, 1, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0, 5, 0, 8, 3, 0, 0, 0, 3],
  [3, 0, 7, 0, 0, 1, 8, 0, 0, 8, 3, 1, 3, 0, 8, 0, 0, 8, 0, 3, 0, 8, 0, 3],
  [9, 0, 0, 8, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 4, 0, 0, 0, 9, 0, 0, 7, 9],
  [3, 0, 8, 3, 0, 0, 0, 8, 2, 2, 0, 0, 0, 8, 0, 0, 0, 8, 3, 3, 0, 8, 0, 3],
  [3, 3, 9, 3, 3, 0, 0, 2, 2, 2, 2, 0, 0, 3, 9, 3, 3, 3, 3, 3, 0, 0, 3, 3],
  [3, 0, 8, 0, 0, 8, 0, 0, 0, 0, 0, 0, 8, 0, 0, 8, 0, 0, 0, 3, 0, 8, 0, 3],
  [3, 3, 3, 3, 3, 0, 0, 0, 3, 3, 3, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3],
]

// NPC 和宝箱 — 手动放置
const ch1StaticEncounters: MapEncounter[] = [
  { id: 'ch1_npc_guide', type: 'npc', tileX: 2, tileY: 6, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, npcName: '冒险向导', npcAvatar: 'guide' as const, npcLines: ['欢迎来到营地，年轻的冒险者！', '在这片土地上，你会遇到许多单词怪物。', '击败它们来学习新单词吧！', '提示：用方向键或 WASD 移动。'] },
  { id: 'ch1_npc_sage', type: 'npc', tileX: 22, tileY: 6, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, npcName: '语法贤者', npcAvatar: 'sage' as const, npcLines: ['我是语法贤者。让我教你一个小窍门。', '"I am" 可以缩写为 "I\'m"。', '"You are" 可以缩写为 "You\'re"。', '记住：am 只跟 I 搭配！'] },
  { id: 'ch1_chest_1', type: 'treasure', tileX: 17, tileY: 4, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 20, coins: 15 } },
  { id: 'ch1_chest_2', type: 'treasure', tileX: 8, tileY: 10, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 15, coins: 10 } },
  { id: 'ch1_chest_3', type: 'treasure', tileX: 2, tileY: 12, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 15, coins: 10 } },
  { id: 'ch1_npc_objects', type: 'npc', tileX: 17, tileY: 9, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, npcName: '物品学者', npcAvatar: 'merchant' as const, npcLines: ['学会了物品名词，你就可以说 I have a book!', 'this 指近处，that 指远处。', '试试说：This is my pen.'] },
  { id: 'ch1_chest_4', type: 'treasure', tileX: 22, tileY: 13, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 25, coins: 20 } },
]

// ★ 核心：从 ContentItem 自动生成怪物 encounters ★
const ch1MonsterSlots = extractMonsterSlots(ch1Tiles)
const ch1MonsterEncounters = generateMonsterEncounters(ch1Words, ch1MonsterSlots, 'ch1')
const ch1CampMap: CampMapData = {
  width: 24, height: 18, tileSize: 64, playerStart: { x: 2, y: 7 },
  tiles: ch1Tiles,
  encounters: [...ch1MonsterEncounters, ...ch1StaticEncounters],
}

const ch1BossTasks: Task[] = [
  // 1. MCQ - hello 词义
  { code: 'B1_1', lessonCode: '', orderIndex: 1, type: 'MCQ', promptEn: 'Choose the meaning of "hello".', promptZhHint: '选择 hello 的意思', options: [{ key: 'A', textEn: 'hello', textZh: '你好' }, { key: 'B', textEn: 'hello', textZh: '再见' }, { key: 'C', textEn: 'hello', textZh: '谢谢' }], answer: { correctOptionKey: 'A' }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'hello' }, links: { contentItemCodes: ['W_HELLO'] } },
  // 2. SPELLING - goodbye
  { code: 'B1_2', lessonCode: '', orderIndex: 2, type: 'SPELLING', promptEn: 'Type the word you hear.', promptZhHint: '听音拼写', answer: { acceptedTexts: ['goodbye'] }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'goodbye' }, links: { contentItemCodes: ['W_GOODBYE'] } },
  // 3. MCQ - am 语法
  { code: 'B1_3', lessonCode: '', orderIndex: 3, type: 'MCQ', promptEn: 'Fill in: I ___ a student.', promptZhHint: '选择正确单词', options: [{ key: 'A', textEn: 'have', textZh: '有' }, { key: 'B', textEn: 'am', textZh: '是' }, { key: 'C', textEn: 'is', textZh: '是' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'I am a student.' }, links: { contentItemCodes: ['W_AM'] } },
  // 4. MCQ - name 词义
  { code: 'B1_4', lessonCode: '', orderIndex: 4, type: 'MCQ', promptEn: '"name" means ___', promptZhHint: '选择 name 的意思', options: [{ key: 'A', textEn: 'name', textZh: '年龄' }, { key: 'B', textEn: 'name', textZh: '名字' }, { key: 'C', textEn: 'name', textZh: '家庭' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'name' }, links: { contentItemCodes: ['W_NAME'] } },
  // 5. SPELLING - please
  { code: 'B1_5', lessonCode: '', orderIndex: 5, type: 'SPELLING', promptEn: 'Type the word you hear.', promptZhHint: '听音拼写', answer: { acceptedTexts: ['please'] }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'please' }, links: { contentItemCodes: ['W_PLEASE'] } },
  // 6. WORD_ORDER - Good morning
  { code: 'B1_6', lessonCode: '', orderIndex: 6, type: 'WORD_ORDER', promptEn: 'Put the words in order.', promptZhHint: '将单词排列成正确的句子：早上好！', fragments: ['morning', 'Good', '!'], answer: { orderedFragments: ['Good', 'morning', '!'] }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'Good morning!' }, links: { contentItemCodes: ['W_GOOD', 'W_MORNING'] } },
  // 7. FILL_BLANK - How are you
  { code: 'B1_7', lessonCode: '', orderIndex: 7, type: 'FILL_BLANK', promptEn: 'How ___ you?', promptZhHint: '填入正确单词：你好吗？', answer: { acceptedTexts: ['are'] }, explanationEn: '"How are you?" is a common greeting.', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'How are you?' }, links: { contentItemCodes: ['W_HOW', 'P_HOW_ARE_YOU'] } },
  // 8. MCQ_REVERSE - 听英文选中文
  { code: 'B1_8', lessonCode: '', orderIndex: 8, type: 'MCQ_REVERSE', promptEn: 'What does "teacher" mean?', promptZhHint: '选择 teacher 的中文意思', options: [{ key: 'A', textEn: 'teacher', textZh: '学生' }, { key: 'B', textEn: 'teacher', textZh: '老师' }, { key: 'C', textEn: 'teacher', textZh: '男孩' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'teacher' }, links: { contentItemCodes: ['W_TEACHER'] } },
  // 9. LISTEN_PICK - 听音选单词
  { code: 'B1_9', lessonCode: '', orderIndex: 9, type: 'LISTEN_PICK', promptEn: 'Listen and pick the correct word.', promptZhHint: '听发音选择正确单词', options: [{ key: 'A', textEn: 'book', textZh: '书' }, { key: 'B', textEn: 'bag', textZh: '包' }, { key: 'C', textEn: 'pen', textZh: '笔' }], answer: { correctOptionKey: 'A' }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'book' }, links: { contentItemCodes: ['W_BOOK'] } },
  // 10. WORD_ORDER - I have a pen
  { code: 'B1_10', lessonCode: '', orderIndex: 10, type: 'WORD_ORDER', promptEn: 'Put the words in order.', promptZhHint: '排列成正确句子：我有一支笔。', fragments: ['a', 'I', 'pen', 'have', '.'], answer: { orderedFragments: ['I', 'have', 'a', 'pen', '.'] }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'I have a pen.' }, links: { contentItemCodes: ['W_HAVE', 'W_PEN', 'P_I_HAVE'] } },
  // 11. SPELLING - morning
  { code: 'B1_11', lessonCode: '', orderIndex: 11, type: 'SPELLING', promptEn: 'Type the word: 早上', promptZhHint: '拼写出"早上"', answer: { acceptedTexts: ['morning'] }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'morning' }, links: { contentItemCodes: ['W_MORNING'] } },
  // 12. MCQ - this/that 辨析
  { code: 'B1_12', lessonCode: '', orderIndex: 12, type: 'MCQ', promptEn: '___ is my book. (pointing to something near you)', promptZhHint: '指近处的东西用什么？', options: [{ key: 'A', textEn: 'This', textZh: '这个' }, { key: 'B', textEn: 'That', textZh: '那个' }, { key: 'C', textEn: 'It', textZh: '它' }], answer: { correctOptionKey: 'A' }, explanationEn: 'Use "this" for things near you.', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'This is my book.' }, links: { contentItemCodes: ['W_THIS', 'P_THIS_IS'] } },
  // 13. FILL_BLANK - Where is
  { code: 'B1_13', lessonCode: '', orderIndex: 13, type: 'FILL_BLANK', promptEn: '___ is the pen?', promptZhHint: '填入正确单词：笔在哪里？', answer: { acceptedTexts: ['Where', 'where'] }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'Where is the pen?' }, links: { contentItemCodes: ['W_WHERE', 'P_WHERE_IS'] } },
  // 14. MCQ_REVERSE - can 词义
  { code: 'B1_14', lessonCode: '', orderIndex: 14, type: 'MCQ_REVERSE', promptEn: 'What does "can" mean?', promptZhHint: '选择 can 的意思', options: [{ key: 'A', textEn: 'can', textZh: '有' }, { key: 'B', textEn: 'can', textZh: '是' }, { key: 'C', textEn: 'can', textZh: '能/会' }], answer: { correctOptionKey: 'C' }, explanationEn: '', explanationZh: '', xpReward: 5, goldReward: 0, tts: { enabled: true, ttsTextEn: 'can' }, links: { contentItemCodes: ['W_CAN'] } },
  // 15. WORD_ORDER - Can you read this?
  { code: 'B1_15', lessonCode: '', orderIndex: 15, type: 'WORD_ORDER', promptEn: 'Put the words in order.', promptZhHint: '排列成正确句子：你能读这个吗？', fragments: ['you', 'this', 'Can', 'read', '?'], answer: { orderedFragments: ['Can', 'you', 'read', 'this', '?'] }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'Can you read this?' }, links: { contentItemCodes: ['W_CAN', 'P_CAN_YOU'] } },
]

// ============================================================
// Chapter 2: 数字与颜色 — Numbers & Colors
// ============================================================

const ch2Words: ContentItem[] = [
  { code: 'W_ONE', type: 'WORD', en: 'one', zh: '一', phonetic: '/wʌn/', sentence: 'I have one apple.', sentenceZh: '我有一个苹果。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 1, distractors: ['W_TWO', 'W_THREE'] },
  { code: 'W_TWO', type: 'WORD', en: 'two', zh: '二', phonetic: '/tuː/', sentence: 'Two cats are here.', sentenceZh: '这里有两只猫。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 1, distractors: ['W_ONE', 'W_FIVE'] },
  { code: 'W_THREE', type: 'WORD', en: 'three', zh: '三', phonetic: '/θriː/', sentence: 'Three dogs run fast.', sentenceZh: '三只狗跑得很快。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 1, distractors: ['W_FOUR', 'W_ONE'] },
  { code: 'W_FOUR', type: 'WORD', en: 'four', zh: '四', phonetic: '/fɔːr/', sentence: 'I see four birds.', sentenceZh: '我看到四只鸟。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 1, distractors: ['W_FIVE', 'W_THREE'] },
  { code: 'W_FIVE', type: 'WORD', en: 'five', zh: '五', phonetic: '/faɪv/', sentence: 'Five stars shine bright.', sentenceZh: '五颗星星闪闪发光。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 1, distractors: ['W_FOUR', 'W_TWO'] },
  { code: 'W_SIX', type: 'WORD', en: 'six', zh: '六', phonetic: '/sɪks/', sentence: 'There are six eggs.', sentenceZh: '有六个鸡蛋。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 1, distractors: ['W_SEVEN', 'W_FIVE'] },
  { code: 'W_SEVEN', type: 'WORD', en: 'seven', zh: '七', phonetic: '/ˈsevən/', sentence: 'Seven days in a week.', sentenceZh: '一周有七天。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 1, distractors: ['W_SIX', 'W_EIGHT'] },
  { code: 'W_EIGHT', type: 'WORD', en: 'eight', zh: '八', phonetic: '/eɪt/', sentence: 'I ate eight grapes.', sentenceZh: '我吃了八颗葡萄。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 2, distractors: ['W_NINE', 'W_SEVEN'] },
  { code: 'W_NINE', type: 'WORD', en: 'nine', zh: '九', phonetic: '/naɪn/', sentence: 'Nine is before ten.', sentenceZh: '九在十前面。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 2, distractors: ['W_TEN', 'W_EIGHT'] },
  { code: 'W_TEN', type: 'WORD', en: 'ten', zh: '十', phonetic: '/ten/', sentence: 'I have ten fingers.', sentenceZh: '我有十根手指。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 2, distractors: ['W_NINE', 'W_ONE'] },
  { code: 'W_RED', type: 'WORD', en: 'red', zh: '红色', phonetic: '/red/', sentence: 'The apple is red.', sentenceZh: '苹果是红色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 3, distractors: ['W_BLUE', 'W_GREEN'] },
  { code: 'W_BLUE', type: 'WORD', en: 'blue', zh: '蓝色', phonetic: '/bluː/', sentence: 'The sky is blue.', sentenceZh: '天空是蓝色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 3, distractors: ['W_RED', 'W_GREEN'] },
  { code: 'W_GREEN', type: 'WORD', en: 'green', zh: '绿色', phonetic: '/ɡriːn/', sentence: 'The tree is green.', sentenceZh: '树是绿色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 3, distractors: ['W_RED', 'W_BLUE'] },
  { code: 'W_YELLOW', type: 'WORD', en: 'yellow', zh: '黄色', phonetic: '/ˈjeloʊ/', sentence: 'The banana is yellow.', sentenceZh: '香蕉是黄色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 3, distractors: ['W_RED', 'W_WHITE'] },
  { code: 'W_WHITE', type: 'WORD', en: 'white', zh: '白色', phonetic: '/waɪt/', sentence: 'Snow is white.', sentenceZh: '雪是白色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 3, distractors: ['W_BLACK', 'W_YELLOW'] },
  { code: 'W_BLACK', type: 'WORD', en: 'black', zh: '黑色', phonetic: '/blæk/', sentence: 'The night is black.', sentenceZh: '夜晚是黑色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 4, distractors: ['W_WHITE', 'W_RED'] },
  { code: 'W_PINK', type: 'WORD', en: 'pink', zh: '粉色', phonetic: '/pɪŋk/', sentence: 'The flower is pink.', sentenceZh: '花是粉色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 4, distractors: ['W_RED', 'W_PURPLE'] },
  { code: 'W_PURPLE', type: 'WORD', en: 'purple', zh: '紫色', phonetic: '/ˈpɜːrpəl/', sentence: 'Grapes are purple.', sentenceZh: '葡萄是紫色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 4, distractors: ['W_PINK', 'W_BLUE'] },
  { code: 'W_BIG', type: 'WORD', en: 'big', zh: '大', phonetic: '/bɪɡ/', sentence: 'This is a big house.', sentenceZh: '这是一栋大房子。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 5, distractors: ['W_SMALL', 'W_RED'] },
  { code: 'W_SMALL', type: 'WORD', en: 'small', zh: '小', phonetic: '/smɔːl/', sentence: 'That is a small cat.', sentenceZh: '那是一只小猫。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 5, distractors: ['W_BIG', 'W_GREEN'] },
  { code: 'W_LONG', type: 'WORD', en: 'long', zh: '长', phonetic: '/lɔːŋ/', sentence: 'The snake is long.', sentenceZh: '蛇是长的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 5, distractors: ['W_SHORT', 'W_BIG'] },
  { code: 'W_SHORT', type: 'WORD', en: 'short', zh: '短', phonetic: '/ʃɔːrt/', sentence: 'The pencil is short.', sentenceZh: '铅笔是短的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH2', dayIndex: 5, distractors: ['W_LONG', 'W_SMALL'] },
]

const ch2Boss: BossConfig = {
  code: 'PRE_A1_CH2_BOSS',
  chapterCode: 'PRE_A1_CH2',
  bossHp: 12,
  playerHp: 5,
  comboThreshold: 4,
  comboBonusDamage: 1,
  bossDamage: 1,
  dailyRetryLimit: 3,
  bossName: '暗影猎手 Shadow Hunter',
  bossType: 'shadow_hunter',
  tier: 2,
}

const ch2Lessons: Lesson[] = [
  { code: 'PRE_A1_CH2_D01', chapterCode: 'PRE_A1_CH2', dayIndex: 1, titleEn: 'Day 1 — Numbers 1-5', titleZh: '第1天—数字1-5', estimatedMinutes: 25, targetTaskCount: 25, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH2_D02', chapterCode: 'PRE_A1_CH2', dayIndex: 2, titleEn: 'Day 2 — Colors (Red, Blue, Green)', titleZh: '第2天—颜色 红蓝绿', estimatedMinutes: 25, targetTaskCount: 25, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH2_D03', chapterCode: 'PRE_A1_CH2', dayIndex: 3, titleEn: 'Day 3 — Big & Small', titleZh: '第3天—大与小', estimatedMinutes: 25, targetTaskCount: 25, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH2_D04', chapterCode: 'PRE_A1_CH2', dayIndex: 4, titleEn: 'Day 4 — Counting Objects', titleZh: '第4天—数物品', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH2_D05', chapterCode: 'PRE_A1_CH2', dayIndex: 5, titleEn: 'Day 5 — Review & Practice', titleZh: '第5天—综合复习', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
]

const ch2Tiles: number[][] = [
  [3, 3, 3, 0, 0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 0, 3, 3, 3],
  [3, 0, 8, 0, 0, 8, 0, 0, 0, 3, 3, 0, 0, 0, 8, 0, 8, 0, 0, 3],
  [0, 0, 4, 0, 0, 0, 0, 7, 0, 0, 0, 0, 7, 0, 0, 0, 0, 4, 0, 0],
  [0, 8, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 8, 0],
  [0, 0, 0, 0, 1, 0, 8, 0, 0, 5, 0, 0, 8, 0, 0, 1, 0, 0, 0, 0],
  [0, 8, 0, 0, 1, 0, 3, 0, 0, 0, 0, 0, 3, 0, 0, 1, 0, 0, 8, 0],
  [0, 0, 6, 0, 1, 0, 0, 0, 2, 2, 2, 0, 0, 0, 0, 1, 0, 6, 0, 0],
  [0, 0, 0, 0, 1, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 1, 0, 0, 0, 0],
  [0, 0, 0, 0, 1, 0, 0, 0, 2, 2, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0],
  [0, 8, 0, 0, 1, 0, 3, 0, 0, 0, 0, 0, 3, 0, 0, 1, 0, 0, 8, 0],
  [0, 0, 0, 0, 1, 0, 8, 0, 0, 5, 0, 0, 8, 0, 0, 1, 0, 0, 0, 0],
  [0, 8, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 8, 0],
  [0, 0, 7, 0, 0, 0, 8, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 7, 0, 9],
  [3, 0, 0, 0, 0, 8, 0, 0, 0, 0, 1, 0, 0, 0, 8, 0, 0, 0, 0, 3],
  [3, 3, 3, 0, 0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 0, 3, 3, 3],
]

const ch2StaticEncounters: MapEncounter[] = [
  { id: 'ch2_npc_1', type: 'npc', tileX: 2, tileY: 6, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, npcName: '数字导师', npcAvatar: 'guide' as const, npcLines: ['冒险者，欢迎来到数字之境！', '学会数字，你就能数清战利品了。', '试试用英语数到五：one, two, three, four, five'] },
  { id: 'ch2_npc_2', type: 'npc', tileX: 17, tileY: 6, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, npcName: '色彩贤者', npcAvatar: 'sage' as const, npcLines: ['颜色也是很重要的基础知识！', 'red = 红, blue = 蓝, green = 绿', '试试描述你身边的事物颜色吧！'] },
  { id: 'ch2_chest_1', type: 'treasure', tileX: 7, tileY: 2, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 18, coins: 12 } },
  { id: 'ch2_chest_2', type: 'treasure', tileX: 12, tileY: 2, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 18, coins: 12 } },
  { id: 'ch2_chest_3', type: 'treasure', tileX: 2, tileY: 12, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 20, coins: 15 } },
  { id: 'ch2_chest_4', type: 'treasure', tileX: 17, tileY: 12, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 20, coins: 15 } },
]

const ch2MonsterSlots = extractMonsterSlots(ch2Tiles)
const ch2MonsterEncounters = generateMonsterEncounters(ch2Words, ch2MonsterSlots, 'ch2')
const ch2CampMap: CampMapData = {
  width: 20, height: 15, tileSize: 64, playerStart: { x: 10, y: 13 },
  tiles: ch2Tiles,
  encounters: [...ch2MonsterEncounters, ...ch2StaticEncounters],
}

const ch2BossTasks: Task[] = [
  { code: 'B2_1', lessonCode: '', orderIndex: 1, type: 'MCQ', promptEn: 'What number is "three"?', promptZhHint: '选择 three 的意思', options: [{ key: 'A', textEn: 'three', textZh: '二' }, { key: 'B', textEn: 'three', textZh: '三' }, { key: 'C', textEn: 'three', textZh: '四' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'three' }, links: { contentItemCodes: ['W_THREE'] } },
  { code: 'B2_2', lessonCode: '', orderIndex: 2, type: 'SPELLING', promptEn: 'Type the color you hear.', promptZhHint: '听音拼写颜色', answer: { acceptedTexts: ['blue'] }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'blue' }, links: { contentItemCodes: ['W_BLUE'] } },
  { code: 'B2_3', lessonCode: '', orderIndex: 3, type: 'MCQ', promptEn: 'The opposite of "big" is ___.', promptZhHint: '大的反义词', options: [{ key: 'A', textEn: 'tall', textZh: '高' }, { key: 'B', textEn: 'small', textZh: '小' }, { key: 'C', textEn: 'long', textZh: '长' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'small' }, links: { contentItemCodes: ['W_SMALL'] } },
  { code: 'B2_4', lessonCode: '', orderIndex: 4, type: 'MCQ', promptEn: 'What color is the sky?', promptZhHint: '天空是什么颜色', options: [{ key: 'A', textEn: 'red', textZh: '红色' }, { key: 'B', textEn: 'green', textZh: '绿色' }, { key: 'C', textEn: 'blue', textZh: '蓝色' }], answer: { correctOptionKey: 'C' }, explanationEn: '', explanationZh: '', xpReward: 6, goldReward: 0, tts: { enabled: true, ttsTextEn: 'blue' }, links: { contentItemCodes: ['W_BLUE'] } },
]

// ============================================================
// Chapter 3: 家庭与动物 — Family & Animals
// ============================================================

const ch3Words: ContentItem[] = [
  { code: 'W_MOM', type: 'WORD', en: 'mom', zh: '妈妈', phonetic: '/mɑːm/', sentence: 'My mom is nice.', sentenceZh: '我妈妈很好。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 1, distractors: ['W_DAD', 'W_CAT'] },
  { code: 'W_DAD', type: 'WORD', en: 'dad', zh: '爸爸', phonetic: '/dæd/', sentence: 'My dad is tall.', sentenceZh: '我爸爸很高。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 1, distractors: ['W_MOM', 'W_DOG'] },
  { code: 'W_BROTHER', type: 'WORD', en: 'brother', zh: '兄弟', phonetic: '/ˈbrʌðər/', sentence: 'My brother is young.', sentenceZh: '我弟弟很小。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 1, distractors: ['W_SISTER', 'W_DAD'] },
  { code: 'W_SISTER', type: 'WORD', en: 'sister', zh: '姐妹', phonetic: '/ˈsɪstər/', sentence: 'My sister is smart.', sentenceZh: '我姐姐很聪明。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 1, distractors: ['W_BROTHER', 'W_MOM'] },
  { code: 'W_FAMILY', type: 'WORD', en: 'family', zh: '家庭', phonetic: '/ˈfæmɪli/', sentence: 'I love my family.', sentenceZh: '我爱我的家人。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 1, distractors: ['W_FRIEND', 'W_MOM'] },
  { code: 'W_FRIEND', type: 'WORD', en: 'friend', zh: '朋友', phonetic: '/frend/', sentence: 'He is my friend.', sentenceZh: '他是我的朋友。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 2, distractors: ['W_FAMILY', 'W_BROTHER'] },
  { code: 'W_CAT', type: 'WORD', en: 'cat', zh: '猫', phonetic: '/kæt/', sentence: 'The cat is cute.', sentenceZh: '这只猫很可爱。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 2, distractors: ['W_DOG', 'W_FISH'] },
  { code: 'W_DOG', type: 'WORD', en: 'dog', zh: '狗', phonetic: '/dɔːɡ/', sentence: 'I like the dog.', sentenceZh: '我喜欢这只狗。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 2, distractors: ['W_CAT', 'W_BIRD'] },
  { code: 'W_FISH', type: 'WORD', en: 'fish', zh: '鱼', phonetic: '/fɪʃ/', sentence: 'The fish swims fast.', sentenceZh: '鱼游得很快。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 3, distractors: ['W_BIRD', 'W_CAT'] },
  { code: 'W_BIRD', type: 'WORD', en: 'bird', zh: '鸟', phonetic: '/bɜːrd/', sentence: 'A bird can fly.', sentenceZh: '鸟会飞。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 3, distractors: ['W_FISH', 'W_DOG'] },
  { code: 'W_RABBIT', type: 'WORD', en: 'rabbit', zh: '兔子', phonetic: '/ˈræbɪt/', sentence: 'The rabbit is white.', sentenceZh: '兔子是白色的。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 3, distractors: ['W_CAT', 'W_DUCK'] },
  { code: 'W_DUCK', type: 'WORD', en: 'duck', zh: '鸭子', phonetic: '/dʌk/', sentence: 'The duck swims in the lake.', sentenceZh: '鸭子在湖里游泳。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 3, distractors: ['W_BIRD', 'W_RABBIT'] },
  { code: 'W_BEAR', type: 'WORD', en: 'bear', zh: '熊', phonetic: '/ber/', sentence: 'The bear is big.', sentenceZh: '熊很大。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 3, distractors: ['W_DOG', 'W_ELEPHANT'] },
  { code: 'W_ELEPHANT', type: 'WORD', en: 'elephant', zh: '大象', phonetic: '/ˈelɪfənt/', sentence: 'An elephant has a long nose.', sentenceZh: '大象有一个长鼻子。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 4, distractors: ['W_BEAR', 'W_MONKEY'] },
  { code: 'W_MONKEY', type: 'WORD', en: 'monkey', zh: '猴子', phonetic: '/ˈmʌŋki/', sentence: 'The monkey climbs trees.', sentenceZh: '猴子爬树。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 4, distractors: ['W_ELEPHANT', 'W_BIRD'] },
  { code: 'W_LIKE', type: 'WORD', en: 'like', zh: '喜欢', phonetic: '/laɪk/', sentence: 'I like cats.', sentenceZh: '我喜欢猫。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 4, distractors: ['W_LOVE', 'W_HAPPY'] },
  { code: 'W_LOVE', type: 'WORD', en: 'love', zh: '爱', phonetic: '/lʌv/', sentence: 'I love my mom.', sentenceZh: '我爱我妈妈。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 4, distractors: ['W_LIKE', 'W_SAD'] },
  { code: 'W_HAPPY', type: 'WORD', en: 'happy', zh: '开心', phonetic: '/ˈhæpi/', sentence: 'I am happy today.', sentenceZh: '我今天很开心。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 5, distractors: ['W_SAD', 'W_LIKE'] },
  { code: 'W_SAD', type: 'WORD', en: 'sad', zh: '伤心', phonetic: '/sæd/', sentence: 'She is sad.', sentenceZh: '她很伤心。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 5, distractors: ['W_HAPPY', 'W_LOVE'] },
  { code: 'W_HUNGRY', type: 'WORD', en: 'hungry', zh: '饿', phonetic: '/ˈhʌŋɡri/', sentence: 'I am hungry.', sentenceZh: '我饿了。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 5, distractors: ['W_THIRSTY', 'W_HAPPY'] },
  { code: 'W_THIRSTY', type: 'WORD', en: 'thirsty', zh: '渴', phonetic: '/ˈθɜːrsti/', sentence: 'I am thirsty.', sentenceZh: '我渴了。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 5, distractors: ['W_HUNGRY', 'W_SAD'] },
  { code: 'W_TIRED', type: 'WORD', en: 'tired', zh: '累', phonetic: '/taɪərd/', sentence: 'I am tired.', sentenceZh: '我累了。', cefrLevel: 'PRE_A1', chapterCode: 'PRE_A1_CH3', dayIndex: 5, distractors: ['W_HAPPY', 'W_HUNGRY'] },
]

const ch3Boss: BossConfig = {
  code: 'PRE_A1_CH3_BOSS',
  chapterCode: 'PRE_A1_CH3',
  bossHp: 15,
  playerHp: 6,
  comboThreshold: 4,
  comboBonusDamage: 2,
  bossDamage: 1,
  dailyRetryLimit: 3,
  bossName: '烈焰守卫 Flame Warden',
  bossType: 'flame_warden',
  tier: 3,
}

const ch3Lessons: Lesson[] = [
  { code: 'PRE_A1_CH3_D01', chapterCode: 'PRE_A1_CH3', dayIndex: 1, titleEn: 'Day 1 — Family: Mom & Dad', titleZh: '第1天—家人：妈妈和爸爸', estimatedMinutes: 25, targetTaskCount: 25, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH3_D02', chapterCode: 'PRE_A1_CH3', dayIndex: 2, titleEn: 'Day 2 — Animals: Cat & Dog', titleZh: '第2天—动物：猫和狗', estimatedMinutes: 25, targetTaskCount: 25, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH3_D03', chapterCode: 'PRE_A1_CH3', dayIndex: 3, titleEn: 'Day 3 — Animals: Fish & Bird', titleZh: '第3天—动物：鱼和鸟', estimatedMinutes: 25, targetTaskCount: 25, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH3_D04', chapterCode: 'PRE_A1_CH3', dayIndex: 4, titleEn: 'Day 4 — Like & Love', titleZh: '第4天—喜欢与爱', estimatedMinutes: 25, targetTaskCount: 25, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH3_D05', chapterCode: 'PRE_A1_CH3', dayIndex: 5, titleEn: 'Day 5 — Happy & Sad', titleZh: '第5天—快乐与悲伤', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
]

const ch3Tiles: number[][] = [
  [3, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3],
  [3, 0, 8, 0, 0, 0, 8, 0, 0, 8, 0, 8, 0, 0, 8, 0, 0, 8, 0, 3],
  [3, 0, 4, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 4, 0, 3],
  [0, 0, 0, 0, 7, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 7, 0, 0, 0, 0],
  [0, 8, 0, 0, 0, 0, 1, 0, 0, 5, 0, 0, 0, 1, 0, 0, 0, 0, 8, 0],
  [0, 0, 0, 0, 0, 1, 0, 0, 3, 0, 0, 3, 0, 0, 1, 0, 0, 0, 0, 0],
  [0, 8, 0, 6, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 6, 0, 8, 0],
  [0, 0, 1, 1, 0, 0, 0, 2, 2, 0, 0, 2, 2, 0, 0, 0, 1, 1, 0, 0],
  [0, 8, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 8, 0],
  [0, 0, 0, 0, 0, 1, 0, 0, 3, 0, 0, 3, 0, 0, 1, 0, 0, 0, 0, 0],
  [0, 8, 0, 0, 0, 0, 1, 0, 0, 5, 0, 0, 0, 1, 0, 0, 0, 0, 8, 0],
  [0, 0, 0, 0, 7, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 7, 0, 0, 0, 0],
  [3, 0, 4, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 4, 0, 3],
  [3, 0, 8, 0, 0, 0, 8, 0, 0, 8, 0, 8, 0, 0, 8, 0, 0, 8, 0, 3],
  [3, 3, 3, 3, 0, 0, 0, 0, 0, 9, 9, 0, 0, 0, 0, 0, 3, 3, 3, 3],
]

const ch3StaticEncounters: MapEncounter[] = [
  { id: 'ch3_npc_1', type: 'npc', tileX: 3, tileY: 6, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, npcName: '森林向导', npcAvatar: 'knight' as const, npcLines: ['冒险者你好！这里是动物森林🌲', '在这里你会遇到很多可爱的动物单词。', 'cat 是猫，dog 是狗，记住了吗？'] },
  { id: 'ch3_npc_2', type: 'npc', tileX: 16, tileY: 6, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, npcName: '家族长老', npcAvatar: 'sage' as const, npcLines: ['家庭是最温暖的地方！', 'mom 是妈妈，dad 是爸爸。', '试着说：I love my mom and dad!'] },
  { id: 'ch3_chest_1', type: 'treasure', tileX: 4, tileY: 3, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 22, coins: 15 } },
  { id: 'ch3_chest_2', type: 'treasure', tileX: 15, tileY: 3, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 22, coins: 15 } },
  { id: 'ch3_chest_3', type: 'treasure', tileX: 4, tileY: 11, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 25, coins: 18 } },
  { id: 'ch3_chest_4', type: 'treasure', tileX: 15, tileY: 11, wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '', defeated: false, reward: { xp: 25, coins: 18 } },
]

const ch3MonsterSlots = extractMonsterSlots(ch3Tiles)
const ch3MonsterEncounters = generateMonsterEncounters(ch3Words, ch3MonsterSlots, 'ch3')
const ch3CampMap: CampMapData = {
  width: 20, height: 15, tileSize: 64, playerStart: { x: 10, y: 7 },
  tiles: ch3Tiles,
  encounters: [...ch3MonsterEncounters, ...ch3StaticEncounters],
}

const ch3BossTasks: Task[] = [
  { code: 'B3_1', lessonCode: '', orderIndex: 1, type: 'MCQ', promptEn: 'What is "cat" in Chinese?', promptZhHint: '选择 cat 的中文意思', options: [{ key: 'A', textEn: 'cat', textZh: '狗' }, { key: 'B', textEn: 'cat', textZh: '猫' }, { key: 'C', textEn: 'cat', textZh: '鸟' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 7, goldReward: 0, tts: { enabled: true, ttsTextEn: 'cat' }, links: { contentItemCodes: ['W_CAT'] } },
  { code: 'B3_2', lessonCode: '', orderIndex: 2, type: 'SPELLING', promptEn: 'Type the word you hear.', promptZhHint: '听音拼写', answer: { acceptedTexts: ['happy'] }, explanationEn: '', explanationZh: '', xpReward: 7, goldReward: 0, tts: { enabled: true, ttsTextEn: 'happy' }, links: { contentItemCodes: ['W_HAPPY'] } },
  { code: 'B3_3', lessonCode: '', orderIndex: 3, type: 'MCQ', promptEn: 'I ___ my mom.', promptZhHint: '选择正确单词', options: [{ key: 'A', textEn: 'like', textZh: '喜欢' }, { key: 'B', textEn: 'love', textZh: '爱' }, { key: 'C', textEn: 'am', textZh: '是' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 7, goldReward: 0, tts: { enabled: true, ttsTextEn: 'I love my mom.' }, links: { contentItemCodes: ['W_LOVE'] } },
  { code: 'B3_4', lessonCode: '', orderIndex: 4, type: 'MCQ', promptEn: 'The opposite of "happy" is ___.', promptZhHint: 'happy 的反义词', options: [{ key: 'A', textEn: 'big', textZh: '大' }, { key: 'B', textEn: 'sad', textZh: '伤心' }, { key: 'C', textEn: 'small', textZh: '小' }], answer: { correctOptionKey: 'B' }, explanationEn: '', explanationZh: '', xpReward: 7, goldReward: 0, tts: { enabled: true, ttsTextEn: 'sad' }, links: { contentItemCodes: ['W_SAD'] } },
  { code: 'B3_5', lessonCode: '', orderIndex: 5, type: 'SPELLING', promptEn: 'Type the word you hear.', promptZhHint: '听音拼写', answer: { acceptedTexts: ['bird'] }, explanationEn: '', explanationZh: '', xpReward: 7, goldReward: 0, tts: { enabled: true, ttsTextEn: 'bird' }, links: { contentItemCodes: ['W_BIRD'] } },
]

// ============================================================
// Exports
// ============================================================

export const chapterConfigs: ChapterConfig[] = [
  {
    code: 'PRE_A1_CH1',
    cefrLevel: 'PRE_A1',
    titleEn: 'Chapter 1 — First Contact',
    titleZh: '第一章 · 初次接触',
    orderIndex: 1,
    days: 5,
    campUnlockRate: 0.8,
    words: ch1Words,
    bossConfig: ch1Boss,
  },
  {
    code: 'PRE_A1_CH2',
    cefrLevel: 'PRE_A1',
    titleEn: 'Chapter 2 — Numbers & Colors',
    titleZh: '第二章 · 数字与颜色',
    orderIndex: 2,
    days: 5,
    campUnlockRate: 0.8,
    words: ch2Words,
    bossConfig: ch2Boss,
  },
  {
    code: 'PRE_A1_CH3',
    cefrLevel: 'PRE_A1',
    titleEn: 'Chapter 3 — Family & Animals',
    titleZh: '第三章 · 家庭与动物',
    orderIndex: 3,
    days: 5,
    campUnlockRate: 0.8,
    words: ch3Words,
    bossConfig: ch3Boss,
  },
]

export const chapterLessons: Record<string, Lesson[]> = {
  PRE_A1_CH1: ch1Lessons,
  PRE_A1_CH2: ch2Lessons,
  PRE_A1_CH3: ch3Lessons,
}

export const chapterCampMaps: Record<string, CampMapData> = {
  PRE_A1_CH1: ch1CampMap,
  PRE_A1_CH2: ch2CampMap,
  PRE_A1_CH3: ch3CampMap,
}

export const chapterBossTasks: Record<string, Task[]> = {
  PRE_A1_CH1: ch1BossTasks,
  PRE_A1_CH2: ch2BossTasks,
  PRE_A1_CH3: ch3BossTasks,
}

// DEV: 启动时打印覆盖率报告
if (import.meta.env.DEV) {
  import('@/data/coverageCheck').then(({ logCoverageReport }) => {
    for (const ch of chapterConfigs) {
      const camp = chapterCampMaps[ch.code]
      const boss = chapterBossTasks[ch.code] ?? []
      const monsterCount = camp?.encounters.filter(e => e.type === 'monster').length ?? 0
      logCoverageReport(ch.code, ch.words, monsterCount, [], boss)
    }
  })
}
