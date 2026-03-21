import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface DailyRecord {
  date: string           // YYYY-MM-DD
  tasksCompleted: number
  correctCount: number
  wrongCount: number
  wordsLearned: number
  xpEarned: number
  coinsEarned: number
}

function storageKey(userId: number): string {
  return `eqa_daily_stats_${userId}`
}

function loadRecords(userId: number): DailyRecord[] {
  try {
    const raw = localStorage.getItem(storageKey(userId))
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return []
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

export const useDailyStatsStore = defineStore('dailyStats', () => {
  let currentUserId = 0
  const records = ref<DailyRecord[]>([])

  function reload(userId: number) {
    currentUserId = userId
    records.value = loadRecords(userId)
  }

  function persist() {
    if (!currentUserId) return
    localStorage.setItem(storageKey(currentUserId), JSON.stringify(records.value))
  }

  function getOrCreateToday(): DailyRecord {
    const d = today()
    let rec = records.value.find(r => r.date === d)
    if (!rec) {
      rec = { date: d, tasksCompleted: 0, correctCount: 0, wrongCount: 0, wordsLearned: 0, xpEarned: 0, coinsEarned: 0 }
      records.value.push(rec)
    }
    return rec
  }

  function recordCorrect(xp: number, coins: number) {
    const rec = getOrCreateToday()
    rec.tasksCompleted++
    rec.correctCount++
    rec.xpEarned += xp
    rec.coinsEarned += coins
    persist()
  }

  function recordWrong() {
    const rec = getOrCreateToday()
    rec.tasksCompleted++
    rec.wrongCount++
    persist()
  }

  function recordWordLearned() {
    const rec = getOrCreateToday()
    rec.wordsLearned++
    persist()
  }

  /** 最近 N 天的记录（含空天补零） */
  function getRecentDays(n: number): DailyRecord[] {
    const result: DailyRecord[] = []
    const now = new Date()
    for (let i = n - 1; i >= 0; i--) {
      const d = new Date(now)
      d.setDate(d.getDate() - i)
      const dateStr = d.toISOString().slice(0, 10)
      const existing = records.value.find(r => r.date === dateStr)
      result.push(existing ?? { date: dateStr, tasksCompleted: 0, correctCount: 0, wrongCount: 0, wordsLearned: 0, xpEarned: 0, coinsEarned: 0 })
    }
    return result
  }

  /** 总正确率 */
  const totalAccuracy = computed(() => {
    const total = records.value.reduce((s, r) => s + r.correctCount + r.wrongCount, 0)
    if (total === 0) return 0
    const correct = records.value.reduce((s, r) => s + r.correctCount, 0)
    return Math.round((correct / total) * 100)
  })

  /** 学习天数 */
  const studyDays = computed(() => records.value.filter(r => r.tasksCompleted > 0).length)

  return {
    records,
    reload,
    recordCorrect,
    recordWrong,
    recordWordLearned,
    getRecentDays,
    totalAccuracy,
    studyDays,
  }
})
