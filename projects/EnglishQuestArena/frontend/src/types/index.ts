// Types for EnglishQuestArena

export type CefrLevel = 'PRE_A1' | 'A1' | 'A2' | 'B1'

export interface User {
  id: number
  username: string
  displayName: string
  ttsVoice: 'en-US' | 'en-GB'
  cefrLevel: CefrLevel
  currentLevel: number
  totalXp: number
  xpToNextLevel: number
  coins: number
  skillPoints: number
  streak: number
  totalCheckins: number
  firstLogin: boolean
}

export interface Chapter {
  code: string
  cefrLevel: string
  titleEn: string
  titleZh: string
  orderIndex: number
  days: number
  bossDayIndex: number
}

export interface Lesson {
  code: string
  chapterCode: string
  dayIndex: number
  titleEn: string
  titleZh: string
  estimatedMinutes: number
  targetTaskCount: number
  autoDrillEnabled: boolean
  completed: boolean
  current: boolean
}

export interface TaskOption {
  key: string
  textEn: string
  textZh: string
}

export interface Task {
  code: string
  lessonCode: string
  orderIndex: number
  type: 'MCQ' | 'MCQ_REVERSE' | 'FLASHCARD' | 'SPELLING' | 'WORD_ORDER' | 'FILL_BLANK' | 'LISTEN_PICK' | 'LISTEN_FILL' | 'DIALOGUE_REVIEW' | 'SITUATION_PICK' | 'ERROR_FIX'
  cefrLevel?: 'PRE_A1' | 'A1' | 'A2' | 'B1'
  promptEn: string
  promptZhHint: string
  options?: TaskOption[]
  tokens?: string[]
  answer: Record<string, unknown>
  explanationEn: string
  explanationZh: string
  xpReward: number
  goldReward: number
  tts: { enabled: boolean; ttsTextEn: string }
  links: { contentItemCodes: string[] }
  dialogue?: {
    lines: Array<{ en: string; zh: string; ttsTextEn: string }>
  }
  followUpQuestions?: Array<{
    type: string
    promptEn: string
    promptZhHint: string
    options: TaskOption[]
    answer: { correctOptionKey: string }
  }>
}

export interface TaskProgress {
  taskKey: string
  completed: boolean
  correct: boolean
  answeredAt?: string
}

export interface TodayData {
  lesson: Lesson
  tasks: Task[]
  progress: TaskProgress[]
}

export interface CheckinRecord {
  date: string
  streak: number
  xpEarned: number
  coinsEarned: number
}

export interface BossConfig {
  code: string
  chapterCode: string
  bossHp: number
  playerHp: number
  comboThreshold: number
  comboBonusDamage: number
  bossDamage: number
  dailyRetryLimit: number
  bossName: string
  bossType: string // gatekeeper, shadow_hunter, flame_warden, etc.
  tier: number // 1-5 difficulty tier
}

export interface BossBattle {
  config: BossConfig
  tasks: Task[]
  bossHp: number
  playerHp: number
  combo: number
  currentIndex: number
  finished: boolean
  victory: boolean
}

export interface SkillNode {
  code: string
  branch: string
  nameEn: string
  nameZh: string
  descriptionEn: string
  descriptionZh: string
  costSkillPoints: number
  prerequisites: string[]
  unlocked: boolean
  available: boolean
  x: number  // position for constellation map
  y: number
}

export interface ArenaSession {
  tasks: Task[]
  progress: TaskProgress[]
  combo: number
  currentIndex: number
}

export interface CefrExam {
  cefrLevel: CefrLevel
  title: string
  totalQuestions: number
  passRate: number
  timeLimitPerQuestion?: number
  unlocked: boolean
  passed: boolean
  bestScore?: number
  attempts: number
  dailyAttemptsLeft: number
}

// ============ Chapter Progression ============

export type ChapterPhase = 'locked' | 'camp' | 'quest' | 'boss' | 'completed'

// 基础词汇条目（向后兼容）
export interface WordItem {
  code: string
  en: string
  zh: string
  phonetic: string
  sentence: string
  sentenceZh: string
}

// ContentItem: 单一数据源，所有模块（Camp/Quest/Boss/Arena）共用
export interface ContentItem extends WordItem {
  type: 'WORD' | 'PHRASE' | 'GRAMMAR'
  cefrLevel: CefrLevel
  chapterCode: string
  dayIndex: number        // 该词主要在第几天教授
  distractors: string[]   // 干扰项的 ContentItem codes
}

export interface ChapterConfig {
  code: string
  cefrLevel: CefrLevel
  titleEn: string
  titleZh: string
  orderIndex: number
  days: number
  campUnlockRate: number // 0-1, e.g. 0.8 = defeat 80% monsters to unlock quest
  words: ContentItem[]
  bossConfig: BossConfig
}

export interface ChapterProgress {
  chapterCode: string
  phase: ChapterPhase
  campDefeated: string[]  // encounter IDs defeated
  campOpened: string[]    // chest IDs opened
  questDaysCompleted: number[] // dayIndex array
  bossDefeated: boolean
}
