import http from './http'

export interface MistakeRecord {
  id: number
  userId: number
  questionId: number
  kpId: number
  userAnswer: string
  correctAnswer: string
  questionType: string
  reviewed: boolean
  createdAt: string
  // detail fields
  stem?: string
  options?: string
  explanation?: string
}

export interface MistakeFilters {
  bankId?: number
  questionType?: string
  reviewed?: boolean
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const mistakeApi = {
  list(params: MistakeFilters = {}) {
    return http.get<{ data: PageResult<MistakeRecord> }>('/content/mistakes', { params })
  },
  getDetail(id: number) {
    return http.get<{ data: MistakeRecord }>(`/content/mistakes/${id}`)
  },
  markReviewed(id: number) {
    return http.put(`/content/mistakes/${id}/review`)
  },
  delete(id: number) {
    return http.delete(`/content/mistakes/${id}`)
  },
  getPracticeIds(count: number = 10) {
    return http.post<{ data: number[] }>(`/content/mistakes/practice?count=${count}`)
  },
}
