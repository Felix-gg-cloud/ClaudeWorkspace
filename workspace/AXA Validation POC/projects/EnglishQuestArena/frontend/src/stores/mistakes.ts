import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 错题收集系统：记录错误答题供回顾
export interface MistakeRecord {
  id: string
  taskCode: string
  taskType: string
  wordCode: string        // 关联的单词
  promptEn: string
  promptZhHint: string
  userAnswer: string       // 用户选的答案
  correctAnswer: string    // 正确答案
  timestamp: string        // ISO datetime
  reviewed: boolean        // 是否已复习
  reviewCount: number
}

function storageKey(userId: number): string {
  return `eqa_mistakes_${userId}`
}

function loadMistakes(userId: number): MistakeRecord[] {
  try {
    const raw = localStorage.getItem(storageKey(userId))
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return []
}

export const useMistakeStore = defineStore('mistakes', () => {
  let currentUserId = 0
  const mistakes = ref<MistakeRecord[]>([])

  /** 登录后调用，加载该用户的数据 */
  function reload(userId: number) {
    currentUserId = userId
    mistakes.value = loadMistakes(userId)
  }

  function persist() {
    if (!currentUserId) return
    localStorage.setItem(storageKey(currentUserId), JSON.stringify(mistakes.value))
  }

  const unreviewedCount = computed(() =>
    mistakes.value.filter(m => !m.reviewed).length
  )

  const totalCount = computed(() => mistakes.value.length)

  // 按单词分组统计错误次数
  const mistakesByWord = computed(() => {
    const map: Record<string, number> = {}
    for (const m of mistakes.value) {
      map[m.wordCode] = (map[m.wordCode] || 0) + 1
    }
    return map
  })

  // 获取最高频错词 top N
  function getTopMistakes(n = 10): Array<{ wordCode: string; count: number }> {
    const entries = Object.entries(mistakesByWord.value)
    entries.sort((a, b) => b[1] - a[1])
    return entries.slice(0, n).map(([wordCode, count]) => ({ wordCode, count }))
  }

  function addMistake(record: Omit<MistakeRecord, 'id' | 'timestamp' | 'reviewed' | 'reviewCount'>) {
    const id = `mistake_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
    mistakes.value.push({
      ...record,
      id,
      timestamp: new Date().toISOString(),
      reviewed: false,
      reviewCount: 0,
    })
    persist()
  }

  function markReviewed(id: string) {
    const m = mistakes.value.find(r => r.id === id)
    if (m) {
      m.reviewed = true
      m.reviewCount++
      persist()
    }
  }

  // 获取未复习错题用于重新练习
  function getUnreviewedMistakes(limit = 20): MistakeRecord[] {
    return mistakes.value
      .filter(m => !m.reviewed)
      .slice(0, limit)
  }

  return {
    mistakes,
    unreviewedCount,
    totalCount,
    mistakesByWord,
    getTopMistakes,
    addMistake,
    markReviewed,
    getUnreviewedMistakes,
    reload,
  }
})
