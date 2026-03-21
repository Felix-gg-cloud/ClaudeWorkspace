<template>
  <div class="page-container">
    <!-- Hero Header -->
    <div class="hero-section">
      <div class="hero-top">
        <div class="hero-left">
          <div class="avatar-ring">
            <div class="avatar">{{ user?.username?.charAt(0).toUpperCase() }}</div>
          </div>
          <div>
            <h2 class="greeting">{{ greetingText }}，{{ user?.username }}</h2>
            <div class="level-tag">
              <span class="level-dot"></span>
              {{ user?.levelTitle }} · Lv.{{ user?.currentLevel }}
            </div>
          </div>
        </div>
        <div class="coins-chip">
          <span class="coin-icon">💎</span>
          <span>{{ user?.coins }}</span>
        </div>
      </div>
    </div>

    <!-- XP Progress Ring -->
    <div class="glass-card xp-ring-card">
      <div class="xp-ring-layout">
        <div class="xp-ring-container">
          <svg viewBox="0 0 120 120" class="xp-svg">
            <defs>
              <linearGradient id="xpGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#00d4ff" />
                <stop offset="100%" stop-color="#a855f7" />
              </linearGradient>
            </defs>
            <circle cx="60" cy="60" r="52" class="ring-bg" />
            <circle cx="60" cy="60" r="52"
              class="ring-fill"
              :style="{ strokeDashoffset: ringOffset }"
            />
          </svg>
          <div class="ring-center">
            <div class="ring-xp">{{ user?.totalXp || 0 }}</div>
            <div class="ring-label">XP</div>
          </div>
        </div>
        <div class="xp-details">
          <div class="xp-detail-item">
            <span class="xp-detail-label">当前等级</span>
            <span class="xp-detail-value text-gradient">Lv.{{ user?.currentLevel }}</span>
          </div>
          <div class="xp-detail-item">
            <span class="xp-detail-label">下一等级</span>
            <span class="xp-detail-value" v-if="user && user.nextLevelXp > 0">{{ user.nextLevelXp }} XP</span>
            <span class="xp-detail-value text-gradient-warm" v-else>已满级 🏆</span>
          </div>
          <div class="xp-detail-item">
            <span class="xp-detail-label">进度</span>
            <span class="xp-detail-value">{{ Math.round(xpPercent) }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="stats-row">
      <div class="stat-chip" @click="$router.push('/calendar')">
        <div class="stat-chip-icon fire">🔥</div>
        <div class="stat-chip-num">{{ user?.streak || 0 }}</div>
        <div class="stat-chip-label">连续天数</div>
      </div>
      <div class="stat-chip" @click="$router.push('/calendar')">
        <div class="stat-chip-icon cal">📅</div>
        <div class="stat-chip-num">{{ user?.totalCheckins || 0 }}</div>
        <div class="stat-chip-label">累计打卡</div>
      </div>
      <div class="stat-chip" @click="$router.push('/today')">
        <div class="stat-chip-icon check">✅</div>
        <div class="stat-chip-num">{{ user?.totalTasksCompleted || 0 }}</div>
        <div class="stat-chip-label">已完成</div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="actions-section">
      <button class="action-card study" @click="$router.push('/today')">
        <div class="action-glow"></div>
        <span class="action-emoji">📝</span>
        <div class="action-body">
          <strong>今日学习</strong>
          <small>开始英语课程挑战</small>
        </div>
        <span class="action-arrow">›</span>
      </button>

      <button class="action-card explore" @click="$router.push('/stages')">
        <div class="action-glow green"></div>
        <span class="action-emoji">🗺️</span>
        <div class="action-body">
          <strong>学习旅程</strong>
          <small>查看阶段地图</small>
        </div>
        <span class="action-arrow">›</span>
      </button>

      <button class="action-card checkin" @click="$router.push('/calendar')">
        <div class="action-glow orange"></div>
        <span class="action-emoji">🎯</span>
        <div class="action-body">
          <strong>每日打卡</strong>
          <small>坚持打卡赢取奖励</small>
        </div>
        <span class="action-arrow">›</span>
      </button>
    </div>

    <!-- Daily Tip -->
    <div class="glass-card tip-card">
      <div class="tip-head">
        <span class="tip-icon">💡</span>
        <span class="tip-title">每日英语小知识</span>
        <span class="tip-badge">Day {{ dayCount }}</span>
      </div>
      <p class="tip-text">{{ dailyTip }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const user = computed(() => userStore.user)

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '🌙 夜深了'
  if (h < 12) return '☀️ 早上好'
  if (h < 14) return '🌤️ 中午好'
  if (h < 18) return '🌇 下午好'
  return '🌆 晚上好'
})

const xpPercent = computed(() => {
  if (!user.value) return 0
  if (user.value.nextLevelXp <= 0) return 100
  const prev = getPrevLevelXp(user.value.currentLevel)
  const range = user.value.nextLevelXp - prev
  const progress = user.value.totalXp - prev
  return Math.min(100, Math.max(0, (progress / range) * 100))
})

const circumference = 2 * Math.PI * 52
const ringOffset = computed(() => {
  return circumference - (xpPercent.value / 100) * circumference
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
  '一天背 5 个新单词，一年就能掌握 1825 个词汇！坚持就是胜利 💪',
  '"Break a leg!" 不是让你摔断腿，而是预祝好运的意思 🎭',
  '"It\'s raining cats and dogs" 表示雨下得很大，不是真的下猫和狗 🐱🐶',
  '英语中 "How come?" 等于 "Why?"，但语气更随意、更口语化。',
  '"Piece of cake" 不是一块蛋糕，而是表示某件事非常简单 🍰',
  '"I\'m over the moon!" 表示非常开心、兴奋，有种飘上月球的感觉 🌙',
  '"Hit the books" 不是打书，而是用功学习的意思 📚',
  '"Let\'s call it a day" 表示今天到此为止，该休息了 🌅',
  '"No pain, no gain" 不劳无获，适用于学习英语的你！💪',
  '"The early bird catches the worm" 早起的鸟儿有虫吃 🐦',
]

const dayCount = computed(() => Math.floor(Date.now() / 86400000) % 365 + 1)
const dailyTip = computed(() => {
  const day = Math.floor(Date.now() / 86400000)
  return tips[day % tips.length]
})
</script>

<style scoped lang="scss">
.hero-section {
  padding: 28px 0 20px;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-ring {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00d4ff, #a855f7);
  padding: 2px;
  flex-shrink: 0;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #111538;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #e8ecf4;
}

.greeting {
  font-size: 17px;
  font-weight: 700;
  color: #e8ecf4;
}

.level-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #00d4ff;
  margin-top: 3px;
}

.level-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00d4ff;
  animation: pulse-glow 2s infinite;
}

.coins-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(168, 85, 247, 0.15);
  border: 1px solid rgba(168, 85, 247, 0.2);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 700;
  color: #c084fc;
}

// XP Ring
.xp-ring-card {
  margin-bottom: 16px;
}

.xp-ring-layout {
  display: flex;
  align-items: center;
  gap: 24px;
}

.xp-ring-container {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.xp-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba(255, 255, 255, 0.05);
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke: url(#xpGradient);
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dasharray: 326.73;
  transition: stroke-dashoffset 1s cubic-bezier(0.4, 0, 0.2, 1);
  filter: drop-shadow(0 0 6px rgba(0, 212, 255, 0.4));
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-xp {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #00d4ff, #a855f7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.ring-label {
  font-size: 11px;
  color: #5a6480;
  font-weight: 600;
  letter-spacing: 2px;
}

.xp-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.xp-detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.xp-detail-label {
  font-size: 13px;
  color: #5a6480;
}

.xp-detail-value {
  font-size: 14px;
  font-weight: 700;
  color: #e8ecf4;
}

// Stats
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.stat-chip {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 16px 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: rgba(255, 255, 255, 0.07);
    transform: translateY(-3px);
    border-color: rgba(255, 255, 255, 0.1);
  }
}

.stat-chip-icon {
  font-size: 26px;
  margin-bottom: 6px;
}

.stat-chip-num {
  font-size: 22px;
  font-weight: 800;
  color: #e8ecf4;
}

.stat-chip-label {
  font-size: 11px;
  color: #5a6480;
  margin-top: 3px;
}

// Actions
.actions-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.35s;
  text-align: left;
  width: 100%;
  position: relative;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.03);

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(255, 255, 255, 0.1);
  }

  &.study {
    &:hover { box-shadow: 0 4px 20px rgba(0, 212, 255, 0.15); }
  }

  &.explore {
    &:hover { box-shadow: 0 4px 20px rgba(16, 185, 129, 0.15); }
  }

  &.checkin {
    &:hover { box-shadow: 0 4px 20px rgba(249, 115, 22, 0.15); }
  }
}

.action-glow {
  position: absolute;
  top: -20px;
  left: -20px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.12), transparent 70%);
  pointer-events: none;

  &.green { background: radial-gradient(circle, rgba(16, 185, 129, 0.12), transparent 70%); }
  &.orange { background: radial-gradient(circle, rgba(249, 115, 22, 0.12), transparent 70%); }
}

.action-emoji {
  font-size: 30px;
  position: relative;
  z-index: 1;
}

.action-body {
  flex: 1;
  position: relative;
  z-index: 1;

  strong {
    display: block;
    font-size: 15px;
    color: #e8ecf4;
    font-weight: 700;
  }

  small {
    font-size: 12px;
    color: #5a6480;
  }
}

.action-arrow {
  font-size: 24px;
  color: #5a6480;
  position: relative;
  z-index: 1;
}

// Tip
.tip-card {
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 100px;
    height: 100px;
    background: radial-gradient(circle, rgba(245, 158, 11, 0.06), transparent 70%);
    pointer-events: none;
  }
}

.tip-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.tip-icon {
  font-size: 20px;
}

.tip-title {
  font-weight: 700;
  font-size: 14px;
  color: #f59e0b;
  flex: 1;
}

.tip-badge {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
  font-weight: 600;
}

.tip-text {
  font-size: 14px;
  color: #8b95b0;
  line-height: 1.7;
  position: relative;
  z-index: 1;
}
</style>
