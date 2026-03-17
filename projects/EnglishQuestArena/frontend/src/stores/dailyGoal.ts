import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'
import { useUserStore } from '@/stores/user'

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
  targetXp: number
  targetTasks: number
  targetMinutes: number
}

interface StoredData {
  config: DailyGoalConfig
  records: Record<string, DailyRecord>
  currentStreak: number
  bestStreak: number
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
    currentStreak: 0,
    bestStreak: 0,
  }
}

function todayKey(): string {
  return new Date().toISOString().slice(0, 10)
}

export const useDailyGoalStore = defineStore('dailyGoal', () => {
  let currentUserId = 0
  const config = ref<DailyGoalConfig>({ targetXp: 50, targetTasks: 10, targetMinutes: 15 })
  const records = ref<Record<string, DailyRecord>>({})
  const currentStreak = ref(0)
  const bestStreak = ref(0)

  /** 登录后调用，加载该用户的数据 */
  function reload(userId: number) {
    currentUserId = userId
    const stored = loadData(userId)
    config.value = stored.config
    records.value = stored.records
    currentStreak.value = stored.currentStreak
    bestStreak.value = stored.bestStreak
  }

  function persist() {
    if (!currentUserId) return
    const data: StoredData = {
      config: config.value,
      records: records.value,
      currentStreak: currentStreak.value,
      bestStreak: bestStreak.value,
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
      currentStreak.value++
      if (currentStreak.value > bestStreak.value) {
        bestStreak.value = currentStreak.value
      }
    }
  }

  /** 签到（调用后端） */
  async function checkin() {
    try {
      const { data } = await http.post('/checkin')
      const d = data as Record<string, unknown>
      currentStreak.value = (d.streak as number) || 0
      const userStore = useUserStore()
      userStore.syncFromServer({ totalXp: d.totalXp as number, coins: d.coins as number })
      return d
    } catch {
      return null
    }
  }

  /** 获取签到历史 */
  async function loadCheckinHistory() {
    try {
      const { data } = await http.get('/checkin/history')
      const list = data as Record<string, unknown>[]
      if (list.length > 0) {
        currentStreak.value = (list[0].streak as number) || 0
      }
      return list
    } catch {
      return []
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
    checkin,
    loadCheckinHistory,
    reload,
  }
})
