<template>
  <div class="stats-page">
    <h1>学习统计</h1>

    <!-- 总览卡片 -->
    <div class="overview-cards">
      <div class="ov-card">
        <div class="ov-card__icon streak"><AppIcon name="zap" :size="22" /></div>
        <div class="ov-card__value">{{ streak.streak }}</div>
        <div class="ov-card__label">连续学习(天)</div>
      </div>
      <div class="ov-card">
        <div class="ov-card__icon total"><AppIcon name="bar-chart-2" :size="22" /></div>
        <div class="ov-card__value">{{ streak.totalDays }}</div>
        <div class="ov-card__label">总学习天数</div>
      </div>
      <div class="ov-card">
        <div class="ov-card__icon correct"><AppIcon name="check-circle" :size="22" /></div>
        <div class="ov-card__value">{{ today.correctCount }}</div>
        <div class="ov-card__label">今日正确</div>
      </div>
      <div class="ov-card">
        <div class="ov-card__icon tasks"><AppIcon name="pen-line" :size="22" /></div>
        <div class="ov-card__value">{{ today.tasksCompleted }}</div>
        <div class="ov-card__label">今日练习次数</div>
      </div>
    </div>

    <!-- 时间范围切换 -->
    <div class="range-toggle">
      <button :class="{ active: rangeDays === 7 }" @click="switchRange(7)">近 7 天</button>
      <button :class="{ active: rangeDays === 30 }" @click="switchRange(30)">近 30 天</button>
    </div>

    <!-- 每日练习量 -->
    <div class="chart-section">
      <h3>每日练习量</h3>
      <div class="bar-chart">
        <div class="bar-chart__bars">
          <div v-for="(d, i) in rangeData" :key="i" class="bar-col">
            <div class="bar-wrapper">
              <div
                class="bar"
                :style="{ height: barHeight(d.correctCount + d.wrongCount) + '%' }"
                :title="`${d.date}: ${d.correctCount + d.wrongCount} 题`"
              />
            </div>
            <span class="bar-label">{{ shortDate(d.date) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 正确率趋势 -->
    <div class="chart-section">
      <h3>正确率趋势</h3>
      <div class="line-chart">
        <svg viewBox="0 0 700 200" preserveAspectRatio="none" class="line-svg">
          <polyline
            :points="accuracyPoints"
            fill="none"
            stroke="#10B981"
            stroke-width="2"
            stroke-linejoin="round"
          />
          <circle
            v-for="(pt, i) in accuracyCircles"
            :key="i"
            :cx="pt.x" :cy="pt.y" r="3"
            fill="#10B981"
          />
        </svg>
        <div class="line-chart__labels">
          <span v-for="(d, i) in rangeData" :key="i" :style="{ left: (i / Math.max(rangeData.length - 1, 1)) * 100 + '%' }">
            {{ shortDate(d.date) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 今日明细 -->
    <div class="chart-section">
      <h3>今日学习明细</h3>
      <div class="today-detail">
        <div class="detail-row">
          <span class="detail-label">练习次数</span>
          <span class="detail-value">{{ today.tasksCompleted }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">正确题数</span>
          <span class="detail-value correct">{{ today.correctCount }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">错误题数</span>
          <span class="detail-value wrong">{{ today.wrongCount }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">学习单词</span>
          <span class="detail-value">{{ today.wordsLearned }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">学习时长</span>
          <span class="detail-value">{{ today.studyMinutes }} 分钟</span>
        </div>
      </div>
    </div>

    <!-- 空数据提示 -->
    <div v-if="!loading && noData" class="empty-hint">
      <AppIcon name="bar-chart-2" :size="40" />
      <p>暂无学习数据，去做几道题吧</p>
      <button class="btn-go" @click="router.push('/practice')">
        <AppIcon name="pen-line" :size="14" /> 去练习
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { statsApi, type TodayStats, type DayStats, type StreakInfo } from '@/api/stats'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()

const loading = ref(true)
const today = ref<TodayStats>({ date: '', tasksCompleted: 0, correctCount: 0, wrongCount: 0, wordsLearned: 0, studyMinutes: 0 })
const streak = ref<StreakInfo>({ streak: 0, totalDays: 0 })
const rangeData = ref<DayStats[]>([])
const rangeDays = ref(7)

const noData = computed(() => streak.value.totalDays === 0 && today.value.tasksCompleted === 0)

const maxPracticeCount = computed(() => {
  return Math.max(1, ...rangeData.value.map(d => d.correctCount + d.wrongCount))
})

function barHeight(count: number) {
  return Math.max(4, (count / maxPracticeCount.value) * 100)
}

function shortDate(dateStr: string) {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  return `${parseInt(parts[1])}/${parseInt(parts[2])}`
}

const accuracyPoints = computed(() => {
  if (rangeData.value.length === 0) return ''
  const w = 700, h = 200, pad = 10
  return rangeData.value.map((d, i) => {
    const x = rangeData.value.length === 1 ? w / 2 : (i / (rangeData.value.length - 1)) * (w - pad * 2) + pad
    const total = d.correctCount + d.wrongCount
    const acc = total > 0 ? d.correctCount / total : 0
    const y = h - pad - acc * (h - pad * 2)
    return `${x},${y}`
  }).join(' ')
})

const accuracyCircles = computed(() => {
  if (rangeData.value.length === 0) return []
  const w = 700, h = 200, pad = 10
  return rangeData.value.map((d, i) => {
    const x = rangeData.value.length === 1 ? w / 2 : (i / (rangeData.value.length - 1)) * (w - pad * 2) + pad
    const total = d.correctCount + d.wrongCount
    const acc = total > 0 ? d.correctCount / total : 0
    const y = h - pad - acc * (h - pad * 2)
    return { x, y }
  })
})

async function loadAll() {
  loading.value = true
  try {
    const [todayRes, streakRes] = await Promise.all([statsApi.getToday(), statsApi.getStreak()])
    today.value = todayRes.data.data
    streak.value = streakRes.data.data
    await loadRange()
  } catch (e) {
    console.error('加载统计失败', e)
  } finally {
    loading.value = false
  }
}

async function loadRange() {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - rangeDays.value + 1)
  const fmt = (d: Date) => d.toISOString().split('T')[0]
  try {
    const res = await statsApi.getRange(fmt(from), fmt(to))
    rangeData.value = res.data.data
  } catch (e) {
    console.error('加载范围统计失败', e)
  }
}

function switchRange(days: number) {
  rangeDays.value = days
  loadRange()
}

onMounted(loadAll)
</script>

<style lang="scss" scoped>
.stats-page {
  max-width: 960px;
  margin: 0 auto;
  animation: fadeUp 0.4s ease-out;

  h1 {
    font-size: 24px;
    font-weight: 800;
    color: var(--text-primary, #0F172A);
    margin-bottom: 24px;
  }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 28px;
}

.ov-card {
  text-align: center;
  padding: 22px 12px;
  border-radius: 18px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
  transition: all 0.2s;

  &:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06); }

  &__icon {
    width: 48px; height: 48px;
    display: flex; align-items: center; justify-content: center;
    border-radius: 14px;
    margin: 0 auto 12px;

    &.streak { background: rgba(245, 158, 11, 0.1); color: #F59E0B; }
    &.total { background: rgba(99, 102, 241, 0.1); color: #6366F1; }
    &.correct { background: rgba(16, 185, 129, 0.1); color: #10B981; }
    &.tasks { background: rgba(236, 72, 153, 0.1); color: #EC4899; }
  }
  &__value { font-size: 28px; font-weight: 800; color: var(--text-primary, #0F172A); }
  &__label { font-size: 12px; color: var(--text-muted, #94A3B8); margin-top: 4px; }
}

.range-toggle {
  display: flex; gap: 8px; margin-bottom: 20px;
  button {
    padding: 7px 18px; border-radius: 10px;
    background: var(--bg-card, #fff);
    border: 1px solid var(--border, #E2E8F0);
    color: var(--text-secondary, #64748B); font-size: 13px; font-weight: 500; cursor: pointer;
    transition: all 0.2s;
    &.active {
      background: rgba(99, 102, 241, 0.1);
      border-color: rgba(99, 102, 241, 0.3);
      color: #6366F1;
      font-weight: 600;
    }
    &:hover:not(.active) { border-color: var(--text-muted, #94A3B8); }
  }
}

.chart-section {
  margin-bottom: 24px;
  padding: 24px;
  border-radius: 18px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);

  h3 {
    font-size: 15px;
    font-weight: 700;
    color: var(--text-primary, #0F172A);
    margin-bottom: 16px;
  }
}

// Bar chart
.bar-chart__bars {
  display: flex; gap: 4px; height: 140px; align-items: flex-end;
}
.bar-col {
  flex: 1;
  display: flex; flex-direction: column; align-items: center;
}
.bar-wrapper {
  flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center;
}
.bar {
  width: 60%; max-width: 24px;
  background: linear-gradient(180deg, #6366F1, #4F46E5);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  transition: height 0.5s;
}
.bar-label {
  font-size: 10px; color: var(--text-muted, #94A3B8); margin-top: 6px; white-space: nowrap;
}

// Line chart
.line-chart { position: relative; }
.line-svg { width: 100%; height: 160px; }
.line-chart__labels {
  position: relative; height: 20px; margin-top: 4px;
  span {
    position: absolute; transform: translateX(-50%);
    font-size: 10px; color: var(--text-muted, #94A3B8); white-space: nowrap;
  }
}

// Today detail
.today-detail {
  display: flex; flex-direction: column; gap: 4px;
}
.detail-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border, #E2E8F0);
  &:last-child { border-bottom: none; }
}
.detail-label { font-size: 14px; color: var(--text-secondary, #64748B); }
.detail-value { font-size: 14px; font-weight: 700; color: var(--text-primary, #0F172A); &.correct { color: #10B981; } &.wrong { color: #EF4444; } }

.empty-hint {
  text-align: center; padding: 48px 20px;
  color: var(--text-muted, #94A3B8);
  p { margin: 12px 0 16px; }
}
.btn-go {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 22px; border-radius: 12px;
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: #fff; border: none; font-size: 14px; font-weight: 600; cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  &:hover { transform: translateY(-2px); }
}

@media (max-width: 640px) {
  .overview-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
