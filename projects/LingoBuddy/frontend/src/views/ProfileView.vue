<template>
  <div class="page-container">
    <div class="profile-header">
      <div class="profile-avatar">{{ user?.username?.charAt(0).toUpperCase() }}</div>
      <h2 class="profile-name">{{ user?.username }}</h2>
      <p class="profile-level">{{ user?.levelTitle }} · Lv.{{ user?.currentLevel }}</p>
    </div>

    <!-- XP Card -->
    <div class="card profile-xp-card">
      <div class="profile-stat-row">
        <div class="profile-stat">
          <div class="profile-stat-val">{{ user?.totalXp || 0 }}</div>
          <div class="profile-stat-label">总经验</div>
        </div>
        <div class="profile-stat">
          <div class="profile-stat-val">🪙 {{ user?.coins || 0 }}</div>
          <div class="profile-stat-label">金币</div>
        </div>
        <div class="profile-stat">
          <div class="profile-stat-val">🔥 {{ user?.streak || 0 }}</div>
          <div class="profile-stat-label">连续天</div>
        </div>
      </div>
    </div>

    <!-- Achievements -->
    <div class="section-header">
      <h3>🏆 成就墙</h3>
      <span class="badge">{{ unlockedCount }}/{{ achievements.length }}</span>
    </div>

    <div class="achievements-list">
      <div
        v-for="a in achievements"
        :key="a.id"
        class="achievement-card"
        :class="{ locked: !a.unlocked }"
      >
        <div class="achievement-icon">{{ a.icon }}</div>
        <div class="achievement-info">
          <div class="achievement-name">{{ a.name }}</div>
          <div class="achievement-desc">{{ a.description }}</div>
          <div v-if="a.unlocked && a.unlockedAt" class="achievement-time">
            {{ formatDate(a.unlockedAt) }}
          </div>
        </div>
        <div v-if="a.unlocked" class="achievement-check">✅</div>
        <div v-else class="achievement-lock">🔒</div>
      </div>
    </div>

    <!-- Logout -->
    <button class="logout-btn" @click="handleLogout">退出登录</button>
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
  padding: 32px 0 16px;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
  margin: 0 auto 12px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
}

.profile-name {
  font-size: 22px;
  font-weight: 800;
}

.profile-level {
  color: #667eea;
  font-size: 14px;
  margin-top: 4px;
}

.profile-xp-card {
  margin-bottom: 24px;
}

.profile-stat-row {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.profile-stat-val {
  font-size: 20px;
  font-weight: 800;
  color: #2d3748;
}

.profile-stat-label {
  font-size: 12px;
  color: #a0aec0;
  margin-top: 4px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h3 {
    font-size: 18px;
    font-weight: 700;
  }
}

.badge {
  background: #ebf8ff;
  color: #3182ce;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.achievements-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 32px;
}

.achievement-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;

  &.locked {
    opacity: 0.5;
    filter: grayscale(0.6);
  }
}

.achievement-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.achievement-info {
  flex: 1;
}

.achievement-name {
  font-weight: 700;
  font-size: 15px;
  color: #2d3748;
}

.achievement-desc {
  font-size: 12px;
  color: #718096;
  margin-top: 2px;
}

.achievement-time {
  font-size: 11px;
  color: #a0aec0;
  margin-top: 2px;
}

.achievement-check {
  font-size: 20px;
}

.achievement-lock {
  font-size: 18px;
}

.logout-btn {
  width: 100%;
  padding: 14px;
  border: 2px solid #e2e8f0;
  border-radius: 14px;
  background: white;
  color: #718096;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 32px;

  &:hover {
    border-color: #fc8181;
    color: #e53e3e;
    background: #fff5f5;
  }
}
</style>
