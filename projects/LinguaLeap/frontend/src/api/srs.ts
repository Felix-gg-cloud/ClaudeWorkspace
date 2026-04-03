import http from './http'

export interface SrsCard {
  cardId: number
  kpId: number
  content: string
  meaningZh: string
  phonetic: string
  bankId: number
  intervalDays: number
  correctStreak: number
  nextReviewAt: string
  reviewCount?: number
  easeFactor?: number
  status?: 'due' | 'upcoming' | 'mastered'
}

export interface SrsReviewResult {
  kpId: number
  correct: boolean
  nextReviewAt: string
  intervalDays: number
  easeFactor: number
  correctStreak: number
}

export interface SrsStats {
  total: number
  dueCount: number
  mastered: number
  learning: number
  nextDueAt?: string
}

export const srsApi = {
  getDueCards() {
    return http.get<{ data: SrsCard[] }>('/content/srs/due')
  },
  getAllCards() {
    return http.get<{ data: SrsCard[] }>('/content/srs/cards')
  },
  review(kpId: number, correct: boolean) {
    return http.post<{ data: SrsReviewResult }>('/content/srs/review', { kpId, correct })
  },
  getStats() {
    return http.get<{ data: SrsStats }>('/content/srs/stats')
  },
}
