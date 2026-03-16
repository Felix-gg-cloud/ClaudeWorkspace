<template>
  <MainLayout>
    <div class="quest-map">
      <!-- Chapter header -->
      <section class="chapter-header dark-panel">
        <div class="chapter-info">
          <span class="chapter-badge">{{ chapter.cefrLevel }}</span>
          <h2 class="chapter-title text-gold">{{ chapter.titleZh }}</h2>
          <p class="chapter-sub">{{ chapter.titleEn }}</p>
        </div>
        <div class="chapter-progress">
          <div class="progress-ring">
            <svg viewBox="0 0 80 80">
              <circle cx="40" cy="40" r="34" class="ring-bg" />
              <circle cx="40" cy="40" r="34" class="ring-fill" :stroke-dasharray="ringDash" />
            </svg>
            <span class="ring-text">{{ completedDays }}/{{ lessons.length }}</span>
          </div>
        </div>
      </section>

      <!-- Map path (vertical dungeon corridor) -->
      <section class="map-path">
        <div class="path-line"></div>

        <div
          v-for="(lesson, idx) in lessons"
          :key="lesson.code"
          class="map-node"
          :class="{
            'node--done': lesson.completed,
            'node--current': lesson.current,
            'node--locked': !lesson.completed && !lesson.current && idx > firstUnlockedIdx,
            'node--boss': idx === chapter.bossDayIndex - 1
          }"
          @click="enterDay(lesson, idx)"
        >
          <!-- Connector dot -->
          <div class="node-dot">
            <span v-if="lesson.completed" class="dot-icon">✓</span>
            <span v-else-if="idx === chapter.bossDayIndex - 1" class="dot-icon">💀</span>
            <span v-else class="dot-icon">{{ idx + 1 }}</span>
          </div>

          <!-- Node card -->
          <div class="node-card">
            <div class="node-card__top">
              <h3 class="node-title">{{ lesson.titleZh }}</h3>
              <span v-if="lesson.completed" class="node-status status-done">已完成</span>
              <span v-else-if="lesson.current" class="node-status status-current">进行中</span>
              <span v-else class="node-status status-locked">🔒</span>
            </div>
            <p class="node-sub">{{ lesson.titleEn }}</p>
            <div class="node-meta">
              <span>📝 {{ lesson.targetTaskCount }} 题</span>
              <span>⏱ ~{{ lesson.estimatedMinutes }} 分钟</span>
            </div>
            <!-- Progress bar for current day -->
            <div v-if="lesson.current" class="node-progress">
              <div class="node-progress-bar">
                <div class="node-progress-fill" :style="{ width: dayProgress + '%' }"></div>
              </div>
              <span class="node-progress-text">{{ dayProgress }}%</span>
            </div>
          </div>

          <!-- Character marker on current -->
          <div v-if="lesson.current" class="node-char">🧙</div>
        </div>
      </section>

      <!-- Bottom action -->
      <section class="map-action">
        <button class="btn-gold btn-start" @click="enterCurrentDay">
          ⚔️ 继续冒险
        </button>
      </section>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useChapterStore } from '@/stores/chapter'
import type { Lesson } from '@/types'

const router = useRouter()
const chapterStore = useChapterStore()

onMounted(async () => {
  await chapterStore.loadChapters()
  await chapterStore.loadLessons(chapterStore.currentChapterCode)
})

const chapter = computed(() => ({
  code: chapterStore.currentChapter.code,
  cefrLevel: chapterStore.currentChapter.cefrLevel,
  titleEn: chapterStore.currentChapter.titleEn,
  titleZh: chapterStore.currentChapter.titleZh,
  orderIndex: chapterStore.currentChapter.orderIndex,
  days: chapterStore.currentChapter.days,
  bossDayIndex: chapterStore.currentChapter.days,
}))

const lessons = computed(() => {
  const raw = chapterStore.currentLessons
  const prog = chapterStore.currentProgress
  const completedDays = prog?.questDaysCompleted ?? []
  // Compute current/completed status from chapter progress
  let foundCurrent = false
  return raw.map(l => {
    const done = completedDays.includes(l.dayIndex)
    const isCurrent = !done && !foundCurrent
    if (isCurrent) foundCurrent = true
    return { ...l, completed: done, current: isCurrent }
  })
})

const completedDays = computed(() => lessons.value.filter(l => l.completed).length)

const firstUnlockedIdx = computed(() => {
  const currentIdx = lessons.value.findIndex(l => l.current)
  return currentIdx >= 0 ? currentIdx : 0
})

const dayProgress = computed(() => {
  // Mock: 30% progress on current day
  return 30
})

const ringCircumference = 2 * Math.PI * 34
const ringDash = computed(() => {
  const pct = completedDays.value / lessons.value.length
  return `${pct * ringCircumference} ${ringCircumference}`
})

function enterDay(lesson: Lesson, idx: number) {
  if (lesson.completed || lesson.current) {
    router.push('/quest/today')
  }
}

function enterCurrentDay() {
  router.push('/quest/today')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.quest-map {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

// Chapter header
.chapter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  margin-bottom: 32px;
}

.chapter-info {
  flex: 1;
}

.chapter-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 800;
  background: rgba($gold, 0.12);
  color: $gold;
  border: 1px solid rgba($gold, 0.25);
  margin-bottom: 8px;
}

.chapter-title {
  font-size: 20px;
  font-weight: 800;
  margin-bottom: 4px;
}

.chapter-sub {
  font-size: 13px;
  color: $text-muted;
}

.progress-ring {
  position: relative;
  width: 70px;
  height: 70px;

  svg {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }
}

.ring-bg {
  fill: none;
  stroke: $border-dim;
  stroke-width: 5;
}

.ring-fill {
  fill: none;
  stroke: $gold;
  stroke-width: 5;
  stroke-linecap: round;
  transition: stroke-dasharray 0.6s ease;
}

.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  color: $text-primary;
}

// Map path
.map-path {
  position: relative;
  padding-left: 36px;
}

.path-line {
  position: absolute;
  left: 18px;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, $gold 0%, $border-dim 50%, $border-dim 100%);
  border-radius: 2px;
}

.map-node {
  position: relative;
  margin-bottom: 20px;
  cursor: pointer;

  &:last-child { margin-bottom: 0; }
}

.node-dot {
  position: absolute;
  left: -36px;
  top: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  z-index: 2;
  background: $bg-dark;
  border: 3px solid $border-dim;
  color: $text-muted;
  transition: all 0.3s;
}

.node--done .node-dot {
  background: $life-green;
  border-color: $life-green;
  color: #fff;
}

.node--current .node-dot {
  background: $gold;
  border-color: $gold;
  color: $bg-abyss;
  box-shadow: $shadow-glow-gold;
  animation: pulse-glow 2s infinite;
}

.node--boss .node-dot {
  border-color: $hp-red;
  font-size: 14px;
}

.node--locked {
  opacity: 0.4;
  pointer-events: none;
}

.node-card {
  background: $glass-bg;
  border: 1px solid $glass-border;
  border-radius: $radius-lg;
  padding: 16px 20px;
  transition: all 0.25s;
}

.node--current .node-card {
  border-color: rgba($gold, 0.3);
  box-shadow: 0 0 16px rgba($gold, 0.08);
}

.node--done .node-card {
  border-color: rgba($life-green, 0.2);
}

.map-node:hover:not(.node--locked) .node-card {
  transform: translateX(4px);
  border-color: rgba($gold, 0.25);
}

.node-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.node-title {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
}

.node-status {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 8px;
}

.status-done {
  background: rgba($life-green, 0.12);
  color: $life-green;
}

.status-current {
  background: rgba($gold, 0.12);
  color: $gold;
}

.status-locked {
  font-size: 13px;
}

.node-sub {
  font-size: 12px;
  color: $text-muted;
  margin-bottom: 8px;
}

.node-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: $text-muted;
}

.node-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.node-progress-bar {
  flex: 1;
  height: 6px;
  background: $border-dim;
  border-radius: 3px;
  overflow: hidden;
}

.node-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $gold, $gold-light);
  border-radius: 3px;
  transition: width 0.4s;
}

.node-progress-text {
  font-size: 11px;
  font-weight: 700;
  color: $gold;
  min-width: 32px;
  text-align: right;
}

.node-char {
  position: absolute;
  left: -56px;
  top: 8px;
  font-size: 24px;
  animation: float 2s ease-in-out infinite;
  filter: drop-shadow(0 0 8px rgba(245, 158, 11, 0.4));
}

// Bottom action
.map-action {
  text-align: center;
  margin-top: 32px;
}

.btn-start {
  padding: 14px 48px;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 1px;
}

// Animations
@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 8px rgba(245, 158, 11, 0.2); }
  50% { box-shadow: 0 0 20px rgba(245, 158, 11, 0.5); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
</style>
