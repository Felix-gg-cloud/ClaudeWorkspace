import http from './http'

export interface TodayStats {
  date: string
  tasksCompleted: number
  correctCount: number
  wrongCount: number
  wordsLearned: number
  studyMinutes: number
}

export interface DayStats {
  date: string
  tasksCompleted: number
  correctCount: number
  wrongCount: number
  wordsLearned: number
  studyMinutes: number
}

export interface StreakInfo {
  streak: number
  totalDays: number
}

export const statsApi = {
  getToday() {
    return http.get<{ data: TodayStats }>('/user/stats/today')
  },
  getRange(from?: string, to?: string) {
    return http.get<{ data: DayStats[] }>('/user/stats/range', { params: { from, to } })
  },
  record(data: { correctCount: number; wrongCount: number; wordsLearned: number; studyMinutes: number }) {
    return http.post('/user/stats/record', data)
  },
  getStreak() {
    return http.get<{ data: StreakInfo }>('/user/stats/streak')
  },
}
