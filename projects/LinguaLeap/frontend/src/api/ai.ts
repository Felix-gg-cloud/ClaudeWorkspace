import http from './http'

export interface AiGenerateResult {
  bankId: number
  kpId: number
  type: string
  stem: string
  options: string | null
  answer: string
  explanation: string
  difficulty: number
  createdBy: string
  fallback?: boolean
}

export interface AiBatchResult {
  total: number
  success: number
  failed: number
  questions: AiGenerateResult[]
}

export interface KpAnalyzeItem {
  content: string
  meaningZh: string
  type: string
  difficulty: number
}

export interface KpAnalyzeResult {
  knowledgePoints: KpAnalyzeItem[]
  total: number
  saved: number
}

export const aiApi = {
  generateQuestion(kpId: number, questionType: string, grade: string) {
    return http.post<{ data: AiGenerateResult }>('/ai/generate/question', { kpId, questionType, grade })
  },

  generateBatch(bankId: number, questionTypes: string[], count: number, grade: string) {
    return http.post<{ data: AiBatchResult }>('/ai/generate/batch', { bankId, questionTypes, count, grade })
  },

  analyzeText(text: string, grade: string, bankId?: number) {
    return http.post<{ data: KpAnalyzeResult }>('/ai/analyze/text', { text, grade, bankId })
  },

  chat(message: string) {
    return http.post<{ data: { reply: string } }>('/ai/chat', { message })
  },
}
