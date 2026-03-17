<template>
  <MainLayout>
    <div class="stats-page">
      <h2 class="page-title">📊 学习统计</h2>

      <!-- 总览卡片 -->
      <section class="overview-cards">
        <div class="ov-card dark-panel">
          <div class="ov-icon">⭐</div>
          <div class="ov-value">{{ userStore.totalXp }}</div>
          <div class="ov-label">经验值</div>
        </div>
        <div class="ov-card dark-panel">
          <div class="ov-icon">💰</div>
          <div class="ov-value">{{ userStore.coins }}</div>
          <div class="ov-label">金币</div>
        </div>
        <div class="ov-card dark-panel">
          <div class="ov-icon">📅</div>
          <div class="ov-value">{{ dailyStatsStore.studyDays }}</div>
          <div class="ov-label">学习天数</div>
        </div>
        <div class="ov-card dark-panel">
          <div class="ov-icon">🎯</div>
          <div class="ov-value">{{ dailyStatsStore.totalAccuracy }}%</div>
          <div class="ov-label">正确率</div>
        </div>
      </section>

      <!-- 7天学习趋势 -->
      <section class="stats-section">
        <h3 class="section-title">📈 近7天学习趋势</h3>
        <div class="chart-card dark-panel">
          <Bar :data="weeklyChartData" :options="barOptions" />
        </div>
      </section>

      <!-- 正确率 + 词汇掌握 双图表 -->
      <section class="stats-section">
        <div class="chart-row">
          <div class="chart-half dark-panel">
            <h4 class="chart-subtitle">正确率分布</h4>
            <Doughnut :data="accuracyChartData" :options="doughnutOptions" />
          </div>
          <div class="chart-half dark-panel">
            <h4 class="chart-subtitle">词汇掌握度</h4>
            <Doughnut :data="masteryChartData" :options="doughnutOptions" />
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
              <span class="mistake-word">{{ wordLabel(m.wordCode) }}</span>
              <span class="mistake-count">× {{ m.count }}</span>
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
import { computed, onMounted } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { Bar, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale,
  BarElement, ArcElement,
  Title, Tooltip, Legend,
} from 'chart.js'
import { useSrsStore } from '@/stores/srs'
import { useMistakeStore } from '@/stores/mistakes'
import { useAchievementStore } from '@/stores/achievements'
import { useUserStore } from '@/stores/user'
import { useDailyStatsStore } from '@/stores/dailyStats'
import { chapterConfigs } from '@/mock/chapters'
import type { ContentItem } from '@/types'

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend)

const srsStore = useSrsStore()
const mistakeStore = useMistakeStore()
const achievementStore = useAchievementStore()
const userStore = useUserStore()
const dailyStatsStore = useDailyStatsStore()

onMounted(async () => {
  await Promise.all([
    srsStore.loadAll(),
    achievementStore.loadAchievements(),
  ])
})

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

// 7-day bar chart
const weeklyChartData = computed(() => {
  const days = dailyStatsStore.getRecentDays(7)
  return {
    labels: days.map(d => d.date.slice(5)), // MM-DD
    datasets: [
      {
        label: '正确',
        data: days.map(d => d.correctCount),
        backgroundColor: 'rgba(16, 185, 129, 0.7)',
        borderRadius: 4,
      },
      {
        label: '错误',
        data: days.map(d => d.wrongCount),
        backgroundColor: 'rgba(220, 38, 38, 0.7)',
        borderRadius: 4,
      },
    ],
  }
})

const barOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: {
    legend: { labels: { color: '#9ca3af', font: { size: 11 } } },
  },
  scales: {
    x: {
      ticks: { color: '#6b7280', font: { size: 11 } },
      grid: { color: 'rgba(255,255,255,0.04)' },
    },
    y: {
      beginAtZero: true,
      ticks: { color: '#6b7280', font: { size: 11 }, stepSize: 1 },
      grid: { color: 'rgba(255,255,255,0.04)' },
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
      backgroundColor: ['rgba(16, 185, 129, 0.8)', 'rgba(220, 38, 38, 0.8)'],
      borderWidth: 0,
    }],
  }
})

// Mastery doughnut
const masteryChartData = computed(() => ({
  labels: ['已掌握', '复习中', '新学'],
  datasets: [{
    data: [masteredCount.value, reviewingCount.value, newWordCount.value],
    backgroundColor: ['rgba(16, 185, 129, 0.8)', 'rgba(245, 158, 11, 0.8)', 'rgba(220, 38, 38, 0.8)'],
    borderWidth: 0,
  }],
}))

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: {
    legend: {
      position: 'bottom' as const,
      labels: { color: '#9ca3af', font: { size: 11 }, padding: 12 },
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
}

// Overview cards
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 24px;
}

.ov-card {
  padding: 12px 8px;
  text-align: center;
}

.ov-icon { font-size: 22px; margin-bottom: 4px; }
.ov-value { font-size: 20px; font-weight: 800; color: $gold; }
.ov-label { font-size: 11px; color: $text-muted; margin-top: 2px; }

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
  background: none;
  border: none;
  color: $gold;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  &:hover { text-decoration: underline; }
}

// Charts
.chart-card {
  padding: 16px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.chart-half {
  padding: 14px;
  text-align: center;
}

.chart-subtitle {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 10px;
}

// Info cards (SRS & mistakes)
.info-card {
  padding: 14px 18px;
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
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 13px;
}

.mistake-word { color: $text-primary; }
.mistake-count { color: $hp-red; font-weight: 600; }

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
  &.ach--unlocked { opacity: 1; border-color: rgba($gold, 0.4); }
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
