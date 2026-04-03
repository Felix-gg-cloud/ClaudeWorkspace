import http from './http'

export interface QuestionBank {
  id: number
  name: string
  description: string
  grade: string
  type: string
  userId: number | null
  sourceFileUrl: string | null
  status: string
  kpCount: number
  questionCount: number
  createdAt: string
}

export interface KnowledgePoint {
  id: number
  bankId: number
  type: string
  content: string
  phonetic: string
  meaningZh: string
  exampleSentence: string
  exampleZh: string
  difficulty: number
  tags: string
  createdAt: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface BankFilters {
  grade?: string
  type?: string
  keyword?: string
  page?: number
  size?: number
}

export const bankApi = {
  list(params: BankFilters = {}) {
    return http.get<{ data: PageResult<QuestionBank> }>('/content/banks', { params })
  },
  getById(id: number) {
    return http.get<{ data: QuestionBank }>(`/content/banks/${id}`)
  },
  create(bank: Partial<QuestionBank>) {
    return http.post<{ data: QuestionBank }>('/content/banks', bank)
  },
  update(id: number, bank: Partial<QuestionBank>) {
    return http.put<{ data: QuestionBank }>(`/content/banks/${id}`, bank)
  },
  delete(id: number) {
    return http.delete(`/content/banks/${id}`)
  },
}

export const kpApi = {
  list(bankId: number, params: { type?: string; keyword?: string; difficulty?: number; page?: number; size?: number } = {}) {
    return http.get<{ data: PageResult<KnowledgePoint> }>(`/content/banks/${bankId}/kps`, { params })
  },
  getById(id: number) {
    return http.get<{ data: KnowledgePoint }>(`/content/kps/${id}`)
  },
}
