<template>
  <MainLayout>
    <div class="stats-page">
      <h2 class="page-title">📊 学习统计</h2>

      <!-- 今日概览 -->
      <section class="stats-section">
        <h3 class="section-title">🎯 今日目标</h3>
        <div class="today-card dark-panel">
          <div class="goal-row">
            <span class="goal-label">经验值</span>
            <div class="goal-bar-wrap">
              <div class="goal-bar">
                <div class="goal-bar__fill" :style="{ width: dailyStore.todayXpProgress * 100 + '%' }"></div>
              </div>
              <span class="goal-text">{{ dailyStore.todayRecord.xpEarned }} / {{ dailyStore.config.targetXp }} XP</span>
            </div>
          </div>
          <div class="goal-row">
            <span class="goal-label">任务</span>
            <div class="goal-bar-wrap">
              <div class="goal-bar">
                <div class="goal-bar__fill goal-bar__fill--blue" :style="{ width: dailyStore.todayTaskProgress * 100 + '%' }"></div>
              </div>
              <span class="goal-text">{{ dailyStore.todayRecord.tasksCompleted }} / {{ dailyStore.config.targetTasks }}</span>
            </div>
          </div>
          <div class="goal-status" :class="{ 'goal-met': dailyStore.isGoalMet }">
            {{ dailyStore.isGoalMet ? '✅ 今日目标已达成！' : '💪 继续加油！' }}
          </div>
        </div>
      </section>

      <!-- 7天趋势 -->
      <section class="stats-section">
        <h3 class="section-title">📈 最近7天</h3>
        <div class="week-chart dark-panel">
          <div class="bar-chart">
            <div v-for="rec in weekRecords" :key="rec.date" class="bar-col">
              <div class="bar-value">{{ rec.xpEarned }}</div>
              <div class="bar-fill-wrap">
                <div class="bar-fill" :style="{ height: barHeight(rec.xpEarned) + '%' }"></div>
              </div>
              <div class="bar-label">{{ formatDay(rec.date) }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 统计汇总 -->
      <section class="stats-section">
        <h3 class="section-title">🏆 总计统计</h3>
        <div class="summary-grid">
          <div class="summary-card dark-panel">
            <div class="summary-icon">⭐</div>
            <div class="summary-value">{{ dailyStore.totalStats.totalXp }}</div>
            <div class="summary-label">总经验值</div>
          </div>
          <div class="summary-card dark-panel">
            <div class="summary-icon">✅</div>
            <div class="summary-value">{{ dailyStore.totalStats.totalTasks }}</div>
            <div class="summary-label">完成任务</div>
          </div>
          <div class="summary-card dark-panel">
            <div class="summary-icon">📖</div>
            <div class="summary-value">{{ dailyStore.totalStats.totalWords }}</div>
            <div class="summary-label">学习单词</div>
          </div>
          <div class="summary-card dark-panel">
            <div class="summary-icon">🔥</div>
            <div class="summary-value">{{ dailyStore.bestStreak }}</div>
            <div class="summary-label">最佳连续</div>
          </div>
        </div>
      </section>

      <!-- SRS 状态 -->
      <section class="stats-section">
        <h3 class="section-title">🧠 记忆系统</h3>
        <div class="srs-card dark-panel">
          <div class="srs-row">
            <span>SRS 词库总量</span>
            <strong>{{ srsStore.totalCards }} 词</strong>
          </div>
          <div class="srs-row">
            <span>今日待复习</span>
            <strong :class="{ 'due-alert': srsStore.dueCards.length > 0 }">{{ srsStore.dueCards.length }} 词</strong>
          </div>
        </div>
      </section>

      <!-- 错题统计 -->
      <section class="stats-section">
        <h3 class="section-title">❌ 错题本</h3>
        <div class="mistake-card dark-panel">
          <div class="srs-row">
            <span>错题总数</span>
            <strong>{{ mistakeStore.totalCount }}</strong>
          </div>
          <div class="srs-row">
            <span>待复习</span>
            <strong :class="{ 'due-alert': mistakeStore.unreviewedCount > 0 }">{{ mistakeStore.unreviewedCount }}</strong>
          </div>
          <div v-if="topMistakes.length > 0" class="top-mistakes">
            <h4>高频错词 TOP 5</h4>
            <div v-for="m in topMistakes" :key="m.wordCode" class="mistake-item">
              <span class="mistake-word">{{ m.wordCode }}</span>
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
import { computed } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useSrsStore } from '@/stores/srs'
import { useMistakeStore } from '@/stores/mistakes'
import { useAchievementStore } from '@/stores/achievements'
import { useDailyGoalStore } from '@/stores/dailyGoal'

const srsStore = useSrsStore()
const mistakeStore = useMistakeStore()
const achievementStore = useAchievementStore()
const dailyStore = useDailyGoalStore()

const weekRecords = computed(() => dailyStore.getRecentRecords(7).reverse())

const topMistakes = computed(() => mistakeStore.getTopMistakes(5))
const achievements = computed(() => achievementStore.getAllAchievements())

function barHeight(xp: number): number {
  const max = Math.max(...weekRecords.value.map(r => r.xpEarned), 1)
  return Math.round((xp / max) * 100)
}

function formatDay(date: string): string {
  const d = new Date(date + 'T00:00:00')
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return days[d.getDay()]
}

function achProgress(ach: ReturnType<typeof achievementStore.getAllAchievements>[0]): number {
  return Math.min(100, Math.round((ach.progress.currentValue / ach.condition.threshold) * 100))
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.stats-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
  margin-bottom: 24px;
}

.stats-section {
  margin-bottom: 28px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: $text-secondary;
  margin-bottom: 12px;
}

// Today goal
.today-card {
  padding: 20px;
}

.goal-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.goal-label {
  width: 50px;
  font-size: 13px;
  color: $text-secondary;
  flex-shrink: 0;
}

.goal-bar-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.goal-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.goal-bar__fill {
  height: 100%;
  background: linear-gradient(90deg, $gold, $gold-dark);
  border-radius: 4px;
  transition: width 0.4s ease;

  &--blue {
    background: linear-gradient(90deg, $magic-purple, #7c3aed);
  }
}

.goal-text {
  font-size: 12px;
  color: $text-muted;
  white-space: nowrap;
}

.goal-status {
  text-align: center;
  font-size: 14px;
  color: $text-secondary;
  padding-top: 4px;

  &.goal-met {
    color: $life-green;
    font-weight: 700;
  }
}

// Week chart
.week-chart {
  padding: 20px;
}

.bar-chart {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 120px;
  gap: 8px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.bar-value {
  font-size: 11px;
  color: $text-muted;
}

.bar-fill-wrap {
  width: 24px;
  height: 80px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  background: linear-gradient(180deg, $gold, $gold-dark);
  border-radius: 4px;
  transition: height 0.4s ease;
  min-height: 2px;
}

.bar-label {
  font-size: 11px;
  color: $text-muted;
}

// Summary grid
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.summary-card {
  padding: 16px;
  text-align: center;
}

.summary-icon {
  font-size: 28px;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 24px;
  font-weight: 800;
  color: $gold;
}

.summary-label {
  font-size: 12px;
  color: $text-muted;
  margin-top: 2px;
}

// SRS & mistakes
.srs-card, .mistake-card {
  padding: 16px 20px;
}

.srs-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: $text-secondary;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  &:last-child {
    border-bottom: none;
  }
}

.due-alert {
  color: $fire-orange;
}

.top-mistakes {
  margin-top: 12px;

  h4 {
    font-size: 13px;
    color: $text-muted;
    margin-bottom: 8px;
  }
}

.mistake-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}

.mistake-word {
  color: $text-primary;
}

.mistake-count {
  color: $hp-red;
  font-weight: 600;
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
  }
}

.ach-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.ach-info {
  min-width: 0;
}

.ach-name {
  font-size: 13px;
  font-weight: 700;
  color: $text-primary;
}

.ach-desc {
  font-size: 11px;
  color: $text-muted;
  margin-top: 2px;
}

.ach-prog {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ach-prog-text {
  font-size: 10px;
  color: $text-muted;
  white-space: nowrap;
}

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

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    color: $text-primary;
  }
}
</style>
