<template>
  <div class="page-container">
    <div class="cal-header">
      <h1 class="cal-title">
        <span class="title-icon">📅</span>
        <span class="text-gradient">打卡日历</span>
      </h1>
    </div>

    <!-- Streak Banner -->
    <div class="glass-card streak-card">
      <div class="streak-row">
        <div class="streak-info">
          <div class="streak-num">
            <span class="fire-icon">🔥</span>
            <span class="text-gradient-warm streak-val">{{ currentStreak }}</span>
          </div>
          <div class="streak-label">连续打卡天数</div>
        </div>
        <button
          class="btn-primary checkin-btn"
          :class="{ checked: todayChecked }"
          :disabled="todayChecked || checkinLoading"
          @click="doCheckin"
        >
          <span v-if="checkinLoading" class="loading-dots"><i></i><i></i><i></i></span>
          <span v-else>{{ todayChecked ? '✅ 已打卡' : '🎯 立即打卡' }}</span>
        </button>
      </div>
    </div>

    <!-- Checkin result -->
    <Transition name="page-slide">
      <div v-if="checkinResult" class="glass-card result-popup">
        <span>🎉</span> 打卡成功！+{{ checkinResult.xpEarned }} XP · +{{ checkinResult.coinsEarned }} 💎
      </div>
    </Transition>

    <!-- Month Nav -->
    <div class="month-nav">
      <button class="nav-btn" @click="prevMonth">‹</button>
      <span class="month-label">{{ year }}年{{ month }}月</span>
      <button class="nav-btn" @click="nextMonth">›</button>
    </div>

    <!-- Calendar Grid -->
    <div class="cal-grid">
      <div class="weekday" v-for="w in weekdays" :key="w">{{ w }}</div>
      <div
        v-for="(day, i) in calendarDays"
        :key="i"
        class="day-cell"
        :class="{
          checked: day.checked,
          today: day.isToday,
          empty: !day.date,
          other: day.otherMonth
        }"
      >
        <template v-if="day.date">
          <span class="day-num">{{ day.dayNum }}</span>
          <span v-if="day.checked" class="day-dot"></span>
        </template>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="glass-card stat-box">
        <div class="stat-val text-gradient">{{ monthCheckins }}</div>
        <div class="stat-label">本月打卡</div>
      </div>
      <div class="glass-card stat-box">
        <div class="stat-val text-gradient-warm">{{ totalCheckins }}</div>
        <div class="stat-label">累计打卡</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/api'
import { useUserStore } from '@/stores/user'
import type { CheckinDto } from '@/types'

interface CheckinResult { date: string; xpEarned: number; coinsEarned: number; streak: number }

const userStore = useUserStore()
const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const checkinDates = ref<Set<string>>(new Set())
const todayChecked = ref(false)
const checkinLoading = ref(false)
const checkinResult = ref<CheckinResult | null>(null)
const currentStreak = ref(0)
const totalCheckins = ref(0)

const monthCheckins = computed(() => {
  let count = 0
  checkinDates.value.forEach(d => {
    if (d.startsWith(`${year.value}-${String(month.value).padStart(2, '0')}`)) count++
  })
  return count
})

const calendarDays = computed(() => {
  const firstDay = new Date(year.value, month.value - 1, 1)
  const lastDay = new Date(year.value, month.value, 0)
  const startOffset = firstDay.getDay()

  const days: Array<{ date: string; dayNum: number; checked: boolean; isToday: boolean; otherMonth: boolean }> = []

  for (let i = 0; i < startOffset; i++) {
    days.push({ date: '', dayNum: 0, checked: false, isToday: false, otherMonth: true })
  }

  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dateStr = `${year.value}-${String(month.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({
      date: dateStr,
      dayNum: d,
      checked: checkinDates.value.has(dateStr),
      isToday: dateStr === todayStr,
      otherMonth: false
    })
  }

  return days
})

async function loadCalendar() {
  try {
    const { data } = await api.get<CheckinDto[]>('/checkin/calendar', {
      params: { year: year.value, month: month.value }
    })
    const dates = new Set<string>()
    data.forEach(d => dates.add(d.date))
    checkinDates.value = dates

    const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    todayChecked.value = dates.has(todayStr)
  } catch (e) {
    console.error(e)
  }
}

async function doCheckin() {
  checkinLoading.value = true
  try {
    const { data } = await api.post<CheckinResult>('/checkin/today')
    checkinResult.value = data
    todayChecked.value = true
    currentStreak.value = data.streak

    const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    checkinDates.value.add(todayStr)

    await userStore.fetchMe()
    totalCheckins.value = userStore.user?.totalCheckins || 0

    setTimeout(() => { checkinResult.value = null }, 3000)
  } catch (e) {
    console.error(e)
  } finally {
    checkinLoading.value = false
  }
}

function prevMonth() {
  if (month.value === 1) { year.value--; month.value = 12 }
  else month.value--
}

function nextMonth() {
  if (month.value === 12) { year.value++; month.value = 1 }
  else month.value++
}

watch([year, month], () => loadCalendar())

onMounted(async () => {
  await loadCalendar()
  currentStreak.value = userStore.user?.streak || 0
  totalCheckins.value = userStore.user?.totalCheckins || 0
})
</script>

<style scoped lang="scss">
.cal-header {
  padding: 28px 0 12px;
}

.cal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 800;
}

.title-icon { font-size: 26px; }

// Streak
.streak-card { margin-bottom: 12px; }

.streak-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.streak-num {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fire-icon {
  font-size: 28px;
  animation: pulse-glow 2s infinite;
}

.streak-val {
  font-size: 36px;
  font-weight: 800;
}

.streak-label {
  font-size: 13px;
  color: #5a6480;
  margin-top: 2px;
}

.checkin-btn {
  padding: 12px 22px;
  font-size: 14px;
  border-radius: 14px;

  &.checked {
    background: rgba(16, 185, 129, 0.15);
    color: #10b981;
    border: 1px solid rgba(16, 185, 129, 0.2);
    box-shadow: none;
    cursor: default;

    &:hover { transform: none; box-shadow: none; }
  }
}

.loading-dots {
  display: flex;
  gap: 4px;
  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: white;
    animation: typing-dots 1.2s infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

// Result popup
.result-popup {
  text-align: center;
  padding: 14px 20px;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 14px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #10b981;
}

// Month nav
.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 28px;
  margin-bottom: 16px;
}

.nav-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: #8b95b0;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: #e8ecf4;
  }
}

.month-label {
  font-size: 16px;
  font-weight: 700;
  color: #e8ecf4;
}

// Calendar grid
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 20px;
}

.weekday {
  text-align: center;
  font-size: 11px;
  color: #5a6480;
  font-weight: 600;
  padding: 6px 0;
}

.day-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #8b95b0;
  position: relative;
  transition: all 0.2s;
  gap: 2px;

  &.empty { visibility: hidden; }
  &.other { color: #2a3050; }

  &.checked {
    background: linear-gradient(135deg, rgba(0, 212, 255, 0.15), rgba(168, 85, 247, 0.15));
    color: #e8ecf4;
    font-weight: 700;
    border: 1px solid rgba(0, 212, 255, 0.2);
  }

  &.today:not(.checked) {
    border: 1px solid rgba(0, 212, 255, 0.4);
    color: #00d4ff;
    font-weight: 700;
    box-shadow: 0 0 12px rgba(0, 212, 255, 0.15);
  }
}

.day-num { font-size: 13px; }

.day-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 6px rgba(0, 212, 255, 0.5);
}

// Stats
.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stat-box {
  text-align: center;
  padding: 20px 16px;
}

.stat-val {
  font-size: 28px;
  font-weight: 800;
}

.stat-label {
  font-size: 12px;
  color: #5a6480;
  margin-top: 4px;
}
</style>
