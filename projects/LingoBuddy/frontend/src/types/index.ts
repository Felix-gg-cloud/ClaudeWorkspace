export interface UserInfo {
  id: number
  username: string
  totalXp: number
  coins: number
  currentLevel: number
  levelTitle: string
  nextLevelXp: number
  streak: number
  totalCheckins: number
  totalTasksCompleted: number
}

export interface TaskDto {
  id: number
  taskCode: string
  type: 'VOCAB_CARD' | 'QUIZ_SINGLE' | 'SPELLING'
  question: string
  options: string | null
  correctAnswer: string
  explanation: string
  xpReward: number
  sortOrder: number
  completed: boolean
}

export interface TodayLessonDto {
  lessonId: number
  title: string
  description: string
  dayIndex: number
  completedCount: number
  totalCount: number
  allCompleted: boolean
  tasks: TaskDto[]
}

export interface TaskCompleteResult {
  xpGained: number
  totalXp: number
  coinsGained: number
  lessonCompleted: boolean
  levelUp: boolean
  newLevel: number
  newLevelTitle: string
  checkinXp: number
  checkinCoins: number
  streak: number
  newAchievements: AchievementDto[]
}

export interface AchievementDto {
  id: number
  code: string
  name: string
  description: string
  icon: string
  conditionType: string
  conditionValue: number
  unlocked: boolean
  unlockedAt?: string
}

export interface CheckinDto {
  date: string
  xpEarned: number
  coinsEarned: number
  streak: number
}

export interface CheckinResult {
  date: string
  xpEarned: number
  coinsEarned: number
  streak: number
}
