<template>
  <MainLayout>
    <div class="stats-page">
      <h2 class="page-title"><span class="title-deco"></span>📊 学习统计<span class="title-deco"></span></h2>

      <!-- 总览卡片 -->
      <section class="overview-cards">
        <div class="ov-card dark-panel">
          <div class="ov-icon">⭐</div>
          <div class="ov-value">{{ animatedXp }}</div>
          <div class="ov-label">经验值</div>
        </div>
        <div class="ov-card dark-panel">
          <div class="ov-icon">💰</div>
          <div class="ov-value">{{ animatedCoins }}</div>
          <div class="ov-label">金币</div>
        </div>
        <div class="ov-card dark-panel">
          <div class="ov-icon">📅</div>
          <div class="ov-value">{{ animatedDays }}</div>
          <div class="ov-label">学习天数</div>
        </div>
        <div class="ov-card dark-panel">
          <div class="ov-icon">🎯</div>
          <div class="ov-value">{{ animatedAccuracy }}%</div>
          <div class="ov-label">正确率</div>
        </div>
      </section>

      <!-- 7天学习趋势 -->
      <section class="stats-section">
        <h3 class="section-title">📈 近7天学习趋势</h3>
        <div class="chart-card dark-panel">
          <Line :data="weeklyChartData" :options="lineOptions" />
        </div>
      </section>

      <!-- 正确率 + 词汇掌握 双图表 -->
      <section class="stats-section">
        <div class="chart-row">
          <div class="chart-half dark-panel">
            <h4 class="chart-subtitle">正确率分布</h4>
            <div class="doughnut-wrap">
              <Doughnut :data="accuracyChartData" :options="doughnutOptions" />
              <div class="doughnut-center">
                <span class="dc-value">{{ dailyStatsStore.totalAccuracy }}%</span>
                <span class="dc-label">正确率</span>
              </div>
            </div>
          </div>
          <div class="chart-half dark-panel">
            <h4 class="chart-subtitle">词汇掌握度</h4>
            <div class="doughnut-wrap">
              <Doughnut :data="masteryChartData" :options="doughnutOptions" />
              <div class="doughnut-center">
                <span class="dc-value">{{ srsStore.totalCards }}</span>
                <span class="dc-label">总词量</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 记忆系统 -->
      <section class="stats-section">
        <div class="section-header">
          <h3 class="section-title">🧠 记忆系统</h3>
          <button class="link-btn" @click="$router.push('/srs-detail')">查看全部 →</button>
        </div>
        <div class="info-card dark-panel">
          <div class="info-row">
            <span>词库总量</span>
            <strong>{{ srsStore.totalCards }} 词</strong>
          </div>
          <div class="info-row">
            <span>今日待复习</span>
            <strong :class="{ 'due-alert': srsStore.dueCards.length > 0 }">{{ srsStore.dueCards.length }} 词</strong>
          </div>
          <div class="info-row">
            <span>已掌握</span>
            <strong class="text-green">{{ masteredCount }} 词</strong>
          </div>
        </div>
      </section>

      <!-- 错题本 -->
      <section class="stats-section">
        <div class="section-header">
          <h3 class="section-title">❌ 错题本</h3>
          <button class="link-btn" @click="$router.push('/mistakes')">查看全部 →</button>
        </div>
        <div class="info-card dark-panel">
          <div class="info-row">
            <span>错题总数</span>
            <strong>{{ mistakeStore.totalCount }}</strong>
          </div>
          <div class="info-row">
            <span>待复习</span>
            <strong :class="{ 'due-alert': mistakeStore.unreviewedCount > 0 }">{{ mistakeStore.unreviewedCount }}</strong>
          </div>
          <div v-if="topMistakes.length > 0" class="top-mistakes">
            <h4>高频错词 TOP 5</h4>
            <div v-for="m in topMistakes" :key="m.wordCode" class="mistake-item">
              <div class="mistake-top">
                <span class="mistake-word">{{ wordLabel(m.wordCode) }}</span>
                <span class="mistake-count">× {{ m.count }}</span>
              </div>
              <div class="mistake-bar">
                <div class="mistake-bar__fill" :style="{ width: mistakeBarWidth(m.count) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 成就进度 -->
      <section class="stats-section">
        <h3 class="section-title">🏅 成就 ({{ achievementStore.unlockedCount }}/{{ achievementStore.totalCount }})</h3>
        <div class="achievement-grid">
          <div
            v-for="ach in achievements"
            :key="ach.code"
            class="ach-card dark-panel"
            :class="{ 'ach--unlocked': ach.progress.unlocked }"
          >
            <div class="ach-icon">{{ ach.icon }}</div>
            <div class="ach-info">
              <div class="ach-name">{{ ach.nameZh }}</div>
              <div class="ach-desc">{{ ach.descZh }}</div>
              <div v-if="!ach.progress.unlocked" class="ach-prog">
                <div class="goal-bar" style="height: 3px;">
                  <div class="goal-bar__fill" :style="{ width: achProgress(ach) + '%' }"></div>
                </div>
                <span class="ach-prog-text">{{ ach.progress.currentValue }}/{{ ach.condition.threshold }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <button class="back-btn" @click="$router.push('/dashboard')">← 返回主页</button>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { Line, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale,
  LineElement, PointElement, ArcElement,
  Title, Tooltip, Legend, Filler,
} from 'chart.js'
import { useSrsStore } from '@/stores/srs'
import { useMistakeStore } from '@/stores/mistakes'
import { useAchievementStore } from '@/stores/achievements'
import { useUserStore } from '@/stores/user'
import { useDailyStatsStore } from '@/stores/dailyStats'
import { chapterConfigs } from '@/mock/chapters'
import type { ContentItem } from '@/types'

ChartJS.register(CategoryScale, LinearScale, LineElement, PointElement, ArcElement, Title, Tooltip, Legend, Filler)

const srsStore = useSrsStore()
const mistakeStore = useMistakeStore()
const achievementStore = useAchievementStore()
const userStore = useUserStore()
const dailyStatsStore = useDailyStatsStore()

// 数字跳动动画
const animatedXp = ref(0)
const animatedCoins = ref(0)
const animatedDays = ref(0)
const animatedAccuracy = ref(0)

function animateValue(refVal: ReturnType<typeof ref<number>>, target: number, duration = 800) {
  const start = refVal.value ?? 0
  const diff = target - start
  if (diff === 0) return
  const startTime = performance.now()
  function tick(now: number) {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    const ease = 1 - Math.pow(1 - progress, 3)
    refVal.value = Math.round(start + diff * ease)
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

function runCountAnimation() {
  animateValue(animatedXp, userStore.totalXp)
  animateValue(animatedCoins, userStore.coins)
  animateValue(animatedDays, dailyStatsStore.studyDays)
  animateValue(animatedAccuracy, dailyStatsStore.totalAccuracy)
}

onMounted(async () => {
  await Promise.all([
    srsStore.loadAll(),
    achievementStore.loadAchievements(),
  ])
  runCountAnimation()
})

// 错题进度条宽度计算
function mistakeBarWidth(count: number): number {
  const first = topMistakes.value[0]
  const max = first ? first.count : 1
  return Math.round((count / max) * 100)
}

// Word lookup
const wordLookup = computed(() => {
  const map: Record<string, ContentItem> = {}
  for (const ch of chapterConfigs) {
    for (const w of ch.words) map[w.code] = w
  }
  return map
})

function wordLabel(code: string): string {
  const w = wordLookup.value[code]
  return w ? `${w.en} (${w.zh})` : code
}

// SRS mastery counts
function getMastery(interval: number, correctStreak: number): string {
  if (correctStreak >= 3 && interval >= 7) return 'mastered'
  if (correctStreak >= 1) return 'reviewing'
  return 'new'
}

const masteredCount = computed(() =>
  Object.values(srsStore.cards).filter(c => getMastery(c.interval, c.correctStreak) === 'mastered').length
)
const reviewingCount = computed(() =>
  Object.values(srsStore.cards).filter(c => getMastery(c.interval, c.correctStreak) === 'reviewing').length
)
const newWordCount = computed(() =>
  Object.values(srsStore.cards).filter(c => getMastery(c.interval, c.correctStreak) === 'new').length
)

// --- Charts ---

// 7-day line chart with area fill
const weeklyChartData = computed(() => {
  const days = dailyStatsStore.getRecentDays(7)
  return {
    labels: days.map(d => d.date.slice(5)),
    datasets: [
      {
        label: '正确',
        data: days.map(d => d.correctCount),
        borderColor: 'rgba(16, 185, 129, 1)',
        backgroundColor: 'rgba(16, 185, 129, 0.15)',
        fill: true,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: 'rgba(16, 185, 129, 1)',
        pointBorderColor: '#111827',
        pointBorderWidth: 2,
        borderWidth: 2.5,
      },
      {
        label: '错误',
        data: days.map(d => d.wrongCount),
        borderColor: 'rgba(239, 68, 68, 1)',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        fill: true,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: 'rgba(239, 68, 68, 1)',
        pointBorderColor: '#111827',
        pointBorderWidth: 2,
        borderWidth: 2.5,
      },
    ],
  }
})

const lineOptions = {
  responsive: true,
  maintainAspectRatio: true,
  animation: { duration: 1000, easing: 'easeOutQuart' as const },
  interaction: { mode: 'index' as const, intersect: false },
  plugins: {
    legend: {
      labels: {
        color: '#9ca3af',
        font: { size: 11, weight: 600 as const },
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 16,
      },
    },
    tooltip: {
      backgroundColor: 'rgba(17, 24, 39, 0.95)',
      titleColor: '#f59e0b',
      bodyColor: '#e5e7eb',
      borderColor: 'rgba(245, 158, 11, 0.3)',
      borderWidth: 1,
      cornerRadius: 8,
      padding: 10,
      titleFont: { weight: 'bold' as const },
    },
  },
  scales: {
    x: {
      ticks: { color: '#6b7280', font: { size: 11 } },
      grid: { display: false },
      border: { display: false },
    },
    y: {
      beginAtZero: true,
      ticks: { color: '#6b7280', font: { size: 11 }, stepSize: 1 },
      grid: { color: 'rgba(255,255,255,0.04)', drawTicks: false },
      border: { display: false },
    },
  },
}

// Accuracy doughnut
const accuracyChartData = computed(() => {
  const days = dailyStatsStore.getRecentDays(30)
  const correct = days.reduce((s, d) => s + d.correctCount, 0)
  const wrong = days.reduce((s, d) => s + d.wrongCount, 0)
  return {
    labels: ['正确', '错误'],
    datasets: [{
      data: [correct || 0, wrong || 0],
      backgroundColor: [
        'rgba(16, 185, 129, 0.85)',
        'rgba(239, 68, 68, 0.75)',
      ],
      hoverBackgroundColor: [
        'rgba(16, 185, 129, 1)',
        'rgba(239, 68, 68, 1)',
      ],
      borderWidth: 0,
      spacing: 3,
    }],
  }
})

// Mastery doughnut
const masteryChartData = computed(() => ({
  labels: ['已掌握', '复习中', '新学'],
  datasets: [{
    data: [masteredCount.value, reviewingCount.value, newWordCount.value],
    backgroundColor: [
      'rgba(16, 185, 129, 0.85)',
      'rgba(245, 158, 11, 0.85)',
      'rgba(124, 58, 237, 0.75)',
    ],
    hoverBackgroundColor: [
      'rgba(16, 185, 129, 1)',
      'rgba(245, 158, 11, 1)',
      'rgba(124, 58, 237, 1)',
    ],
    borderWidth: 0,
    spacing: 3,
  }],
}))

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: true,
  cutout: '65%',
  animation: { animateRotate: true, duration: 900 },
  plugins: {
    legend: {
      position: 'bottom' as const,
      labels: {
        color: '#9ca3af',
        font: { size: 11, weight: 600 as const },
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 14,
      },
    },
    tooltip: {
      backgroundColor: 'rgba(17, 24, 39, 0.95)',
      titleColor: '#f59e0b',
      bodyColor: '#e5e7eb',
      borderColor: 'rgba(245, 158, 11, 0.3)',
      borderWidth: 1,
      cornerRadius: 8,
      padding: 10,
    },
  },
}

const topMistakes = computed(() => mistakeStore.getTopMistakes(5))
const achievements = computed(() => achievementStore.getAllAchievements())

function achProgress(ach: ReturnType<typeof achievementStore.getAllAchievements>[0]): number {
  return Math.min(100, Math.round((ach.progress.currentValue / ach.condition.threshold) * 100))
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.stats-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 16px 60px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.title-deco {
  display: inline-block;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, transparent, $gold, transparent);
  border-radius: 1px;
}

// Overview cards
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 24px;
}

.ov-card {
  padding: 14px 8px;
  text-align: center;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 3px;
    background: linear-gradient(90deg, transparent, rgba($gold, 0.6), transparent);
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-glow-gold;
  }
}

.ov-icon { font-size: 24px; margin-bottom: 6px; }
.ov-value {
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, $gold-light 0%, $gold 50%, $gold-dark 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.ov-label { font-size: 11px; color: $text-muted; margin-top: 4px; letter-spacing: 0.5px; }

// Sections
.stats-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: $text-secondary;
  margin-bottom: 12px;

  .section-header & {
    margin-bottom: 0;
  }
}

.link-btn {
  background: rgba($gold, 0.1);
  border: 1px solid rgba($gold, 0.25);
  border-radius: 16px;
  color: $gold;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 5px 14px;
  transition: all 0.2s;
  &:hover {
    background: rgba($gold, 0.2);
    border-color: rgba($gold, 0.5);
    box-shadow: 0 0 12px rgba($gold, 0.15);
  }
}

// Charts
.chart-card {
  padding: 20px;
  background: linear-gradient(145deg, rgba(26, 31, 53, 0.9) 0%, rgba(17, 24, 39, 0.95) 100%);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.chart-half {
  padding: 18px 14px;
  text-align: center;
  background: linear-gradient(145deg, rgba(26, 31, 53, 0.9) 0%, rgba(17, 24, 39, 0.95) 100%);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 12px;
    background: radial-gradient(ellipse at center, rgba($gold, 0.03) 0%, transparent 70%);
    pointer-events: none;
  }
}

.chart-subtitle {
  font-size: 13px;
  font-weight: 700;
  color: $text-secondary;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}

// Doughnut center labels
.doughnut-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doughnut-center {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
}

.dc-value {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, $gold-light, $gold);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.dc-label {
  font-size: 10px;
  color: $text-muted;
  letter-spacing: 1px;
  text-transform: uppercase;
}

// Info cards (SRS & mistakes)
.info-card {
  padding: 16px 20px;
  background: linear-gradient(145deg, rgba(26, 31, 53, 0.9) 0%, rgba(17, 24, 39, 0.95) 100%);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
  font-size: 14px;
  color: $text-secondary;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  &:last-child { border-bottom: none; }
}

.due-alert { color: $fire-orange; }
.text-green { color: $life-green; }

.top-mistakes {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);

  h4 {
    font-size: 12px;
    color: $text-muted;
    margin-bottom: 6px;
  }
}

.mistake-item {
  padding: 5px 0;
  font-size: 13px;
}

.mistake-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.mistake-word { color: $text-primary; }
.mistake-count { color: $hp-red; font-weight: 600; }

.mistake-bar {
  height: 4px;
  width: 100%;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.mistake-bar__fill {
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, rgba($hp-red, 0.6), rgba($fire-orange, 0.8));
  transition: width 0.6s ease-out;
}

// Achievements
.achievement-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.ach-card {
  display: flex;
  gap: 10px;
  padding: 12px;
  opacity: 0.5;
  transition: opacity 0.3s;
  &.ach--unlocked {
    opacity: 1;
    border-color: rgba($gold, 0.4);
    box-shadow: 0 0 16px rgba($gold, 0.15), inset 0 0 20px rgba($gold, 0.05);
    animation: achGlow 3s ease-in-out infinite alternate;
  }
}

@keyframes achGlow {
  from { box-shadow: 0 0 12px rgba($gold, 0.1), inset 0 0 16px rgba($gold, 0.03); }
  to { box-shadow: 0 0 22px rgba($gold, 0.25), inset 0 0 28px rgba($gold, 0.08); }
}

.ach-icon { font-size: 28px; flex-shrink: 0; }
.ach-info { min-width: 0; }
.ach-name { font-size: 13px; font-weight: 700; color: $text-primary; }
.ach-desc { font-size: 11px; color: $text-muted; margin-top: 2px; }

.ach-prog {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ach-prog-text { font-size: 10px; color: $text-muted; white-space: nowrap; }

// Back button
.back-btn {
  display: block;
  margin: 20px auto 0;
  padding: 10px 28px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: rgba(255, 255, 255, 0.12); color: $text-primary; }
}
</style>
