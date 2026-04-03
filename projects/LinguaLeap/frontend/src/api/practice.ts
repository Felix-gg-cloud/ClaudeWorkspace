import http from './http'

export interface PracticeStartReq {
  bankId: number
  questionType: string
  grade?: string
  count: number
}

export interface UnitPracticeStartReq {
  unitId: number
  questionType: string
  grade?: string
  count: number
}

export interface StudySetPracticeStartReq {
  studySetId: number
  questionType: string
  grade?: string
  count: number
}

export interface PracticeStartRes {
  sessionId: number
  totalCount: number
  questionType: string
}

export interface WordInfo {
  word: string
  phonetic?: string
  meaning: string
}

export interface PracticeQuestion {
  questionId: number
  type: string
  stem: string
  difficulty: number
  grade?: string
  options?: string[]
  words?: WordInfo[]
  knowledgePoints?: string
  exampleSentence?: string
  exampleZh?: string
  extraData?: Record<string, any>
  progress: {
    current: number
    total: number
    correctCount: number
  }
}

export interface AnswerResult {
  correct: boolean
  correctAnswer: string
  explanation: string
  knowledgePoints?: string
  exampleSentence?: string
  exampleZh?: string
  words?: WordInfo[]
  sessionProgress: {
    current: number
    total: number
    correctCount: number
  }
}

export interface PracticeResult {
  sessionId: number
  totalCount: number
  correctCount: number
  accuracy: number
  duration: number
}

export interface TranslateJudgeResult {
  correct: boolean
  score: number
  feedback: string
  corrections: string[]
}

export const practiceApi = {
  start(data: PracticeStartReq) {
    return http.post<{ data: PracticeStartRes }>('/content/practice/start', data)
  },
  startByUnit(data: UnitPracticeStartReq) {
    return http.post<{ data: PracticeStartRes }>('/content/practice/start-by-unit', data)
  },
  startByStudySet(data: StudySetPracticeStartReq) {
    return http.post<{ data: PracticeStartRes }>('/content/practice/start-by-study-set', data)
  },
  next(sessionId: number) {
    return http.get<{ data: PracticeQuestion }>(`/content/practice/${sessionId}/next`)
  },
  answer(sessionId: number, questionId: number, answer: string) {
    return http.post<{ data: AnswerResult }>(`/content/practice/${sessionId}/answer`, { questionId, answer })
  },
  finish(sessionId: number) {
    return http.post<{ data: PracticeResult }>(`/content/practice/${sessionId}/finish`)
  },
  result(sessionId: number) {
    return http.get<{ data: PracticeResult }>(`/content/practice/${sessionId}/result`)
  },
  judgeTranslate(stem: string, referenceAnswer: string, userAnswer: string, grade: string) {
    return http.post<{ data: TranslateJudgeResult }>('/ai/judge/translate', {
      stem, referenceAnswer, userAnswer, grade,
    })
  },
  analyzePractice(params: { totalCount: number; correctCount: number; wrongDetails: string; grade: string }) {
    return http.post<{ data: { analysis: string } }>('/ai/analyze/practice', params)
  },
}
