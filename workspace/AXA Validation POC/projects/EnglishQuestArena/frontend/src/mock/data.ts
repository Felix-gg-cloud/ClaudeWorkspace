// Mock data for frontend-only development
import type { User, Task, TaskOption, Lesson, BossConfig, SkillNode, CefrExam } from '@/types'

export const mockUser: User = {
  id: 1,
  username: 'admin',
  displayName: 'Hero',
  avatar: '',
  ttsVoice: 'en-US',
  cefrLevel: 'PRE_A1',
  currentLevel: 2,
  totalXp: 245,
  xpToNextLevel: 500,
  coins: 120,
  skillPoints: 3,
  firstLogin: false
}

export const mockLessons: Lesson[] = [
  { code: 'PRE_A1_CH1_D01', chapterCode: 'PRE_A1_CH1', dayIndex: 1, titleEn: 'Day 1 — Greetings', titleZh: '第1天—问候与 I am', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: true, current: false },
  { code: 'PRE_A1_CH1_D02', chapterCode: 'PRE_A1_CH1', dayIndex: 2, titleEn: 'Day 2 — Name & Introduction', titleZh: '第2天—名字与介绍', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: true },
  { code: 'PRE_A1_CH1_D03', chapterCode: 'PRE_A1_CH1', dayIndex: 3, titleEn: 'Day 3 — Numbers 1-5 & I have', titleZh: '第3天—数字1-5与 I have', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH1_D04', chapterCode: 'PRE_A1_CH1', dayIndex: 4, titleEn: 'Day 4 — Numbers 6-10', titleZh: '第4天—数字6-10', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
  { code: 'PRE_A1_CH1_D05', chapterCode: 'PRE_A1_CH1', dayIndex: 5, titleEn: 'Day 5 — This/That', titleZh: '第5天—this/that 指物', estimatedMinutes: 30, targetTaskCount: 30, autoDrillEnabled: true, completed: false, current: false },
]

export const mockTodayTasks: Task[] = [
  {
    code: 'PRE_A1_CH1_D02_WU01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 1, type: 'MCQ',
    promptEn: 'Choose the correct reply to: "Hi!"',
    promptZhHint: '提示：选择合适回复',
    options: [
      { key: 'A', textEn: 'Hello!', textZh: '你好！' },
      { key: 'B', textEn: 'Goodbye!', textZh: '再见！' },
      { key: 'C', textEn: 'Morning!', textZh: '早晨！' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: 'You reply with "Hello!" or "Hi!"',
    explanationZh: '用 Hello 或 Hi 回应。',
    xpReward: 5, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'Hi!' },
    links: { contentItemCodes: ['W_HI', 'W_HELLO'] }
  },
  {
    code: 'PRE_A1_CH1_D02_WU02', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 2, type: 'MCQ',
    promptEn: 'Fill in: I ___ {playerName}.',
    promptZhHint: '提示：选择正确单词',
    options: [
      { key: 'A', textEn: 'am', textZh: '是' },
      { key: 'B', textEn: 'is', textZh: '是' },
      { key: 'C', textEn: 'are', textZh: '是' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: 'Use "am" with "I".',
    explanationZh: 'I 后面用 am。',
    xpReward: 6, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'I am {playerName}.' },
    links: { contentItemCodes: ['W_I', 'W_AM', 'P_I_AM_NAME'] }
  },
  {
    code: 'PRE_A1_CH1_D02_C01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 3, type: 'FLASHCARD',
    promptEn: 'my', promptZhHint: '我的',
    answer: { action: 'GOT_IT' },
    explanationEn: 'My = belonging to me.',
    explanationZh: 'my 表示"我的"。',
    xpReward: 2, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'my' },
    links: { contentItemCodes: ['W_MY'] }
  },
  {
    code: 'PRE_A1_CH1_D02_C02', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 4, type: 'FLASHCARD',
    promptEn: 'your', promptZhHint: '你的',
    answer: { action: 'GOT_IT' },
    explanationEn: 'Your = belonging to you.',
    explanationZh: 'your 表示"你的"。',
    xpReward: 2, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'your' },
    links: { contentItemCodes: ['W_YOUR'] }
  },
  {
    code: 'PRE_A1_CH1_D02_S01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 5, type: 'SPELLING',
    promptEn: 'Type the word you hear.',
    promptZhHint: '提示：听音拼写',
    answer: { acceptedTexts: ['name'] },
    explanationEn: '', explanationZh: '',
    xpReward: 8, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'name' },
    links: { contentItemCodes: ['W_NAME'] }
  },
  {
    code: 'PRE_A1_CH1_D02_O01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 6, type: 'WORD_ORDER',
    promptEn: 'Arrange the words.',
    promptZhHint: '提示：排序成句子',
    tokens: ['name', 'is', 'My', '{playerName}', '.'],
    answer: { correctTokens: ['My', 'name', 'is', '{playerName}', '.'] },
    explanationEn: '', explanationZh: '',
    xpReward: 8, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'My name is {playerName}.' },
    links: { contentItemCodes: ['P_MY_NAME_IS'] }
  },
  {
    code: 'PRE_A1_CH1_D02_DLG01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 7, type: 'DIALOGUE_REVIEW',
    promptEn: '', promptZhHint: '',
    dialogue: {
      lines: [
        { en: 'Hi!', zh: '嗨！', ttsTextEn: 'Hi!' },
        { en: 'Hello!', zh: '你好！', ttsTextEn: 'Hello!' },
        { en: 'What is your name?', zh: '你叫什么名字？', ttsTextEn: 'What is your name?' },
        { en: 'My name is {playerName}.', zh: '我的名字是{playerName}。', ttsTextEn: 'My name is {playerName}.' },
        { en: 'Nice to meet you.', zh: '很高兴认识你。', ttsTextEn: 'Nice to meet you.' }
      ]
    },
    followUpQuestions: [
      {
        type: 'MCQ',
        promptEn: 'Which sentence asks a name?',
        promptZhHint: '提示：哪一句是在问名字？',
        options: [
          { key: 'A', textEn: 'What is your name?', textZh: '你叫什么名字？' },
          { key: 'B', textEn: 'Goodbye!', textZh: '再见！' }
        ],
        answer: { correctOptionKey: 'A' }
      }
    ],
    answer: {},
    explanationEn: '', explanationZh: '',
    xpReward: 10, goldReward: 0,
    tts: { enabled: true, ttsTextEn: '' },
    links: { contentItemCodes: ['P_WHAT_IS_YOUR_NAME', 'P_MY_NAME_IS'] }
  },
  // ——— 新增题型 ———
  {
    code: 'PRE_A1_CH1_D02_MR01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 8, type: 'MCQ_REVERSE',
    cefrLevel: 'PRE_A1',
    promptEn: '你好',
    promptZhHint: '你好',
    options: [
      { key: 'A', textEn: 'hello', textZh: '你好' },
      { key: 'B', textEn: 'goodbye', textZh: '再见' },
      { key: 'C', textEn: 'thank you', textZh: '谢谢' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: '"hello" means 你好.',
    explanationZh: 'hello 是英文中最常用的问候语。',
    xpReward: 5, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'hello' },
    links: { contentItemCodes: ['W_HELLO'] }
  },
  {
    code: 'PRE_A1_CH1_D02_FB01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 9, type: 'FILL_BLANK',
    cefrLevel: 'A1',
    promptEn: 'I ___ a student.',
    promptZhHint: '我是一个学生。',
    options: [
      { key: 'A', textEn: 'am', textZh: '是' },
      { key: 'B', textEn: 'is', textZh: '是' },
      { key: 'C', textEn: 'are', textZh: '是' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: 'Use "am" with "I".',
    explanationZh: '主语 I 后面搭配 am。',
    xpReward: 8, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'I am a student.' },
    links: { contentItemCodes: ['W_AM', 'P_I_AM_NAME'] }
  },
  {
    code: 'PRE_A1_CH1_D02_LP01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 10, type: 'LISTEN_PICK',
    cefrLevel: 'PRE_A1',
    promptEn: 'Listen and pick the correct word.',
    promptZhHint: '听发音，选择正确的单词',
    options: [
      { key: 'A', textEn: 'name', textZh: '名字' },
      { key: 'B', textEn: 'game', textZh: '游戏' },
      { key: 'C', textEn: 'same', textZh: '相同' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: 'The word is "name".',
    explanationZh: '发音是 name（名字）。',
    xpReward: 6, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'name' },
    links: { contentItemCodes: ['W_NAME'] }
  },
  // ——— 新增题型 LISTEN_FILL ———
  {
    code: 'PRE_A1_CH1_D02_LF01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 11, type: 'LISTEN_FILL',
    cefrLevel: 'PRE_A1',
    promptEn: 'Type the word you hear.',
    promptZhHint: '听发音，拼写你听到的单词',
    answer: { acceptedTexts: ['hello'] },
    explanationEn: 'The word is "hello".',
    explanationZh: '这个单词是 hello（你好）。',
    xpReward: 8, goldReward: 1,
    tts: { enabled: true, ttsTextEn: 'hello' },
    links: { contentItemCodes: ['W_HELLO'] }
  },
  // ——— 新增题型 SITUATION_PICK ———
  {
    code: 'PRE_A1_CH1_D02_SP01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 12, type: 'SITUATION_PICK',
    cefrLevel: 'PRE_A1',
    promptEn: 'What do you say?',
    promptZhHint: '你在街上遇到一个朋友，你应该怎么打招呼？',
    options: [
      { key: 'A', textEn: 'Hello! How are you?', textZh: '你好！你怎么样？' },
      { key: 'B', textEn: 'Goodbye!', textZh: '再见！' },
      { key: 'C', textEn: 'I am sorry.', textZh: '对不起。' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: 'When you meet a friend, you say "Hello! How are you?"',
    explanationZh: '遇到朋友要用 Hello 打招呼，并用 How are you? 问候。',
    xpReward: 6, goldReward: 1,
    tts: { enabled: true, ttsTextEn: 'Hello! How are you?' },
    links: { contentItemCodes: ['W_HELLO', 'P_HOW_ARE_YOU'] }
  },
  // ——— 新增题型 ERROR_FIX ———
  {
    code: 'PRE_A1_CH1_D02_EF01', lessonCode: 'PRE_A1_CH1_D02', orderIndex: 13, type: 'ERROR_FIX',
    cefrLevel: 'PRE_A1',
    promptEn: 'I is a student.',
    promptZhHint: '找出语法错误并选择正确的句子',
    options: [
      { key: 'A', textEn: 'I am a student.', textZh: '我是一个学生。' },
      { key: 'B', textEn: 'I are a student.', textZh: '我是一个学生。' },
      { key: 'C', textEn: 'I is a students.', textZh: '我是学生们。' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: '"I" uses "am", not "is". → I am a student.',
    explanationZh: '主语 I 要搭配 am，不能用 is。正确：I am a student.',
    xpReward: 8, goldReward: 1,
    tts: { enabled: true, ttsTextEn: 'I am a student.' },
    links: { contentItemCodes: ['W_AM', 'P_I_AM_NAME'] }
  }
]

export const mockBossConfig: BossConfig = {
  code: 'PRE_A1_CH1_BOSS',
  chapterCode: 'PRE_A1_CH1',
  bossHp: 10,
  playerHp: 5,
  comboThreshold: 5,
  comboBonusDamage: 1,
  bossDamage: 1,
  dailyRetryLimit: 1,
  bossName: '守门人 Gatekeeper',
  bossType: 'gatekeeper',
  tier: 1
}

export const mockBossTasks: Task[] = [
  {
    code: 'BOSS_1', lessonCode: '', orderIndex: 1, type: 'MCQ',
    promptEn: 'Choose the meaning of "hello".',
    promptZhHint: '选择 hello 的意思',
    options: [
      { key: 'A', textEn: 'hello', textZh: '你好' },
      { key: 'B', textEn: 'hello', textZh: '再见' },
      { key: 'C', textEn: 'hello', textZh: '谢谢' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: '', explanationZh: '',
    xpReward: 5, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'hello' },
    links: { contentItemCodes: ['W_HELLO'] }
  },
  {
    code: 'BOSS_2', lessonCode: '', orderIndex: 2, type: 'SPELLING',
    promptEn: 'Type the word you hear.',
    promptZhHint: '听音拼写',
    answer: { acceptedTexts: ['goodbye'] },
    explanationEn: '', explanationZh: '',
    xpReward: 5, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'goodbye' },
    links: { contentItemCodes: ['W_GOODBYE'] }
  },
  {
    code: 'BOSS_3', lessonCode: '', orderIndex: 3, type: 'MCQ',
    promptEn: 'Fill in: I ___ a pen.',
    promptZhHint: '选择正确单词',
    options: [
      { key: 'A', textEn: 'have', textZh: '有' },
      { key: 'B', textEn: 'am', textZh: '是' },
      { key: 'C', textEn: 'is', textZh: '是' }
    ],
    answer: { correctOptionKey: 'A' },
    explanationEn: '', explanationZh: '',
    xpReward: 5, goldReward: 0,
    tts: { enabled: true, ttsTextEn: 'I have a pen.' },
    links: { contentItemCodes: ['W_HAVE'] }
  }
]

export const mockSkillNodes: SkillNode[] = [
  { code: 'SK_GRAMMAR_I_AM', branch: 'GRAMMAR', nameEn: 'I am Pattern', nameZh: 'I am 句型', descriptionEn: 'Use "I am ..." to introduce yourself.', descriptionZh: '用 I am ... 进行自我介绍。', costSkillPoints: 1, prerequisites: [], unlocked: true, available: true, x: 400, y: 100 },
  { code: 'SK_GRAMMAR_MY_NAME', branch: 'GRAMMAR', nameEn: 'My name is', nameZh: 'My name is 句型', descriptionEn: 'Use "My name is ..." to say your name.', descriptionZh: '用 My name is ... 说出你的名字。', costSkillPoints: 1, prerequisites: ['SK_GRAMMAR_I_AM'], unlocked: true, available: true, x: 300, y: 220 },
  { code: 'SK_GRAMMAR_I_HAVE', branch: 'GRAMMAR', nameEn: 'I have Pattern', nameZh: 'I have 句型', descriptionEn: 'Use "I have ..." to say what you own.', descriptionZh: '用 I have ... 描述你拥有的东西。', costSkillPoints: 1, prerequisites: ['SK_GRAMMAR_I_AM'], unlocked: false, available: true, x: 500, y: 220 },
  { code: 'SK_VOCAB_GREETINGS', branch: 'VOCAB', nameEn: 'Greetings', nameZh: '问候词汇', descriptionEn: 'Master greeting words.', descriptionZh: '掌握问候相关词汇。', costSkillPoints: 1, prerequisites: [], unlocked: true, available: true, x: 200, y: 100 },
  { code: 'SK_VOCAB_NUMBERS', branch: 'VOCAB', nameEn: 'Numbers 1-10', nameZh: '数字 1-10', descriptionEn: 'Master numbers one to ten.', descriptionZh: '掌握数字一到十。', costSkillPoints: 2, prerequisites: ['SK_VOCAB_GREETINGS'], unlocked: false, available: true, x: 150, y: 250 },
  { code: 'SK_VOCAB_OBJECTS', branch: 'VOCAB', nameEn: 'Common Objects', nameZh: '常见物品', descriptionEn: 'Learn pen, book, phone...', descriptionZh: '学习 pen, book, phone...', costSkillPoints: 2, prerequisites: ['SK_VOCAB_GREETINGS'], unlocked: false, available: false, x: 250, y: 350 },
  { code: 'SK_GRAMMAR_THIS_THAT', branch: 'GRAMMAR', nameEn: 'This / That', nameZh: 'this/that 指示', descriptionEn: 'Use this/that to point at things.', descriptionZh: '用 this/that 指代事物。', costSkillPoints: 2, prerequisites: ['SK_GRAMMAR_I_HAVE'], unlocked: false, available: false, x: 550, y: 350 },
  { code: 'SK_LISTENING_BASIC', branch: 'LISTENING', nameEn: 'Basic Listening', nameZh: '基础听力', descriptionEn: 'Recognize spoken words.', descriptionZh: '识别口语单词。', costSkillPoints: 1, prerequisites: [], unlocked: false, available: true, x: 600, y: 100 },
]

export const mockCefrExams: CefrExam[] = [
  { cefrLevel: 'PRE_A1', title: 'Pre-A1 入门测验', totalQuestions: 15, passRate: 0.7, timeLimitPerQuestion: 30, unlocked: true, passed: false, bestScore: undefined, attempts: 0, dailyAttemptsLeft: 3 },
  { cefrLevel: 'A1', title: 'A1 基础测验', totalQuestions: 15, passRate: 0.7, timeLimitPerQuestion: 25, unlocked: false, passed: false, bestScore: undefined, attempts: 0, dailyAttemptsLeft: 3 },
  { cefrLevel: 'A2', title: 'A2 进阶测验', totalQuestions: 20, passRate: 0.75, timeLimitPerQuestion: 20, unlocked: false, passed: false, bestScore: undefined, attempts: 0, dailyAttemptsLeft: 3 },
  { cefrLevel: 'B1', title: 'B1 中级测验', totalQuestions: 25, passRate: 0.8, timeLimitPerQuestion: 20, unlocked: false, passed: false, bestScore: undefined, attempts: 0, dailyAttemptsLeft: 3 },
]

function examTask(code: string, type: Task['type'], promptEn: string, promptZhHint: string, options: TaskOption[], correctKey: string, cefrLevel: Task['cefrLevel']): Task {
  return {
    code, lessonCode: 'exam', orderIndex: 0, type, cefrLevel,
    promptEn, promptZhHint, options,
    answer: { correctOptionKey: correctKey },
    explanationEn: '', explanationZh: '',
    xpReward: 5, goldReward: 2,
    tts: { enabled: false, ttsTextEn: promptEn },
    links: { contentItemCodes: [] },
  }
}

export const mockExamTasks: Task[] = [
  // PRE_A1 level (15 tasks)
  examTask('exam-1', 'MCQ', 'apple', '苹果', [{ key: 'A', textEn: 'apple', textZh: '苹果' }, { key: 'B', textEn: 'banana', textZh: '香蕉' }, { key: 'C', textEn: 'grape', textZh: '葡萄' }, { key: 'D', textEn: 'pear', textZh: '梨' }], 'A', 'PRE_A1'),
  examTask('exam-2', 'MCQ_REVERSE', 'cat', '猫', [{ key: 'A', textEn: 'cat', textZh: '猫' }, { key: 'B', textEn: 'dog', textZh: '狗' }, { key: 'C', textEn: 'bird', textZh: '鸟' }, { key: 'D', textEn: 'fish', textZh: '鱼' }], 'A', 'PRE_A1'),
  examTask('exam-3', 'FILL_BLANK', 'I ___ a student.', '我是一名学生。', [{ key: 'A', textEn: 'am', textZh: 'am' }, { key: 'B', textEn: 'is', textZh: 'is' }, { key: 'C', textEn: 'are', textZh: 'are' }], 'A', 'PRE_A1'),
  examTask('exam-4', 'LISTEN_PICK', 'hello', '你好', [{ key: 'A', textEn: 'hello', textZh: '你好' }, { key: 'B', textEn: 'help', textZh: '帮助' }, { key: 'C', textEn: 'yellow', textZh: '黄色' }, { key: 'D', textEn: 'jello', textZh: '果冻' }], 'A', 'PRE_A1'),
  examTask('exam-5', 'SPELLING', 'book', '书', [], 'A', 'PRE_A1'),
  examTask('exam-6', 'MCQ', 'red', '红色', [{ key: 'A', textEn: 'red', textZh: '红色' }, { key: 'B', textEn: 'blue', textZh: '蓝色' }, { key: 'C', textEn: 'green', textZh: '绿色' }, { key: 'D', textEn: 'yellow', textZh: '黄色' }], 'A', 'PRE_A1'),
  examTask('exam-7', 'WORD_ORDER', 'I like cats', '我喜欢猫', [{ key: 'A', textEn: 'I', textZh: 'I' }, { key: 'B', textEn: 'like', textZh: 'like' }, { key: 'C', textEn: 'cats', textZh: 'cats' }], 'A', 'PRE_A1'),
  examTask('exam-8', 'MCQ_REVERSE', 'water', '水', [{ key: 'A', textEn: 'water', textZh: '水' }, { key: 'B', textEn: 'juice', textZh: '果汁' }, { key: 'C', textEn: 'milk', textZh: '牛奶' }, { key: 'D', textEn: 'tea', textZh: '茶' }], 'A', 'PRE_A1'),
  examTask('exam-9', 'FILL_BLANK', 'She ___ happy.', '她很开心。', [{ key: 'A', textEn: 'is', textZh: 'is' }, { key: 'B', textEn: 'am', textZh: 'am' }, { key: 'C', textEn: 'are', textZh: 'are' }], 'A', 'PRE_A1'),
  examTask('exam-10', 'MCQ', 'dog', '狗', [{ key: 'A', textEn: 'dog', textZh: '狗' }, { key: 'B', textEn: 'cat', textZh: '猫' }, { key: 'C', textEn: 'pig', textZh: '猪' }, { key: 'D', textEn: 'cow', textZh: '牛' }], 'A', 'PRE_A1'),
  examTask('exam-11', 'MCQ', 'good morning', '早上好', [{ key: 'A', textEn: 'Good morning!', textZh: '早上好！' }, { key: 'B', textEn: 'Good night!', textZh: '晚安！' }, { key: 'C', textEn: 'Goodbye!', textZh: '再见！' }], 'A', 'PRE_A1'),
  examTask('exam-12', 'MCQ_REVERSE', 'teacher', '老师', [{ key: 'A', textEn: 'teacher', textZh: '老师' }, { key: 'B', textEn: 'student', textZh: '学生' }, { key: 'C', textEn: 'boy', textZh: '男孩' }], 'A', 'PRE_A1'),
  examTask('exam-13', 'FILL_BLANK', 'I ___ a pen.', '我有一支笔。', [{ key: 'A', textEn: 'have', textZh: 'have' }, { key: 'B', textEn: 'am', textZh: 'am' }, { key: 'C', textEn: 'is', textZh: 'is' }], 'A', 'PRE_A1'),
  examTask('exam-14', 'MCQ', '___ is your name?', '你叫什么名字？', [{ key: 'A', textEn: 'What', textZh: '什么' }, { key: 'B', textEn: 'Where', textZh: '哪里' }, { key: 'C', textEn: 'Who', textZh: '谁' }], 'A', 'PRE_A1'),
  examTask('exam-15', 'LISTEN_PICK', 'thank you', '谢谢你', [{ key: 'A', textEn: 'thank you', textZh: '谢谢你' }, { key: 'B', textEn: 'sorry', textZh: '对不起' }, { key: 'C', textEn: 'please', textZh: '请' }], 'A', 'PRE_A1'),
  // A1 level (15 tasks)
  examTask('exam-16', 'MCQ', 'How old are you?', '你几岁了？', [{ key: 'A', textEn: 'I am ten.', textZh: '我十岁。' }, { key: 'B', textEn: 'I am fine.', textZh: '我很好。' }, { key: 'C', textEn: 'I am Tom.', textZh: '我是汤姆。' }], 'A', 'A1'),
  examTask('exam-17', 'FILL_BLANK', 'There ___ two cats.', '有两只猫。', [{ key: 'A', textEn: 'are', textZh: 'are' }, { key: 'B', textEn: 'is', textZh: 'is' }, { key: 'C', textEn: 'am', textZh: 'am' }], 'A', 'A1'),
  examTask('exam-18', 'MCQ_REVERSE', 'breakfast', '早餐', [{ key: 'A', textEn: 'breakfast', textZh: '早餐' }, { key: 'B', textEn: 'lunch', textZh: '午餐' }, { key: 'C', textEn: 'dinner', textZh: '晚餐' }, { key: 'D', textEn: 'snack', textZh: '零食' }], 'A', 'A1'),
  examTask('exam-19', 'MCQ', 'He ___ to school every day.', '他每天去上学。', [{ key: 'A', textEn: 'goes', textZh: 'goes' }, { key: 'B', textEn: 'go', textZh: 'go' }, { key: 'C', textEn: 'going', textZh: 'going' }], 'A', 'A1'),
  examTask('exam-20', 'FILL_BLANK', 'This is ___ book. (my)', '这是我的书。', [{ key: 'A', textEn: 'my', textZh: 'my' }, { key: 'B', textEn: 'me', textZh: 'me' }, { key: 'C', textEn: 'I', textZh: 'I' }], 'A', 'A1'),
  examTask('exam-21', 'MCQ', 'Where is the cat?', '猫在哪里？', [{ key: 'A', textEn: 'On the table.', textZh: '在桌子上。' }, { key: 'B', textEn: 'It is white.', textZh: '它是白色的。' }, { key: 'C', textEn: 'Yes, it is.', textZh: '是的。' }], 'A', 'A1'),
  examTask('exam-22', 'LISTEN_PICK', 'Monday', '星期一', [{ key: 'A', textEn: 'Monday', textZh: '星期一' }, { key: 'B', textEn: 'Tuesday', textZh: '星期二' }, { key: 'C', textEn: 'Sunday', textZh: '星期日' }], 'A', 'A1'),
  examTask('exam-23', 'MCQ_REVERSE', 'umbrella', '雨伞', [{ key: 'A', textEn: 'umbrella', textZh: '雨伞' }, { key: 'B', textEn: 'table', textZh: '桌子' }, { key: 'C', textEn: 'window', textZh: '窗户' }], 'A', 'A1'),
  examTask('exam-24', 'FILL_BLANK', 'I ___ like rice.', '我不喜欢米饭。', [{ key: 'A', textEn: "don't", textZh: "don't" }, { key: 'B', textEn: 'not', textZh: 'not' }, { key: 'C', textEn: "doesn't", textZh: "doesn't" }], 'A', 'A1'),
  examTask('exam-25', 'MCQ', 'Can you swim?', '你会游泳吗？', [{ key: 'A', textEn: 'Yes, I can.', textZh: '是的，我会。' }, { key: 'B', textEn: 'Yes, I am.', textZh: '是的，我是。' }, { key: 'C', textEn: 'Yes, I do.', textZh: '是的。' }], 'A', 'A1'),
  examTask('exam-26', 'SPELLING', 'teacher', '老师', [], 'A', 'A1'),
  examTask('exam-27', 'MCQ', 'She ___ a new bag.', '她有一个新包。', [{ key: 'A', textEn: 'has', textZh: 'has' }, { key: 'B', textEn: 'have', textZh: 'have' }, { key: 'C', textEn: 'having', textZh: 'having' }], 'A', 'A1'),
  examTask('exam-28', 'MCQ_REVERSE', 'kitchen', '厨房', [{ key: 'A', textEn: 'kitchen', textZh: '厨房' }, { key: 'B', textEn: 'bedroom', textZh: '卧室' }, { key: 'C', textEn: 'bathroom', textZh: '浴室' }], 'A', 'A1'),
  examTask('exam-29', 'FILL_BLANK', 'We ___ playing football.', '我们正在踢足球。', [{ key: 'A', textEn: 'are', textZh: 'are' }, { key: 'B', textEn: 'is', textZh: 'is' }, { key: 'C', textEn: 'am', textZh: 'am' }], 'A', 'A1'),
  examTask('exam-30', 'MCQ', 'What time is it?', '现在几点？', [{ key: 'A', textEn: 'It is three o\'clock.', textZh: '三点钟。' }, { key: 'B', textEn: 'It is Monday.', textZh: '星期一。' }, { key: 'C', textEn: 'It is sunny.', textZh: '晴天。' }], 'A', 'A1'),
]
