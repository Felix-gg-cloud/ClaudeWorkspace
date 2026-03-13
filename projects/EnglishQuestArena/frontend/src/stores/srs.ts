import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// SRS（间隔重复）系统：追踪每个单词的复习间隔
export interface SrsCard {
  wordCode: string
  interval: number      // 当前间隔（天）
  easeFactor: number    // 难度因子 (≥1.3)
  nextReviewAt: string  // ISO date
  reviewCount: number
  correctStreak: number
  lastReviewAt: string | null
}

const STORAGE_KEY = 'eqa_srs'

function loadCards(): Record<string, SrsCard> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return {}
}

export const useSrsStore = defineStore('srs', () => {
  const cards = ref<Record<string, SrsCard>>(loadCards())

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cards.value))
  }

  // 今天需要复习的词
  const dueCards = computed(() => {
    const now = new Date().toISOString().slice(0, 10) // YYYY-MM-DD
    return Object.values(cards.value).filter(c => c.nextReviewAt <= now)
  })

  const totalCards = computed(() => Object.keys(cards.value).length)

  // SM-2 算法简化版
  function reviewCard(wordCode: string, quality: number /* 0-5 */) {
    let card = cards.value[wordCode]
    if (!card) {
      card = {
        wordCode,
        interval: 1,
        easeFactor: 2.5,
        nextReviewAt: new Date().toISOString().slice(0, 10),
        reviewCount: 0,
        correctStreak: 0,
        lastReviewAt: null,
      }
      cards.value[wordCode] = card
    }

    card.reviewCount++
    card.lastReviewAt = new Date().toISOString().slice(0, 10)

    if (quality >= 3) {
      // 答对
      card.correctStreak++
      if (card.correctStreak === 1) {
        card.interval = 1
      } else if (card.correctStreak === 2) {
        card.interval = 6
      } else {
        card.interval = Math.round(card.interval * card.easeFactor)
      }
    } else {
      // 答错，重置
      card.correctStreak = 0
      card.interval = 1
    }

    // 更新 easeFactor (SM-2)
    card.easeFactor = Math.max(
      1.3,
      card.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
    )

    // 计算下次复习日期
    const next = new Date()
    next.setDate(next.getDate() + card.interval)
    card.nextReviewAt = next.toISOString().slice(0, 10)

    persist()
  }

  // 添加新词到 SRS（首次学习时调用）
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
    persist()
  }

  return {
    cards,
    dueCards,
    totalCards,
    reviewCard,
    addWord,
  }
})
