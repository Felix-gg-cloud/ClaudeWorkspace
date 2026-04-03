import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'

interface UserInfo {
  id: number
  username: string
  displayName: string
  grade: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<UserInfo | null>(null)
  const token = ref<string | null>(localStorage.getItem('ll_token'))

  const isLoggedIn = computed(() => !!token.value)
  const displayName = computed(() => user.value?.displayName ?? user.value?.username ?? '')
  const grade = computed(() => user.value?.grade ?? 'junior')

  async function register(username: string, password: string, displayName?: string, gradeVal?: string) {
    const { data } = await http.post('/auth/register', { username, password, displayName, grade: gradeVal })
    const res = data.data
    token.value = res.token
    localStorage.setItem('ll_token', res.token)
    await fetchUser()
  }

  async function login(username: string, password: string) {
    const { data } = await http.post('/auth/login', { username, password })
    const res = data.data
    token.value = res.token
    localStorage.setItem('ll_token', res.token)
    await fetchUser()
  }

  async function fetchUser() {
    try {
      const { data } = await http.get('/user/me')
      user.value = data.data
      localStorage.setItem('ll_user', JSON.stringify(data.data))
    } catch {
      // load from cache
      const cached = localStorage.getItem('ll_user')
      if (cached) user.value = JSON.parse(cached)
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('ll_token')
    localStorage.removeItem('ll_user')
  }

  function loadFromCache() {
    const cached = localStorage.getItem('ll_user')
    if (cached) user.value = JSON.parse(cached)
  }

  async function updateProfile(data: { displayName?: string; grade?: string }) {
    await http.put('/user/me', data)
    await fetchUser()
  }

  return {
    user, token, isLoggedIn, displayName, grade,
    register, login, fetchUser, logout, loadFromCache, updateProfile,
  }
})
