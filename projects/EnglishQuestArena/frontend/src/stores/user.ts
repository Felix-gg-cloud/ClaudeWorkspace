import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { mockUser } from '@/mock/data'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const isLoggedIn = ref(false)

  const displayName = computed(() => user.value?.displayName || 'Hero')

  function login(_username: string, _password: string): boolean {
    // Mock login
    user.value = { ...mockUser }
    isLoggedIn.value = true
    return true
  }

  function logout() {
    user.value = null
    isLoggedIn.value = false
  }

  function updateProfile(name: string, voice: 'en-US' | 'en-GB') {
    if (user.value) {
      user.value.displayName = name
      user.value.ttsVoice = voice
      if (user.value.firstLogin) user.value.firstLogin = false
    }
  }

  function addXp(amount: number) {
    if (!user.value) return
    user.value.totalXp += amount
    // Check level up
    if (user.value.totalXp >= user.value.xpToNextLevel) {
      user.value.currentLevel++
      user.value.skillPoints++
    }
  }

  function addCoins(amount: number) {
    if (user.value) user.value.coins += amount
  }

  // Replace {playerName} in text
  function replacePlayerName(text: string): string {
    return text.replace(/\{playerName\}/g, displayName.value)
  }

  return { user, isLoggedIn, displayName, login, logout, updateProfile, addXp, addCoins, replacePlayerName }
})
