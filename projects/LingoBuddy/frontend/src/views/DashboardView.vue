<template>
  <div class="page-container">
    <!-- Header -->
    <div class="dash-header">
      <div class="user-row">
        <div class="avatar">{{ user?.username?.charAt(0).toUpperCase() }}</div>
        <div class="user-info">
          <h2 class="greeting">{{ greetingText }}，{{ user?.username }} 👋</h2>
          <p class="level-badge">{{ user?.levelTitle }} · Lv.{{ user?.currentLevel }}</p>
        </div>
        <div class="coins-badge">🪙 {{ user?.coins }}</div>
      </div>
    </div>

    <!-- XP Progress -->
    <div class="card xp-card">
      <div class="xp-header">
        <span class="xp-label">经验值</span>
        <span class="xp-value">{{ user?.totalXp }} XP</span>
      </div>
      <div class="level-progress">
        <div class="level-progress-bar" :style="{ width: xpPercent + '%' }"></div>
      </div>
      <div class="xp-footer">
        <span>Lv.{{ user?.currentLevel }}</span>
        <span v-if="user && user.nextLevelXp > 0">下一级: {{ user.nextLevelXp }} XP</span>
        <span v-else>🏆 已满级</span>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid">
      <div class="stat-card" @click="$router.push('/calendar')">
        <div class="stat-icon">🔥</div>
        <div class="stat-value">{{ user?.streak || 0 }}</div>
        <div class="stat-label">连续天数</div>
      </div>
      <div class="stat-card" @click="$router.push('/calendar')">
        <div class="stat-icon">📅</div>
        <div class="stat-value">{{ user?.totalCheckins || 0 }}</div>
        <div class="stat-label">总打卡</div>
      </div>
      <div class="stat-card" @click="$router.push('/today')">
        <div class="stat-icon">✅</div>
        <div class="stat-value">{{ user?.totalTasksCompleted || 0 }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="quick-actions">
      <button class="action-btn study-btn" @click="$router.push('/today')">
        <span class="action-icon">📝</span>
        <div class="action-text">
          <strong>今日学习</strong>
          <small>开始今天的英语课程</small>
        </div>
        <span class="action-arrow">→</span>
      </button>

      <button class="action-btn checkin-btn" @click="$router.push('/calendar')">
        <span class="action-icon">🎯</span>
        <div class="action-text">
          <strong>每日打卡</strong>
          <small>坚持打卡赢取奖励</small>
        </div>
        <span class="action-arrow">→</span>
      </button>
    </div>

    <!-- Daily Tip -->
    <div class="card tip-card">
      <div class="tip-header">💡 每日小知识</div>
      <p class="tip-text">{{ dailyTip }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const user = computed(() => userStore.user)

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const xpPercent = computed(() => {
  if (!user.value) return 0
  if (user.value.nextLevelXp <= 0) return 100
  const prev = getPrevLevelXp(user.value.currentLevel)
  const range = user.value.nextLevelXp - prev
  const progress = user.value.totalXp - prev
  return Math.min(100, Math.max(0, (progress / range) * 100))
})

function getPrevLevelXp(level: number): number {
  const table: Record<number, number> = { 1: 0, 2: 200, 3: 500, 4: 1000, 5: 1800, 6: 3000, 7: 5000 }
  return table[level] || 0
}

const tips = [
  '"How are you?" 除了回答 "I\'m fine"，还可以说 "I\'m doing great!" 或 "Not bad!"',
  '英语中 "a" 用在辅音发音前，"an" 用在元音发音前。例如：an hour（h不发音）',
  '"I\'d like" 比 "I want" 更礼貌，在餐厅点餐时推荐使用。',
  '英语中 "please" 和 "thank you" 是非常重要的礼貌用语，多用不会错！',
  '"Excuse me" 用于打扰别人前，"Sorry" 用于道歉。不要搞混哦！',
  '一天背5个新单词，一年就能掌握 1825 个词汇！坚持就是胜利💪',
  '"Break a leg!" 不是让你摔断腿，而是预祝好运的意思 🎭',
]

const dailyTip = computed(() => {
  const day = Math.floor(Date.now() / 86400000)
  return tips[day % tips.length]
})

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    const u = await userStore.fetchMe()
    if (!u) router.replace('/login')
  }
})
</script>

<style scoped lang="scss">
.dash-header {
  padding: 24px 0 16px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.greeting {
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
}

.level-badge {
  font-size: 13px;
  color: #667eea;
  margin-top: 2px;
}

.coins-badge {
  background: #fef3c7;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #d69e2e;
}

.xp-card {
  margin: 8px 0 16px;
}

.xp-header, .xp-footer {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #718096;
}

.xp-header {
  margin-bottom: 8px;
}

.xp-value {
  font-weight: 700;
  color: #667eea;
}

.xp-footer {
  margin-top: 6px;
  font-size: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 16px 8px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  }
}

.stat-icon {
  font-size: 28px;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #2d3748;
}

.stat-label {
  font-size: 12px;
  color: #a0aec0;
  margin-top: 2px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s;
  text-align: left;
  width: 100%;

  &:hover {
    transform: translateY(-2px);
  }
}

.study-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.checkin-btn {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(72, 187, 120, 0.4);
}

.action-icon {
  font-size: 32px;
}

.action-text {
  flex: 1;

  strong {
    display: block;
    font-size: 16px;
    margin-bottom: 2px;
  }

  small {
    font-size: 12px;
    opacity: 0.8;
  }
}

.action-arrow {
  font-size: 20px;
  opacity: 0.6;
}

.tip-card {
  background: linear-gradient(135deg, #fef3c7 0%, #fefce8 100%);
  border: 1px solid #f6e05e;
}

.tip-header {
  font-weight: 700;
  margin-bottom: 8px;
  color: #d69e2e;
}

.tip-text {
  font-size: 14px;
  color: #744210;
  line-height: 1.6;
}
</style>
