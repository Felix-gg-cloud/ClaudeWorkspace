import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'

// SRS（间隔重复）系统：对接后端 SM-2 算法
export interface SrsCard {
  wordCode: string
  interval: number
  easeFactor: number
  nextReviewAt: string // YYYY-MM-DD
  reviewCount: number
  correctStreak: number
  lastReviewAt: string | null
}

export const useSrsStore = defineStore('srs', () => {
  const cards = ref<Record<string, SrsCard>>({})

  const dueCards = computed(() => {
    const now = new Date().toISOString().slice(0, 10)
    return Object.values(cards.value).filter(c => c.nextReviewAt <= now)
  })

  const totalCards = computed(() => Object.keys(cards.value).length)

  /** 从后端加载全部 SRS 记录 */
  async function loadAll() {
    try {
      const { data } = await http.get('/srs/all')
      const map: Record<string, SrsCard> = {}
      for (const r of data as Record<string, unknown>[]) {
        const code = r.contentItemCode as string
        map[code] = {
          wordCode: code,
          interval: (r.intervalDays as number) || 1,
          easeFactor: (r.easeFactor as number) || 2.5,
          nextReviewAt: ((r.nextReviewDate as string) || '').slice(0, 10),
          reviewCount: (r.totalReviews as number) || 0,
          correctStreak: (r.repetitions as number) || 0,
          lastReviewAt: null,
        }
      }
      cards.value = map
    } catch { /* ignore */ }
  }

  /** 提交复习结果 */
  async function reviewCard(wordCode: string, quality: number) {
    try {
      const { data } = await http.post('/srs/review', { contentItemCode: wordCode, quality })
      const r = data as Record<string, unknown>
      cards.value[wordCode] = {
        wordCode,
        interval: (r.intervalDays as number) || 1,
        easeFactor: (r.easeFactor as number) || 2.5,
        nextReviewAt: ((r.nextReviewDate as string) || '').slice(0, 10),
        reviewCount: (r.totalReviews as number) || 0,
        correctStreak: (r.repetitions as number) || 0,
        lastReviewAt: new Date().toISOString().slice(0, 10),
      }
    } catch {
      // 离线回退：本地 SM-2
      let card = cards.value[wordCode]
      if (!card) {
        card = { wordCode, interval: 1, easeFactor: 2.5, nextReviewAt: new Date().toISOString().slice(0, 10), reviewCount: 0, correctStreak: 0, lastReviewAt: null }
        cards.value[wordCode] = card
      }
      card.reviewCount++
      card.lastReviewAt = new Date().toISOString().slice(0, 10)
      if (quality >= 3) {
        card.correctStreak++
        card.interval = card.correctStreak <= 1 ? 1 : card.correctStreak === 2 ? 6 : Math.round(card.interval * card.easeFactor)
      } else {
        card.correctStreak = 0
        card.interval = 1
      }
      card.easeFactor = Math.max(1.3, card.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
      const next = new Date()
      next.setDate(next.getDate() + card.interval)
      card.nextReviewAt = next.toISOString().slice(0, 10)
    }
  }

  function addWord(wordCode: string) {
    if (cards.value[wordCode]) return
    cards.value[wordCode] = {
      wordCode,
      interval: 1,
      easeFactor: 2.5,
      nextReviewAt: new Date().toISOString().slice(0, 10),
      reviewCount: 0,
      correctStreak: 0,
      lastReviewAt: null,
    }
  }

  return {
    cards,
    dueCards,
    totalCards,
    loadAll,
    reviewCard,
    addWord,
  }
})
