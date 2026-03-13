import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 成就系统定义
export interface AchievementDef {
  code: string
  nameZh: string
  nameEn: string
  descZh: string
  descEn: string
  icon: string           // emoji icon
  category: 'learning' | 'streak' | 'combat' | 'collection' | 'mastery'
  condition: {
    type: 'words_learned' | 'streak_days' | 'boss_defeated' | 'tasks_completed' | 'perfect_combo' | 'exam_passed' | 'mistakes_reviewed'
    threshold: number
  }
  xpReward: number
  coinReward: number
}

export interface AchievementProgress {
  code: string
  unlocked: boolean
  unlockedAt: string | null
  currentValue: number
  notified: boolean      // 是否已弹窗通知
}

// 预定义成就列表
export const ACHIEVEMENT_DEFS: AchievementDef[] = [
  // 学习类
  { code: 'first_word', nameZh: '初识英语', nameEn: 'First Word', descZh: '学习第一个单词', descEn: 'Learn your first word', icon: '📖', category: 'learning', condition: { type: 'words_learned', threshold: 1 }, xpReward: 10, coinReward: 5 },
  { code: 'vocab_10', nameZh: '单词新手', nameEn: 'Word Beginner', descZh: '学习10个单词', descEn: 'Learn 10 words', icon: '📚', category: 'learning', condition: { type: 'words_learned', threshold: 10 }, xpReward: 30, coinReward: 15 },
  { code: 'vocab_25', nameZh: '词汇达人', nameEn: 'Word Master', descZh: '学习25个单词', descEn: 'Learn 25 words', icon: '🎓', category: 'learning', condition: { type: 'words_learned', threshold: 25 }, xpReward: 50, coinReward: 25 },
  { code: 'vocab_50', nameZh: '词汇学者', nameEn: 'Word Scholar', descZh: '学习50个单词', descEn: 'Learn 50 words', icon: '🏆', category: 'learning', condition: { type: 'words_learned', threshold: 50 }, xpReward: 100, coinReward: 50 },
  // 连续学习类
  { code: 'streak_3', nameZh: '初露锋芒', nameEn: '3-Day Streak', descZh: '连续学习3天', descEn: 'Study for 3 consecutive days', icon: '🔥', category: 'streak', condition: { type: 'streak_days', threshold: 3 }, xpReward: 20, coinReward: 10 },
  { code: 'streak_7', nameZh: '一周坚持', nameEn: '7-Day Streak', descZh: '连续学习7天', descEn: 'Study for 7 consecutive days', icon: '💪', category: 'streak', condition: { type: 'streak_days', threshold: 7 }, xpReward: 50, coinReward: 30 },
  { code: 'streak_30', nameZh: '月度勇士', nameEn: '30-Day Streak', descZh: '连续学习30天', descEn: 'Study for 30 consecutive days', icon: '⭐', category: 'streak', condition: { type: 'streak_days', threshold: 30 }, xpReward: 200, coinReward: 100 },
  // 战斗类
  { code: 'first_boss', nameZh: '首次击败', nameEn: 'First Boss', descZh: '击败第一个Boss', descEn: 'Defeat your first boss', icon: '⚔️', category: 'combat', condition: { type: 'boss_defeated', threshold: 1 }, xpReward: 50, coinReward: 30 },
  { code: 'combo_5', nameZh: '连击新秀', nameEn: 'Combo Starter', descZh: '达成5连击', descEn: 'Achieve a 5-combo streak', icon: '💥', category: 'combat', condition: { type: 'perfect_combo', threshold: 5 }, xpReward: 30, coinReward: 15 },
  { code: 'combo_10', nameZh: '连击大师', nameEn: 'Combo Master', descZh: '达成10连击', descEn: 'Achieve a 10-combo streak', icon: '🌟', category: 'combat', condition: { type: 'perfect_combo', threshold: 10 }, xpReward: 80, coinReward: 40 },
  // 任务类
  { code: 'tasks_50', nameZh: '任务初级', nameEn: 'Task Runner', descZh: '完成50个任务', descEn: 'Complete 50 tasks', icon: '✅', category: 'collection', condition: { type: 'tasks_completed', threshold: 50 }, xpReward: 40, coinReward: 20 },
  { code: 'tasks_200', nameZh: '任务高手', nameEn: 'Task Expert', descZh: '完成200个任务', descEn: 'Complete 200 tasks', icon: '🎯', category: 'collection', condition: { type: 'tasks_completed', threshold: 200 }, xpReward: 100, coinReward: 50 },
  // 精通类
  { code: 'exam_pre_a1', nameZh: '入门通关', nameEn: 'Pre-A1 Pass', descZh: '通过Pre-A1等级考试', descEn: 'Pass the Pre-A1 exam', icon: '🏅', category: 'mastery', condition: { type: 'exam_passed', threshold: 1 }, xpReward: 100, coinReward: 50 },
  { code: 'review_10', nameZh: '错题克星', nameEn: 'Mistake Buster', descZh: '复习10道错题', descEn: 'Review 10 mistakes', icon: '🔄', category: 'mastery', condition: { type: 'mistakes_reviewed', threshold: 10 }, xpReward: 30, coinReward: 15 },
]

const STORAGE_KEY = 'eqa_achievements'

function loadProgress(): Record<string, AchievementProgress> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return {}
}

export const useAchievementStore = defineStore('achievements', () => {
  const progress = ref<Record<string, AchievementProgress>>(loadProgress())

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(progress.value))
  }

  // 确保所有成就都有进度条目
  function ensureInitialized() {
    for (const def of ACHIEVEMENT_DEFS) {
      if (!progress.value[def.code]) {
        progress.value[def.code] = {
          code: def.code,
          unlocked: false,
          unlockedAt: null,
          currentValue: 0,
          notified: false,
        }
      }
    }
  }

  ensureInitialized()

  const unlockedCount = computed(() =>
    Object.values(progress.value).filter(p => p.unlocked).length
  )

  const totalCount = computed(() => ACHIEVEMENT_DEFS.length)

  // 获取待通知的新成就
  const pendingNotifications = computed(() =>
    ACHIEVEMENT_DEFS.filter(def => {
      const p = progress.value[def.code]
      return p && p.unlocked && !p.notified
    })
  )

  // 更新某个条件类型的进度值，自动检查是否解锁
  function updateProgress(conditionType: AchievementDef['condition']['type'], value: number): AchievementDef[] {
    const newlyUnlocked: AchievementDef[] = []
    for (const def of ACHIEVEMENT_DEFS) {
      if (def.condition.type !== conditionType) continue
      const p = progress.value[def.code]
      if (!p || p.unlocked) continue
      p.currentValue = value
      if (value >= def.condition.threshold) {
        p.unlocked = true
        p.unlockedAt = new Date().toISOString()
        newlyUnlocked.push(def)
      }
    }
    if (newlyUnlocked.length > 0) persist()
    return newlyUnlocked
  }

  function markNotified(code: string) {
    const p = progress.value[code]
    if (p) {
      p.notified = true
      persist()
    }
  }

  // 获取所有成就及其进度
  function getAllAchievements(): Array<AchievementDef & { progress: AchievementProgress }> {
    return ACHIEVEMENT_DEFS.map(def => ({
      ...def,
      progress: progress.value[def.code] || { code: def.code, unlocked: false, unlockedAt: null, currentValue: 0, notified: false },
    }))
  }

  return {
    progress,
    unlockedCount,
    totalCount,
    pendingNotifications,
    updateProgress,
    markNotified,
    getAllAchievements,
  }
})
