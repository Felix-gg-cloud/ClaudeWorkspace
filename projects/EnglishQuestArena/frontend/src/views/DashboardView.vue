<template>
  <MainLayout>
    <div class="dashboard">
      <!-- Hero: Character Card -->
      <section class="hero-section">
        <div class="char-card dark-panel">
          <div class="char-avatar">
            <div class="avatar-ring">
              <div class="avatar-img">{{ userStore.user?.displayName?.[0] ?? '?' }}</div>
            </div>
            <div class="level-badge">Lv.{{ userStore.user?.currentLevel }}</div>
          </div>

          <div class="char-info">
            <div class="char-name-row">
              <h2 class="char-name">{{ userStore.displayName }}</h2>
              <span class="cefr-badge" :class="'cefr-' + (userStore.user?.cefrLevel ?? 'PRE_A1').toLowerCase().replace('_', '')">
                {{ userStore.user?.cefrLevel ?? 'PRE_A1' }}
              </span>
            </div>
            <p class="char-title">{{ titleText }}</p>

            <!-- XP bar -->
            <div class="stat-row">
              <span class="stat-label">经验</span>
              <div class="hp-bar hp-bar--gold">
                <div class="hp-bar__fill" :style="{ width: xpPercent + '%' }"></div>
                <span class="hp-bar__text">{{ userStore.user?.totalXp }} / {{ xpMax }} XP</span>
              </div>
            </div>

            <!-- Stats row -->
            <div class="stat-chips">
              <span class="chip chip-gold">🪙 {{ userStore.user?.coins }}</span>
              <span class="chip chip-fire">🔥 {{ userStore.user?.streak }}天</span>
              <span class="chip chip-purple">✨ {{ userStore.user?.skillPoints }}点</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Chapter Journey -->
      <section class="chapter-section">
        <h3 class="section-title">📜 学习旅程</h3>

        <!-- Chapter selector tabs -->
        <div class="chapter-tabs">
          <button
            v-for="ch in chapterStore.allChapters"
            :key="ch.code"
            class="chapter-tab"
            :class="{
              'tab--active': ch.code === chapterStore.currentChapterCode,
              'tab--locked': chapterStore.progressMap[ch.code]?.phase === 'locked',
              'tab--completed': chapterStore.progressMap[ch.code]?.phase === 'completed',
            }"
            @click="selectChapter(ch.code)"
          >
            <span v-if="chapterStore.progressMap[ch.code]?.phase === 'completed'" class="tab-check">✓</span>
            <span v-else-if="chapterStore.progressMap[ch.code]?.phase === 'locked'" class="tab-lock">🔒</span>
            <span class="tab-num">{{ ch.orderIndex }}</span>
          </button>
        </div>

        <!-- Current chapter card -->
        <div class="chapter-card dark-panel">
          <div class="chapter-head">
            <span class="chapter-tag">{{ chapterStore.currentChapter.cefrLevel }}</span>
            <span class="chapter-name">{{ chapterStore.currentChapter.titleZh }}</span>
            <span class="chapter-phase-badge" :class="'phase-' + chapterStore.currentPhase">
              {{ phaseLabel }}
            </span>
          </div>

          <!-- 4-stage journey flow -->
          <div class="journey-flow">
            <div
              v-for="(stage, idx) in journeyStages"
              :key="stage.key"
              class="journey-stage"
              :class="{
                'stage--active': stage.active,
                'stage--done': stage.done,
                'stage--locked': stage.locked,
              }"
              @click="enterStage(stage)"
            >
              <div class="stage-icon">{{ stage.icon }}</div>
              <div class="stage-info">
                <h4 class="stage-name">{{ stage.name }}</h4>
                <p class="stage-desc">{{ stage.desc }}</p>
                <div v-if="stage.progress !== undefined" class="stage-prog">
                  <div class="hp-bar hp-bar--gold" style="height: 4px;">
                    <div class="hp-bar__fill" :style="{ width: stage.progress + '%' }"></div>
                  </div>
                  <span class="stage-prog-text">{{ stage.progress }}%</span>
                </div>
              </div>
              <div v-if="stage.locked" class="stage-lock">🔒</div>
              <div v-else-if="stage.done" class="stage-check">✓</div>
            </div>
            <!-- Connecting arrows -->
            <div class="journey-arrows">
              <span v-for="i in 2" :key="i" class="arrow">→</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Portal cards -->
      <section class="portal-section">
        <h3 class="section-title">🏰 冒险入口</h3>
        <div class="portal-grid">
          <div
            v-for="portal in portals"
            :key="portal.route"
            class="portal-card"
            :class="[portal.cls, { 'portal--disabled': portal.disabled }]"
            @click="!portal.disabled && goTo(portal.route)"
            @mouseenter="sound.click()"
          >
            <div class="portal-icon">{{ portal.icon }}</div>
            <h4 class="portal-name">{{ portal.name }}</h4>
            <p class="portal-desc">{{ portal.desc }}</p>
            <div v-if="portal.disabled" class="portal-lock-overlay">🔒</div>
            <div class="portal-glow"></div>
          </div>
        </div>
      </section>

      <!-- Checkin streak -->
      <section class="checkin-section">
        <h3 class="section-title">📅 签到记录</h3>
        <div class="checkin-row dark-panel">
          <div
            v-for="d in 7"
            :key="d"
            class="checkin-day"
            :class="{ 'checkin-day--done': d <= (userStore.user?.streak ?? 0) }"
          >
            <div class="checkin-circle">
              <span v-if="d <= (userStore.user?.streak ?? 0)">✓</span>
              <span v-else>{{ d }}</span>
            </div>
            <span class="checkin-label">Day {{ d }}</span>
          </div>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'
import { useChapterStore } from '@/stores/chapter'
import { useSound } from '@/composables/useSound'

const router = useRouter()
const userStore = useUserStore()
const chapterStore = useChapterStore()
const sound = useSound()

const xpMax = computed(() => userStore.user?.xpToNextLevel ?? 200)
const xpPercent = computed(() => {
  const u = userStore.user
  if (!u) return 0
  return Math.min(100, (u.totalXp / u.xpToNextLevel) * 100)
})

const titleText = computed(() => {
  const lv = userStore.user?.currentLevel ?? 1
  if (lv >= 10) return '暗影猎手'
  if (lv >= 5) return '见习骑士'
  return '初心冒险者'
})

const phaseLabel = computed(() => {
  const labels: Record<string, string> = {
    locked: '🔒 未解锁',
    camp: '🏕️ 营地探索',
    quest: '⚔️ 主线任务',
    boss: '💀 Boss 战',
    completed: '✓ 已通关',
  }
  return labels[chapterStore.currentPhase] ?? ''
})

const journeyStages = computed(() => {
  const phase = chapterStore.currentPhase
  const campRate = Math.round(chapterStore.campCompletionRate * 100)
  const prog = chapterStore.currentProgress
  const lessons = chapterStore.currentLessons
  const questRate = lessons.length > 0
    ? Math.round(((prog?.questDaysCompleted?.length ?? 0) / lessons.length) * 100)
    : 0

  return [
    {
      key: 'camp',
      icon: '🏕️',
      name: '营地探索',
      desc: '发现单词, 收集知识',
      route: '/camp',
      active: phase === 'camp',
      done: phase === 'quest' || phase === 'boss' || phase === 'completed',
      locked: phase === 'locked',
      progress: phase !== 'locked' ? campRate : undefined,
    },
    {
      key: 'quest',
      icon: '⚔️',
      name: '主线任务',
      desc: `每日练习 (${prog?.questDaysCompleted?.length ?? 0}/${lessons.length}天)`,
      route: '/quest',
      active: phase === 'quest',
      done: phase === 'boss' || phase === 'completed',
      locked: phase === 'locked' || phase === 'camp',
      progress: (phase === 'quest' || phase === 'boss' || phase === 'completed') ? questRate : undefined,
    },
    {
      key: 'boss',
      icon: '💀',
      name: 'Boss 战',
      desc: '章末大考验',
      route: '/boss',
      active: phase === 'boss',
      done: phase === 'completed',
      locked: phase !== 'boss' && phase !== 'completed',
      progress: undefined,
    },
  ]
})

const portals = computed(() => [
  { icon: '🏕️', name: '营地探索', desc: '探索学习, 收集单词', route: '/camp', cls: 'portal-camp', disabled: chapterStore.currentPhase === 'locked' },
  { icon: '⚔️', name: '主线任务', desc: '每日刷题, 通关前进', route: '/quest', cls: 'portal-quest', disabled: !chapterStore.isQuestUnlocked },
  { icon: '🏟️', name: 'SRS 副本', desc: '间隔复习, 强化记忆', route: '/arena', cls: 'portal-arena', disabled: false },
  { icon: '🌟', name: '技能星图', desc: '星座图, 解锁能力', route: '/skill-tree', cls: 'portal-skill', disabled: false },
  { icon: '💀', name: 'Boss 战', desc: '挑战Boss, 赢取奖励', route: '/boss', cls: 'portal-boss', disabled: !chapterStore.isBossUnlocked },
  { icon: '🔔', name: '试炼之门', desc: '突破封印, 解锁更高等级', route: '/cefr-exam', cls: 'portal-exam', disabled: false },
])

function selectChapter(code: string) {
  const prog = chapterStore.progressMap[code]
  if (prog?.phase === 'locked') return
  chapterStore.setCurrentChapter(code)
  sound.click()
}

function enterStage(stage: { locked: boolean; route: string }) {
  if (stage.locked) return
  sound.click()
  router.push(stage.route)
}

function goTo(route: string) {
  sound.click()
  router.push(route)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.dashboard {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 60px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 14px;
  color: $text-secondary;
}

// Hero char card
.hero-section {
  animation: slide-up 0.5s ease-out;
}

.char-card {
  display: flex;
  gap: 24px;
  padding: 28px;
  align-items: center;
}

.char-avatar {
  position: relative;
  flex-shrink: 0;
}

.avatar-ring {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, $gold, $magic-purple);
  animation: pulse-glow 3s infinite;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: $bg-dark;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 900;
  color: $gold;
}

.level-badge {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, $gold, $gold-dark);
  color: $bg-abyss;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 10px;
  border-radius: 10px;
}

.char-info {
  flex: 1;
  min-width: 0;
}

.char-name {
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
}

.char-title {
  font-size: 13px;
  color: $magic-purple;
  margin: 2px 0 12px;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;

  .stat-label {
    font-size: 12px;
    color: $text-muted;
    width: 32px;
    flex-shrink: 0;
  }

  .hp-bar {
    flex: 1;
  }
}

.stat-chips {
  display: flex;
  gap: 12px;
}

.chip {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid $border-dim;

  &-gold { color: $gold; }
  &-fire { color: $fire-orange; }
  &-purple { color: $magic-purple; }
}

// Chapter journey
.chapter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.chapter-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  border: 2px solid $border-dim;
  background: $bg-card;
  cursor: pointer;
  transition: all $transition-normal;
  position: relative;

  .tab-num {
    font-size: 16px;
    font-weight: 800;
    color: $text-muted;
  }

  .tab-check, .tab-lock {
    font-size: 14px;
    position: absolute;
    top: -4px;
    right: -4px;
  }

  .tab-check {
    background: $life-green;
    color: #fff;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
  }

  &.tab--active {
    border-color: $gold;
    background: rgba($gold, 0.1);
    box-shadow: $shadow-glow-gold;

    .tab-num { color: $gold; }
  }

  &.tab--locked {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &.tab--completed {
    border-color: $life-green;
  }
}

.chapter-card {
  padding: 20px;
}

.chapter-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.chapter-tag {
  background: linear-gradient(135deg, $gold, $gold-dark);
  color: $bg-abyss;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 10px;
  border-radius: 6px;
}

.chapter-name {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
  flex: 1;
}

.chapter-phase-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid $border-dim;

  &.phase-camp { color: $life-green; border-color: rgba($life-green, 0.3); }
  &.phase-quest { color: $gold; border-color: rgba($gold, 0.3); }
  &.phase-boss { color: $hp-red; border-color: rgba($hp-red, 0.3); }
  &.phase-completed { color: $life-green; border-color: rgba($life-green, 0.3); }
  &.phase-locked { color: $text-muted; }
}

// Journey flow (3 stages)
.journey-flow {
  display: flex;
  gap: 12px;
  position: relative;
}

.journey-arrows {
  display: none; // handled by gap + stage borders
}

.journey-stage {
  flex: 1;
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 14px;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid $border-dim;
  cursor: pointer;
  transition: all $transition-normal;
  position: relative;

  &:hover:not(.stage--locked) {
    transform: translateY(-2px);
    border-color: $border-subtle;
  }

  &.stage--active {
    border-color: $gold;
    background: rgba($gold, 0.06);
    box-shadow: 0 0 12px rgba($gold, 0.15);
  }

  &.stage--done {
    border-color: $life-green;
    background: rgba($life-green, 0.04);
  }

  &.stage--locked {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.stage-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.stage-info {
  flex: 1;
  min-width: 0;
}

.stage-name {
  font-size: 13px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 2px;
}

.stage-desc {
  font-size: 11px;
  color: $text-muted;
  margin-bottom: 4px;
}

.stage-prog {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stage-prog-text {
  font-size: 11px;
  font-weight: 700;
  color: $gold;
}

.stage-lock {
  font-size: 16px;
}

.stage-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: $life-green;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
}

// Portal disabled state
.portal--disabled {
  opacity: 0.35;
  cursor: not-allowed !important;
  pointer-events: none;
}

.portal-lock-overlay {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 16px;
}

// Portals
.portal-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.portal-card {
  position: relative;
  padding: 24px;
  border-radius: $radius-xl;
  background: $glass-bg;
  border: 1px solid $glass-border;
  backdrop-filter: blur(10px);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s $ease-smooth;

  &:hover {
    transform: translateY(-4px) scale(1.02);
    border-color: rgba(255, 255, 255, 0.15);
  }

  &:hover .portal-glow {
    opacity: 1;
  }
}

.portal-icon {
  font-size: 36px;
  margin-bottom: 10px;
}

.portal-name {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 4px;
}

.portal-desc {
  font-size: 12px;
  color: $text-muted;
}

.portal-glow {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.4s;
  pointer-events: none;
  border-radius: inherit;
}

.portal-quest .portal-glow  { background: radial-gradient(ellipse at center, rgba(245, 158, 11, 0.08) 0%, transparent 70%); }
.portal-arena .portal-glow  { background: radial-gradient(ellipse at center, rgba(59, 130, 246, 0.08) 0%, transparent 70%); }
.portal-skill .portal-glow  { background: radial-gradient(ellipse at center, rgba(124, 58, 237, 0.08) 0%, transparent 70%); }
.portal-boss .portal-glow   { background: radial-gradient(ellipse at center, rgba(220, 38, 38, 0.08) 0%, transparent 70%); }
.portal-exam .portal-glow   { background: radial-gradient(ellipse at center, rgba(201, 160, 255, 0.08) 0%, transparent 70%); }

.portal-quest:hover { border-color: rgba($gold, 0.3); }
.portal-arena:hover { border-color: rgba($mana-blue, 0.3); }
.portal-skill:hover { border-color: rgba($magic-purple, 0.3); }
.portal-boss:hover  { border-color: rgba($hp-red, 0.3); }
.portal-exam:hover  { border-color: rgba($magic-purple, 0.3); }

// CEFR badge in char card
.char-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cefr-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 6px;
}
.cefr-prea1 { background: #4a3560; color: #c9a0ff; border: 1px solid #7b5ea7; }
.cefr-a1 { background: #2a4a2a; color: #7ddf7d; border: 1px solid #4a8a4a; }
.cefr-a2 { background: #4a4a2a; color: #dfdf7d; border: 1px solid #8a8a4a; }
.cefr-b1 { background: #2a3a4a; color: #7dc8df; border: 1px solid #4a7a8a; }

// Checkin
.checkin-row {
  display: flex;
  gap: 0;
  padding: 16px;
}

.checkin-day {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.checkin-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: $bg-dark;
  border: 1px solid $border-dim;
  color: $text-muted;
  transition: all $transition-normal;
}

.checkin-label {
  font-size: 10px;
  color: $text-muted;
}

.checkin-day--done .checkin-circle {
  background: linear-gradient(135deg, $gold, $gold-dark);
  border-color: $gold;
  color: $bg-abyss;
  font-weight: 800;
}
</style>
