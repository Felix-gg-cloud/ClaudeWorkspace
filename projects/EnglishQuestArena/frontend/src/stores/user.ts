import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import http from '@/api/http'
import { useDailyGoalStore } from '@/stores/dailyGoal'
import { useMistakeStore } from '@/stores/mistakes'
import { useAchievementStore } from '@/stores/achievements'
import { useChapterStore } from '@/stores/chapter'

/** 登录/注册/恢复会话后，通知各 store 加载该用户的本地数据 */
function reloadLocalStores(userId: number) {
  useDailyGoalStore().reload(userId)
  useMistakeStore().reload(userId)
  useAchievementStore().reload(userId)
  useChapterStore().reload(userId)
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const isLoggedIn = ref(false)

  const displayName = computed(() => user.value?.displayName || 'Hero')
  const totalXp = computed(() => user.value?.totalXp ?? 0)
  const coins = computed(() => user.value?.coins ?? 0)
  const streak = computed(() => user.value?.streak ?? 0)
  const currentLevel = computed(() => user.value?.currentLevel ?? 1)
  const avatar = computed(() => user.value?.avatar || user.value?.displayName?.charAt(0).toUpperCase() || '?')
  const username = computed(() => user.value?.username ?? '')

  /** 登录 */
  async function login(username: string, password: string): Promise<boolean> {
    try {
      const { data } = await http.post('/auth/login', { username, password })
      user.value = mapUser(data)
      isLoggedIn.value = true
      reloadLocalStores(user.value.id)
      return true
    } catch {
      return false
    }
  }

  /** 注册 */
  async function register(username: string, password: string, displayName?: string): Promise<boolean> {
    try {
      const { data } = await http.post('/auth/register', { username, password, displayName: displayName || 'Hero' })
      user.value = mapUser(data)
      isLoggedIn.value = true
      reloadLocalStores(user.value.id)
      return true
    } catch {
      return false
    }
  }

  /** 退出 */
  async function logout() {
    try { await http.post('/auth/logout') } catch { /* ignore */ }
    user.value = null
    isLoggedIn.value = false
  }

  /** 恢复会话（页面刷新时调用） */
  async function restoreSession(): Promise<boolean> {
    try {
      const { data } = await http.get('/auth/me')
      user.value = mapUser(data)
      isLoggedIn.value = true
      reloadLocalStores(user.value.id)
      return true
    } catch {
      user.value = null
      isLoggedIn.value = false
      return false
    }
  }

  /** 更新个人资料（同步到后端） */
  async function updateProfile(profile: { displayName?: string; avatar?: string; cefrLevel?: string; ttsVoice?: string }): Promise<boolean> {
    try {
      const { data } = await http.put('/auth/profile', profile)
      user.value = mapUser(data)
      return true
    } catch {
      return false
    }
  }

  /** 加 XP（本地同步更新 + 后台同步后端） */
  function addXp(amount: number) {
    if (!user.value) return
    const newXp = user.value.totalXp + amount
    const levelUp = newXp >= user.value.xpToNextLevel
    user.value = {
      ...user.value,
      totalXp: newXp,
      currentLevel: levelUp ? user.value.currentLevel + 1 : user.value.currentLevel,
      skillPoints: levelUp ? user.value.skillPoints + 1 : user.value.skillPoints,
    }
    http.post('/progress/xp', { xp: amount }).catch(() => {})
  }

  function addCoins(amount: number) {
    if (!user.value) return
    user.value = { ...user.value, coins: user.value.coins + amount }
    http.post('/progress/coins', { coins: amount }).catch(() => {})
  }

  /** 同时加 XP 和金币（单次赋值，避免响应式竞争） */
  function addReward(xp: number, coinAmount: number) {
    if (!user.value) return
    const newXp = user.value.totalXp + xp
    const levelUp = newXp >= user.value.xpToNextLevel
    user.value = {
      ...user.value,
      totalXp: newXp,
      coins: user.value.coins + coinAmount,
      currentLevel: levelUp ? user.value.currentLevel + 1 : user.value.currentLevel,
      skillPoints: levelUp ? user.value.skillPoints + 1 : user.value.skillPoints,
    }
    http.post('/progress/xp', { xp }).catch(() => {})
    http.post('/progress/coins', { coins: coinAmount }).catch(() => {})
  }

  /** 从后端数据同步用户状态（其他 API 返回 totalXp/coins 时调用） */
  function syncFromServer(data: { totalXp?: number; coins?: number }) {
    if (!user.value) return
    if (data.totalXp !== undefined) user.value.totalXp = data.totalXp
    if (data.coins !== undefined) user.value.coins = data.coins
  }

  function replacePlayerName(text: string): string {
    return text.replace(/\{playerName\}/g, displayName.value)
  }

  return { user, isLoggedIn, displayName, totalXp, coins, streak, currentLevel, avatar, username, login, register, logout, restoreSession, updateProfile, addXp, addCoins, addReward, syncFromServer, replacePlayerName }
})

function mapUser(data: Record<string, unknown>): User {
  return {
    id: data.id as number,
    username: data.username as string,
    displayName: (data.displayName as string) || 'Hero',
    avatar: (data.avatar as string) || '⚔️',
    ttsVoice: (data.ttsVoice as 'en-US' | 'en-GB') || 'en-US',
    cefrLevel: (data.cefrLevel as User['cefrLevel']) || 'PRE_A1',
    currentLevel: (data.currentLevel as number) || 1,
    totalXp: (data.totalXp as number) || 0,
    xpToNextLevel: (data.xpToNextLevel as number) || 200,
    coins: (data.coins as number) || 0,
    skillPoints: (data.skillPoints as number) || 0,
    streak: (data.streak as number) || 0,
    totalCheckins: (data.totalCheckins as number) || 0,
    firstLogin: (data.firstLogin as boolean) ?? true,
  }
}
