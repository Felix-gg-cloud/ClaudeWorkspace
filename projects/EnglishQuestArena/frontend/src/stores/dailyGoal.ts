import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 每日目标系统
export interface DailyRecord {
  date: string          // YYYY-MM-DD
  xpEarned: number
  tasksCompleted: number
  wordsLearned: number
  timeSpentMinutes: number
  goalMet: boolean
}

export interface DailyGoalConfig {
  targetXp: number
  targetTasks: number
  targetMinutes: number
}

interface StoredData {
  config: DailyGoalConfig
  records: Record<string, DailyRecord>
}

function storageKey(userId: number): string {
  return `eqa_daily_goal_${userId}`
}

function loadData(userId: number): StoredData {
  try {
    const raw = localStorage.getItem(storageKey(userId))
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return emptyData()
}

function emptyData(): StoredData {
  return {
    config: { targetXp: 50, targetTasks: 10, targetMinutes: 15 },
    records: {},
  }
}

function todayKey(): string {
  return new Date().toISOString().slice(0, 10)
}

export const useDailyGoalStore = defineStore('dailyGoal', () => {
  let currentUserId = 0
  const config = ref<DailyGoalConfig>({ targetXp: 50, targetTasks: 10, targetMinutes: 15 })
  const records = ref<Record<string, DailyRecord>>({})

  /** 登录后调用，加载该用户的数据 */
  function reload(userId: number) {
    currentUserId = userId
    const stored = loadData(userId)
    config.value = stored.config
    records.value = stored.records
  }

  function persist() {
    if (!currentUserId) return
    const data: StoredData = {
      config: config.value,
      records: records.value,
    }
    localStorage.setItem(storageKey(currentUserId), JSON.stringify(data))
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

  function addXp(amount: number) {
    const rec = ensureToday()
    rec.xpEarned += amount
    checkGoal(rec)
    persist()
  }

  function addTaskCompleted(count = 1) {
    const rec = ensureToday()
    rec.tasksCompleted += count
    persist()
  }

  function addWordLearned(count = 1) {
    const rec = ensureToday()
    rec.wordsLearned += count
    persist()
  }

  function addTimeSpent(minutes: number) {
    const rec = ensureToday()
    rec.timeSpentMinutes += minutes
    persist()
  }

  function checkGoal(rec: DailyRecord) {
    if (!rec.goalMet && rec.xpEarned >= config.value.targetXp) {
      rec.goalMet = true
    }
  }

  function updateConfig(newConfig: Partial<DailyGoalConfig>) {
    Object.assign(config.value, newConfig)
    persist()
  }

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
    reload,
  }
})
