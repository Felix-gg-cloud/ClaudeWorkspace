import http from './http'

// ========== 类型定义 ==========

export interface ChatMessage {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
  msgType: 'text' | 'quiz' | 'quiz_result' | 'assessment_result'
  metadata?: string
  createdAt: string
}

export interface ChatSession {
  id: number
  type: 'chat' | 'assessment'
  title: string
  status: 'active' | 'closed'
  createdAt: string
  updatedAt: string
}

export interface SendMessageReply {
  sessionId: number
  reply: string
  role: string
  msgType: string
  quizData?: string
}

export interface AssessmentReply {
  sessionId: number
  reply: string
  phase: 'welcome' | 'quiz' | 'complete'
  msgType: string
  quizData?: string
}

export interface StudentProfile {
  vocabularyLevel: string
  grammarLevel: string
  listeningLevel: string
  interests: string
  weakPoints: string
  strongPoints: string
  learningStyle: string
  aiAssessment: string
  assessedAt: string
}

// ========== 对话 API ==========

export const teacherApi = {
  /** 发送消息给 AI 老师 */
  sendMessage(sessionId: number, message: string, grade?: string) {
    return http.post<{ code: number; data: SendMessageReply }>('/ai/teacher/chat/send', {
      sessionId,
      message,
      grade,
    })
  },

  /** 获取/创建活跃对话 */
  getOrCreateSession(type: string = 'chat') {
    return http.post<{ code: number; data: ChatSession }>('/ai/teacher/chat/session', { type })
  },

  /** 获取对话历史 */
  getHistory(sessionId: number) {
    return http.get<{ code: number; data: ChatMessage[] }>(`/ai/teacher/chat/history/${sessionId}`)
  },

  /** 获取所有会话列表 */
  getSessions() {
    return http.get<{ code: number; data: ChatSession[] }>('/ai/teacher/chat/sessions')
  },
}

// ========== 评估 API ==========

export const assessmentApi = {
  /** 开始入学评估 */
  start(grade: string) {
    return http.post<{ code: number; data: AssessmentReply }>('/ai/teacher/assessment/start', {
      grade,
    })
  },

  /** 评估对话 */
  chat(sessionId: number, message: string, grade?: string) {
    return http.post<{ code: number; data: AssessmentReply }>('/ai/teacher/assessment/chat', {
      sessionId,
      message,
      grade,
    })
  },

  /** 检查评估状态 */
  status() {
    return http.get<{ code: number; data: { assessed: boolean } }>('/ai/teacher/assessment/status')
  },

  /** 获取学生画像 */
  getProfile() {
    return http.get<{ code: number; data: StudentProfile }>('/ai/teacher/profile')
  },
}
