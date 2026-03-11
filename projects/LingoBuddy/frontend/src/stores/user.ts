import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'
import type { UserInfo } from '@/types'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserInfo | null>(null)
  const isLoggedIn = ref(false)

  async function login(username: string, password: string) {
    const { data } = await api.post<UserInfo>('/auth/login', { username, password })
    user.value = data
    isLoggedIn.value = true
    return data
  }

  async function fetchMe() {
    try {
      const { data } = await api.get<UserInfo>('/me')
      user.value = data
      isLoggedIn.value = true
      return data
    } catch {
      user.value = null
      isLoggedIn.value = false
      return null
    }
  }

  async function logout() {
    try { await api.post('/auth/logout') } catch {}
    user.value = null
    isLoggedIn.value = false
  }

  function updateXp(totalXp: number) {
    if (user.value) user.value.totalXp = totalXp
  }

  return { user, isLoggedIn, login, fetchMe, logout, updateXp }
})
