import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 每日目标 & 打卡系统
export interface DailyRecord {
  date: string          // YYYY-MM-DD
  xpEarned: number
  tasksCompleted: number
  wordsLearned: number
  timeSpentMinutes: number
  goalMet: boolean
}

export interface DailyGoalConfig {
  targetXp: number           // 每日目标XP
  targetTasks: number        // 每日目标任务数
  targetMinutes: number      // 每日目标学习时长
}

const STORAGE_KEY = 'eqa_daily_goal'

interface StoredData {
  config: DailyGoalConfig
  records: Record<string, DailyRecord>
  currentStreak: number
  bestStreak: number
}

function loadData(): StoredData {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return {
    config: { targetXp: 50, targetTasks: 10, targetMinutes: 15 },
    records: {},
    currentStreak: 0,
    bestStreak: 0,
  }
}

function todayKey(): string {
  return new Date().toISOString().slice(0, 10)
}

export const useDailyGoalStore = defineStore('dailyGoal', () => {
  const stored = loadData()
  const config = ref<DailyGoalConfig>(stored.config)
  const records = ref<Record<string, DailyRecord>>(stored.records)
  const currentStreak = ref(stored.currentStreak)
  const bestStreak = ref(stored.bestStreak)

  function persist() {
    const data: StoredData = {
      config: config.value,
      records: records.value,
      currentStreak: currentStreak.value,
      bestStreak: bestStreak.value,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  }

  function ensureToday(): DailyRecord {
    const key = todayKey()
    if (!records.value[key]) {
      records.value[key] = {
        date: key,
        xpEarned: 0,
        tasksCompleted: 0,
        wordsLearned: 0,
        timeSpentMinutes: 0,
        goalMet: false,
      }
    }
    return records.value[key]
  }

  const todayRecord = computed(() => {
    const key = todayKey()
    return records.value[key] || { date: key, xpEarned: 0, tasksCompleted: 0, wordsLearned: 0, timeSpentMinutes: 0, goalMet: false }
  })

  const todayXpProgress = computed(() =>
    Math.min(1, todayRecord.value.xpEarned / config.value.targetXp)
  )

  const todayTaskProgress = computed(() =>
    Math.min(1, todayRecord.value.tasksCompleted / config.value.targetTasks)
  )

  const isGoalMet = computed(() =>
    todayRecord.value.xpEarned >= config.value.targetXp
  )

  // 记录 XP 获得
  function addXp(amount: number) {
    const rec = ensureToday()
    rec.xpEarned += amount
    checkGoal(rec)
    persist()
  }

  // 记录任务完成
  function addTaskCompleted(count = 1) {
    const rec = ensureToday()
    rec.tasksCompleted += count
    persist()
  }

  // 记录新词学习
  function addWordLearned(count = 1) {
    const rec = ensureToday()
    rec.wordsLearned += count
    persist()
  }

  // 记录学习时长
  function addTimeSpent(minutes: number) {
    const rec = ensureToday()
    rec.timeSpentMinutes += minutes
    persist()
  }

  function checkGoal(rec: DailyRecord) {
    if (!rec.goalMet && rec.xpEarned >= config.value.targetXp) {
      rec.goalMet = true
      // 更新连续天数
      currentStreak.value++
      if (currentStreak.value > bestStreak.value) {
        bestStreak.value = currentStreak.value
      }
    }
  }

  // 设置每日目标
  function updateConfig(newConfig: Partial<DailyGoalConfig>) {
    Object.assign(config.value, newConfig)
    persist()
  }

  // 获取最近N天的记录
  function getRecentRecords(days = 7): DailyRecord[] {
    const result: DailyRecord[] = []
    const now = new Date()
    for (let i = 0; i < days; i++) {
      const d = new Date(now)
      d.setDate(d.getDate() - i)
      const key = d.toISOString().slice(0, 10)
      result.push(
        records.value[key] || { date: key, xpEarned: 0, tasksCompleted: 0, wordsLearned: 0, timeSpentMinutes: 0, goalMet: false }
      )
    }
    return result
  }

  // 获取总计统计
  const totalStats = computed(() => {
    const all = Object.values(records.value)
    return {
      totalXp: all.reduce((s, r) => s + r.xpEarned, 0),
      totalTasks: all.reduce((s, r) => s + r.tasksCompleted, 0),
      totalWords: all.reduce((s, r) => s + r.wordsLearned, 0),
      totalMinutes: all.reduce((s, r) => s + r.timeSpentMinutes, 0),
      totalDays: all.filter(r => r.xpEarned > 0).length,
    }
  })

  return {
    config,
    records,
    currentStreak,
    bestStreak,
    todayRecord,
    todayXpProgress,
    todayTaskProgress,
    isGoalMet,
    totalStats,
    addXp,
    addTaskCompleted,
    addWordLearned,
    addTimeSpent,
    updateConfig,
    getRecentRecords,
  }
})
