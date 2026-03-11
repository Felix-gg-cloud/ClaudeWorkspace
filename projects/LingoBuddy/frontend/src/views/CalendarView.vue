<template>
  <div class="page-container">
    <div class="cal-header">
      <h1 class="cal-title">📅 打卡日历</h1>
    </div>

    <!-- Streak Banner -->
    <div class="card streak-card">
      <div class="streak-row">
        <div>
          <div class="streak-value">🔥 {{ currentStreak }}</div>
          <div class="streak-label">连续打卡天数</div>
        </div>
        <button
          class="btn-primary checkin-action-btn"
          :disabled="todayChecked || checkinLoading"
          @click="doCheckin"
        >
          {{ todayChecked ? '✅ 已打卡' : '🎯 立即打卡' }}
        </button>
      </div>
    </div>

    <!-- Checkin result popup -->
    <div v-if="checkinResult" class="card checkin-result animate__animated animate__bounceIn">
      <span class="result-emoji">🎉</span>
      打卡成功！获得 {{ checkinResult.xpEarned }} XP + {{ checkinResult.coinsEarned }} 🪙
    </div>

    <!-- Month Navigation -->
    <div class="month-nav">
      <button class="month-btn" @click="prevMonth">‹</button>
      <span class="month-label">{{ year }}年{{ month }}月</span>
      <button class="month-btn" @click="nextMonth">›</button>
    </div>

    <!-- Calendar Grid -->
    <div class="cal-grid">
      <div class="cal-weekday" v-for="w in weekdays" :key="w">{{ w }}</div>
      <div
        v-for="(day, i) in calenderDays"
        :key="i"
        class="checkin-cell"
        :class="{
          checked: day.checked,
          today: day.isToday,
          empty: !day.date,
          'other-month': day.otherMonth
        }"
      >
        <template v-if="day.date">{{ day.dayNum }}</template>
      </div>
    </div>

    <!-- Stats -->
    <div class="card cal-stats">
      <div class="cal-stat-item">
        <span class="cal-stat-val">{{ monthCheckins }}</span>
        <span class="cal-stat-label">本月打卡</span>
      </div>
      <div class="cal-stat-item">
        <span class="cal-stat-val">{{ totalCheckins }}</span>
        <span class="cal-stat-label">累计打卡</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/api'
import { useUserStore } from '@/stores/user'
import type { CheckinDto, CheckinResult } from '@/types'

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

const calenderDays = computed(() => {
  const firstDay = new Date(year.value, month.value - 1, 1)
  const lastDay = new Date(year.value, month.value, 0)
  const startOffset = firstDay.getDay()

  const days: Array<{ date: string; dayNum: number; checked: boolean; isToday: boolean; otherMonth: boolean }> = []

  // Previous month padding
  for (let i = 0; i < startOffset; i++) {
    days.push({ date: '', dayNum: 0, checked: false, isToday: false, otherMonth: true })
  }

  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  // Current month days
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
  padding: 24px 0 8px;
}

.cal-title {
  font-size: 24px;
  font-weight: 800;
}

.streak-card {
  margin-bottom: 16px;
}

.streak-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.streak-value {
  font-size: 28px;
  font-weight: 800;
}

.streak-label {
  font-size: 13px;
  color: #718096;
  margin-top: 2px;
}

.checkin-action-btn {
  padding: 10px 20px;
  font-size: 14px;

  &:disabled {
    background: #e2e8f0;
    color: #a0aec0;
    box-shadow: none;
  }
}

.checkin-result {
  text-align: center;
  padding: 14px;
  margin-bottom: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #f0fff4, #e6ffed);
  border: 1px solid #9ae6b4;
  color: #276749;
}

.result-emoji {
  font-size: 18px;
  margin-right: 4px;
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
}

.month-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background: white;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover { background: #f7fafc; }
}

.month-label {
  font-size: 16px;
  font-weight: 700;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-bottom: 20px;
}

.cal-weekday {
  text-align: center;
  font-size: 12px;
  color: #a0aec0;
  font-weight: 600;
  padding: 4px 0;
}

.checkin-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;

  &.empty { visibility: hidden; }
  &.other-month { color: #cbd5e0; }

  &.checked {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: white;
    font-weight: 700;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  }

  &.today:not(.checked) {
    border: 2px solid #667eea;
    color: #667eea;
    font-weight: 700;
  }
}

.cal-stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.cal-stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cal-stat-val {
  font-size: 24px;
  font-weight: 800;
  color: #667eea;
}

.cal-stat-label {
  font-size: 13px;
  color: #a0aec0;
}
</style>
