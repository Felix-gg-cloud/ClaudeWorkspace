import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'
import { useUserStore } from '@/stores/user'

// 成就系统定义
export interface AchievementDef {
  code: string
  nameZh: string
  nameEn: string
  descZh: string
  descEn: string
  icon: string
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
  notified: boolean
}

// 前端成就定义（用于展示 icon/category/description）
export const ACHIEVEMENT_DEFS: AchievementDef[] = [
  { code: 'first_word', nameZh: '初识英语', nameEn: 'First Word', descZh: '学习第一个单词', descEn: 'Learn your first word', icon: '📖', category: 'learning', condition: { type: 'words_learned', threshold: 1 }, xpReward: 10, coinReward: 5 },
  { code: 'vocab_10', nameZh: '单词新手', nameEn: 'Word Beginner', descZh: '学习10个单词', descEn: 'Learn 10 words', icon: '📚', category: 'learning', condition: { type: 'words_learned', threshold: 10 }, xpReward: 30, coinReward: 15 },
  { code: 'vocab_25', nameZh: '词汇达人', nameEn: 'Word Master', descZh: '学习25个单词', descEn: 'Learn 25 words', icon: '🎓', category: 'learning', condition: { type: 'words_learned', threshold: 25 }, xpReward: 50, coinReward: 25 },
  { code: 'vocab_50', nameZh: '词汇学者', nameEn: 'Word Scholar', descZh: '学习50个单词', descEn: 'Learn 50 words', icon: '🏆', category: 'learning', condition: { type: 'words_learned', threshold: 50 }, xpReward: 100, coinReward: 50 },
  { code: 'streak_3', nameZh: '初露锋芒', nameEn: '3-Day Streak', descZh: '连续学习3天', descEn: 'Study for 3 consecutive days', icon: '🔥', category: 'streak', condition: { type: 'streak_days', threshold: 3 }, xpReward: 20, coinReward: 10 },
  { code: 'streak_7', nameZh: '一周坚持', nameEn: '7-Day Streak', descZh: '连续学习7天', descEn: 'Study for 7 consecutive days', icon: '💪', category: 'streak', condition: { type: 'streak_days', threshold: 7 }, xpReward: 50, coinReward: 30 },
  { code: 'streak_30', nameZh: '月度勇士', nameEn: '30-Day Streak', descZh: '连续学习30天', descEn: 'Study for 30 consecutive days', icon: '⭐', category: 'streak', condition: { type: 'streak_days', threshold: 30 }, xpReward: 200, coinReward: 100 },
  { code: 'first_boss', nameZh: '首次击败', nameEn: 'First Boss', descZh: '击败第一个Boss', descEn: 'Defeat your first boss', icon: '⚔️', category: 'combat', condition: { type: 'boss_defeated', threshold: 1 }, xpReward: 50, coinReward: 30 },
  { code: 'combo_5', nameZh: '连击新秀', nameEn: 'Combo Starter', descZh: '达成5连击', descEn: 'Achieve a 5-combo streak', icon: '💥', category: 'combat', condition: { type: 'perfect_combo', threshold: 5 }, xpReward: 30, coinReward: 15 },
  { code: 'combo_10', nameZh: '连击大师', nameEn: 'Combo Master', descZh: '达成10连击', descEn: 'Achieve a 10-combo streak', icon: '🌟', category: 'combat', condition: { type: 'perfect_combo', threshold: 10 }, xpReward: 80, coinReward: 40 },
  { code: 'tasks_50', nameZh: '任务初级', nameEn: 'Task Runner', descZh: '完成50个任务', descEn: 'Complete 50 tasks', icon: '✅', category: 'collection', condition: { type: 'tasks_completed', threshold: 50 }, xpReward: 40, coinReward: 20 },
  { code: 'tasks_200', nameZh: '任务高手', nameEn: 'Task Expert', descZh: '完成200个任务', descEn: 'Complete 200 tasks', icon: '🎯', category: 'collection', condition: { type: 'tasks_completed', threshold: 200 }, xpReward: 100, coinReward: 50 },
  { code: 'exam_pre_a1', nameZh: '入门通关', nameEn: 'Pre-A1 Pass', descZh: '通过Pre-A1等级考试', descEn: 'Pass the Pre-A1 exam', icon: '🏅', category: 'mastery', condition: { type: 'exam_passed', threshold: 1 }, xpReward: 100, coinReward: 50 },
  { code: 'review_10', nameZh: '错题克星', nameEn: 'Mistake Buster', descZh: '复习10道错题', descEn: 'Review 10 mistakes', icon: '🔄', category: 'mastery', condition: { type: 'mistakes_reviewed', threshold: 10 }, xpReward: 30, coinReward: 15 },
]

// notified 状态存 localStorage（纯展示用），按用户隔离
function notifiedKey(userId: number): string {
  return `eqa_achievement_notified_${userId}`
}
function loadNotified(userId: number): Set<string> {
  try {
    const raw = localStorage.getItem(notifiedKey(userId))
    if (raw) return new Set(JSON.parse(raw))
  } catch { /* ignore */ }
  return new Set()
}

export const useAchievementStore = defineStore('achievements', () => {
  let currentUserId = 0
  const progress = ref<Record<string, AchievementProgress>>({})
  const notifiedSet = ref<Set<string>>(new Set())

  /** 登录后调用，加载该用户的 notified 状态 */
  function reload(userId: number) {
    currentUserId = userId
    notifiedSet.value = loadNotified(userId)
  }

  const unlockedCount = computed(() =>
    Object.values(progress.value).filter(p => p.unlocked).length
  )

  const totalCount = computed(() => ACHIEVEMENT_DEFS.length)

  const pendingNotifications = computed(() =>
    ACHIEVEMENT_DEFS.filter(def => {
      const p = progress.value[def.code]
      return p && p.unlocked && !notifiedSet.value.has(def.code)
    })
  )

  /** 从后端加载成就列表 */
  async function loadAchievements() {
    try {
      const { data } = await http.get('/achievements')
      const map: Record<string, AchievementProgress> = {}
      for (const a of data as Record<string, unknown>[]) {
        const code = a.code as string
        map[code] = {
          code,
          unlocked: (a.unlocked as boolean) || false,
          unlockedAt: null,
          currentValue: (a.currentValue as number) || 0,
          notified: notifiedSet.value.has(code),
        }
      }
      progress.value = map
    } catch {
      // 离线时初始化
      for (const def of ACHIEVEMENT_DEFS) {
        if (!progress.value[def.code]) {
          progress.value[def.code] = { code: def.code, unlocked: false, unlockedAt: null, currentValue: 0, notified: false }
        }
      }
    }
  }

  /** 提交条件检查，触发解锁 */
  async function updateProgress(conditionType: AchievementDef['condition']['type'], value: number): Promise<AchievementDef[]> {
    const newlyUnlocked: AchievementDef[] = []
    try {
      const { data } = await http.post('/achievements/check', { conditionType, value })
      const d = data as Record<string, unknown>
      const unlocked = (d.newlyUnlocked as Record<string, unknown>[]) || []
      for (const u of unlocked) {
        const code = u.code as string
        const def = ACHIEVEMENT_DEFS.find(a => a.code === code)
        if (def) newlyUnlocked.push(def)
        if (progress.value[code]) {
          progress.value[code].unlocked = true
          progress.value[code].unlockedAt = new Date().toISOString()
        }
      }
      // 同步 XP/coins
      const userStore = useUserStore()
      userStore.syncFromServer({ totalXp: d.totalXp as number, coins: d.coins as number })
    } catch { /* ignore */ }
    return newlyUnlocked
  }

  function markNotified(code: string) {
    notifiedSet.value.add(code)
    const p = progress.value[code]
    if (p) p.notified = true
    if (currentUserId) localStorage.setItem(notifiedKey(currentUserId), JSON.stringify([...notifiedSet.value]))
  }

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
    loadAchievements,
    updateProgress,
    markNotified,
    getAllAchievements,
    reload,
  }
})
