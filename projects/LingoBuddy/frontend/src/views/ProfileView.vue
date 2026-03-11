<template>
  <div class="page-container">
    <!-- Avatar & Info -->
    <div class="profile-header">
      <div class="avatar-ring">
        <div class="avatar-inner">{{ user?.username?.charAt(0).toUpperCase() }}</div>
      </div>
      <h2 class="profile-name text-gradient">{{ user?.username }}</h2>
      <p class="profile-level">{{ user?.levelTitle }} · Lv.{{ user?.currentLevel }}</p>
    </div>

    <!-- Stats -->
    <div class="stats-grid">
      <div class="glass-card stat-item">
        <div class="stat-val text-gradient">{{ user?.totalXp || 0 }}</div>
        <div class="stat-label">总经验</div>
      </div>
      <div class="glass-card stat-item">
        <div class="stat-val text-gradient-warm">💎 {{ user?.coins || 0 }}</div>
        <div class="stat-label">宝石</div>
      </div>
      <div class="glass-card stat-item">
        <div class="stat-val" style="color: #f97316;">🔥 {{ user?.streak || 0 }}</div>
        <div class="stat-label">连续天</div>
      </div>
    </div>

    <!-- Achievements -->
    <div class="section-header">
      <h3>🏆 成就墙</h3>
      <span class="badge-pill">{{ unlockedCount }}/{{ achievements.length }}</span>
    </div>

    <div class="achievements-list">
      <div
        v-for="a in achievements"
        :key="a.id"
        class="glass-card ach-card"
        :class="{ locked: !a.unlocked }"
      >
        <div class="ach-icon">{{ a.icon }}</div>
        <div class="ach-body">
          <div class="ach-name">{{ a.name }}</div>
          <div class="ach-desc">{{ a.description }}</div>
          <div v-if="a.unlocked && a.unlockedAt" class="ach-time">{{ formatDate(a.unlockedAt) }}</div>
        </div>
        <div class="ach-badge">
          <span v-if="a.unlocked" class="ach-done">✅</span>
          <span v-else class="ach-lock">🔒</span>
        </div>
      </div>
    </div>

    <!-- Logout -->
    <button class="btn-ghost logout-btn" @click="handleLogout">退出登录</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import api from '@/api'
import type { AchievementDto } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const achievements = ref<AchievementDto[]>([])

const unlockedCount = computed(() => achievements.value.filter(a => a.unlocked).length)

function formatDate(dateStr: string) {
  try {
    const d = new Date(dateStr)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch {
    return dateStr
  }
}

async function handleLogout() {
  await userStore.logout()
  router.replace('/login')
}

onMounted(async () => {
  try {
    const { data } = await api.get<AchievementDto[]>('/achievements')
    achievements.value = data
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped lang="scss">
.profile-header {
  text-align: center;
  padding: 32px 0 20px;
}

.avatar-ring {
  width: 88px;
  height: 88px;
  margin: 0 auto 14px;
  border-radius: 50%;
  padding: 3px;
  background: conic-gradient(#00d4ff, #a855f7, #ec4899, #00d4ff);
  animation: glow-ring 4s linear infinite;
}

.avatar-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #0c1029;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 800;
  color: #e8ecf4;
}

.profile-name {
  font-size: 22px;
  font-weight: 800;
}

.profile-level {
  color: #00d4ff;
  font-size: 14px;
  margin-top: 4px;
}

// Stats grid
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px 8px;
}

.stat-val {
  font-size: 18px;
  font-weight: 800;
}

.stat-label {
  font-size: 11px;
  color: #5a6480;
  margin-top: 4px;
}

// Section
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h3 {
    font-size: 18px;
    font-weight: 700;
    color: #e8ecf4;
  }
}

.badge-pill {
  background: rgba(0, 212, 255, 0.1);
  color: #00d4ff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(0, 212, 255, 0.15);
}

// Achievements
.achievements-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 28px;
}

.ach-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  transition: all 0.3s;

  &.locked {
    opacity: 0.4;
    filter: grayscale(0.6);
  }

  &:not(.locked):hover {
    border-color: rgba(0, 212, 255, 0.3);
  }
}

.ach-icon {
  font-size: 30px;
  flex-shrink: 0;
}

.ach-body { flex: 1; }

.ach-name {
  font-weight: 700;
  font-size: 14px;
  color: #e8ecf4;
}

.ach-desc {
  font-size: 12px;
  color: #5a6480;
  margin-top: 2px;
}

.ach-time {
  font-size: 11px;
  color: #3d4663;
  margin-top: 2px;
}

.ach-done { font-size: 20px; }
.ach-lock { font-size: 18px; }

// Logout
.logout-btn {
  width: 100%;
  padding: 14px;
  margin-bottom: 32px;
  color: #5a6480;

  &:hover {
    border-color: rgba(239, 68, 68, 0.4);
    color: #ef4444;
    background: rgba(239, 68, 68, 0.06);
  }
}
</style>
