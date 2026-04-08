<template>
  <div class="dashboard">
    <!-- 入学评估引导卡 -->
    <div v-if="showAssessmentBanner" class="banner banner--pink" @click="goToAssessment">
      <div class="banner-glow banner-glow--pink" />
      <span class="banner-emoji">🌸</span>
      <div class="banner-text">
        <h3>Hi！我是你的英语老师 Lily</h3>
        <p>先来做个小测试，让我了解你的水平，好为你制定学习计划哦～</p>
      </div>
      <svg class="banner-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
    </div>

    <!-- Phase 5a: AI 学习计划卡 -->
    <div v-if="teachingPlan && !showAssessmentBanner" class="plan-card" @click="goToPlanTarget">
      <div class="plan-card__header">
        <span class="plan-card__icon">{{ phaseEmoji }}</span>
        <div class="plan-card__info">
          <h3>{{ teachingPlan.phaseLabel }}</h3>
          <p v-if="teachingPlan.levelCode">当前级别：{{ teachingPlan.levelCode }}</p>
        </div>
        <span class="plan-card__badge">{{ phaseAction }}</span>
      </div>
      <div class="plan-card__hint">
        {{ planHint }}
      </div>
    </div>

    <!-- Hero 欢迎区域 -->
    <div class="hero">
      <div class="hero-bg">
        <div class="hero-orb hero-orb-1" />
        <div class="hero-orb hero-orb-2" />
      </div>
      <div class="hero-content">
        <div class="hero-greeting">
          <h1>{{ greeting }}，{{ displayName }}！</h1>
          <div v-if="streakInfo && streakInfo.streak > 0" class="hero-streak">
            🔥 连续{{ streakInfo.streak }}天
          </div>
        </div>
        <p class="hero-sub">{{ heroMessage }}</p>
      </div>
    </div>

    <!-- ===== 今日学习任务 ===== -->
    <div class="daily-tasks">
      <div class="daily-tasks__header">
        <h2>📋 今日学习</h2>
        <span v-if="allTasksDone" class="daily-tasks__badge">🎉 全部完成</span>
      </div>

      <div class="task-list">
        <!-- 第一步：复习 -->
        <div class="task-item" :class="{ done: srsStats.dueCount === 0, active: srsStats.dueCount > 0 }" @click="srsStats.dueCount > 0 && router.push('/review')">
          <div class="task-step">1</div>
          <div class="task-body">
            <h4>间隔复习</h4>
            <p v-if="srsStats.dueCount > 0">{{ srsStats.dueCount }} 个词需要复习，趁记忆还在！</p>
            <p v-else>今日复习已完成 ✓</p>
          </div>
          <div class="task-count" v-if="srsStats.dueCount > 0">
            <span class="task-num">{{ srsStats.dueCount }}</span>
            <span class="task-unit">待复习</span>
          </div>
          <AppIcon v-else name="check-circle" :size="24" class="task-done-icon" />
        </div>

        <!-- 第二步：学新知识 -->
        <div class="task-item" :class="{ done: todayLearned >= dailyGoal, active: srsStats.dueCount === 0 && todayLearned < dailyGoal }" @click="goToCurrentUnit">
          <div class="task-step">2</div>
          <div class="task-body">
            <h4>学习新内容</h4>
            <p v-if="currentUnitName">{{ currentUnitName }} — 已掌握 {{ todayLearned }}/{{ dailyGoal }} 个</p>
            <p v-else>从推荐的级别开始学习吧</p>
          </div>
          <div class="task-progress-ring" v-if="todayLearned < dailyGoal">
            <svg viewBox="0 0 36 36">
              <circle cx="18" cy="18" r="15.5" fill="none" stroke="#E2E8F0" stroke-width="3" />
              <circle cx="18" cy="18" r="15.5" fill="none" stroke="#6366F1" stroke-width="3"
                :stroke-dasharray="`${(todayLearned / dailyGoal) * 97.4} 97.4`"
                stroke-linecap="round" transform="rotate(-90 18 18)" />
            </svg>
            <span class="ring-text">{{ todayLearned }}</span>
          </div>
          <AppIcon v-else name="check-circle" :size="24" class="task-done-icon" />
        </div>

        <!-- 第三步：练习 -->
        <div class="task-item" :class="{ done: todayPracticed >= 10, active: todayLearned >= dailyGoal && todayPracticed < 10 }" @click="router.push('/practice')">
          <div class="task-step">3</div>
          <div class="task-body">
            <h4>做练习</h4>
            <p v-if="todayPracticed > 0">今日已练 {{ todayPracticed }} 题，正确率 {{ todayAccuracy }}%</p>
            <p v-else>学完新内容，趁热练一练</p>
          </div>
          <div class="task-count" v-if="todayPracticed < 10">
            <span class="task-num">{{ todayPracticed }}</span>
            <span class="task-unit">/10题</span>
          </div>
          <AppIcon v-else name="check-circle" :size="24" class="task-done-icon" />
        </div>
      </div>

      <!-- 错题提醒 -->
      <div v-if="unreviewedMistakes > 0" class="mistake-nudge" @click="router.push('/mistakes')">
        <span>📝 你有 <strong>{{ unreviewedMistakes }}</strong> 道错题未复习</span>
        <AppIcon name="arrow-right" :size="16" />
      </div>
    </div>

    <!-- ===== 快速入口 ===== -->
    <div class="quick-actions">
      <router-link to="/upload" class="quick-btn quick-btn--green">
        <AppIcon name="upload" :size="20" />
        <span>上传材料</span>
      </router-link>
      <router-link to="/chat" class="quick-btn quick-btn--pink">
        <AppIcon name="message-circle" :size="20" />
        <span>问 Lily 老师</span>
      </router-link>
      <router-link to="/levels" class="quick-btn quick-btn--indigo">
        <AppIcon name="book-open" :size="20" />
        <span>知识库</span>
      </router-link>
      <router-link to="/stats" class="quick-btn quick-btn--amber">
        <AppIcon name="bar-chart-2" :size="20" />
        <span>学习统计</span>
      </router-link>
    </div>

    <!-- 我的学习集 -->
    <div v-if="studySets.length > 0" class="study-sets-section">
      <div class="section-header">
        <h3><span class="section-icon">📚</span> 我的学习集</h3>
        <router-link to="/upload" class="section-link">上传更多 →</router-link>
      </div>
      <div class="study-sets-grid">
        <router-link
          v-for="s in studySets.slice(0, 4)" :key="s.id" :to="`/study-sets/${s.id}`"
          class="study-set-card"
        >
          <div class="study-set-header">
            <span class="study-set-status"
              :class="{
                'study-set-status--ready': s.status === 'ready',
                'study-set-status--processing': s.status === 'processing',
                'study-set-status--failed': s.status === 'failed',
              }">{{ s.status === 'ready' ? '✓ 可用' : s.status === 'processing' ? '⏳ 处理中' : '✗ 失败' }}</span>
            <span class="study-set-count">{{ s.itemCount }} 个知识点</span>
          </div>
          <h4>{{ s.title }}</h4>
          <p v-if="s.aiSummary" class="study-set-summary">{{ s.aiSummary }}</p>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { assessmentApi, orchestratorApi, type StudentProfile, type TeachingPlan } from '@/api/teacher'
import { statsApi, type TodayStats, type StreakInfo } from '@/api/stats'
import { srsApi, type SrsStats } from '@/api/srs'
import { levelApi } from '@/api/level'
import { studySetApi, type StudySet } from '@/api/studySet'
import { mistakeApi } from '@/api/mistakes'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const userStore = useUserStore()
const displayName = computed(() => userStore.user?.displayName || userStore.user?.username || '同学')
const showAssessmentBanner = ref(false)
const teachingPlan = ref<TeachingPlan | null>(null)
const todayStats = ref<TodayStats | null>(null)
const streakInfo = ref<StreakInfo | null>(null)
const srsStats = ref<SrsStats>({ total: 0, dueCount: 0, mastered: 0, learning: 0 })
const studySets = ref<StudySet[]>([])
const unreviewedMistakes = ref(0)
const currentUnitName = ref('')
const currentUnitPath = ref('')
const dailyGoal = 10

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayLearned = computed(() => todayStats.value?.wordsLearned || 0)
const todayPracticed = computed(() => {
  if (!todayStats.value) return 0
  return todayStats.value.correctCount + todayStats.value.wrongCount
})
const todayAccuracy = computed(() => {
  const total = todayPracticed.value
  if (total === 0) return 0
  return Math.round((todayStats.value!.correctCount / total) * 100)
})
const allTasksDone = computed(() =>
  srsStats.value.dueCount === 0 && todayLearned.value >= dailyGoal && todayPracticed.value >= 10
)

const heroMessage = computed(() => {
  if (allTasksDone.value) return '今日学习任务全部完成，太棒了！明天继续加油 🌟'
  if (srsStats.value.dueCount > 0) return `${srsStats.value.dueCount} 个词等着你复习，先把记忆巩固住！`
  if (todayLearned.value < dailyGoal) return `今天再学 ${dailyGoal - todayLearned.value} 个新词就达标啦 💪`
  if (todayPracticed.value < 10) return '新知识学完了，来做几道练习检验一下！'
  return '今天想学点什么？'
})

function mapVocabToLevel(vocab: string, grade: string): string | null {
  // L3-L12 体系：每年级一个级别
  const gradeMap: Record<string, string[]> = {
    primary: ['L3', 'L4', 'L5', 'L6'],
    junior: ['L7', 'L8', 'L9'],
    senior: ['L10', 'L11', 'L12'],
  }
  const codes = gradeMap[grade] ?? gradeMap['junior']!
  const vocabIdx: Record<string, number> = { beginner: 0, elementary: 1, intermediate: 1, upper: 2, advanced: codes.length - 1 }
  const idx = vocabIdx[vocab] ?? 0
  return codes[Math.min(idx, codes.length - 1)] ?? null
}

function goToCurrentUnit() {
  if (currentUnitPath.value) router.push(currentUnitPath.value)
  else router.push('/levels')
}

const phaseEmoji = computed(() => {
  const map: Record<string, string> = { review: '🔄', learn: '📖', practice: '✏️', summary: '📊' }
  return map[teachingPlan.value?.phase || ''] || '📋'
})
const phaseAction = computed(() => {
  const map: Record<string, string> = { review: '开始复习', learn: '开始学习', practice: '开始练习', summary: '查看总结' }
  return map[teachingPlan.value?.phase || ''] || '开始'
})
const planHint = computed(() => {
  const p = teachingPlan.value?.phase
  if (p === 'practice') return '点击进入练习模式，巩固已学知识'
  if (p === 'review') return '点击进入复习，巩固薄弱知识点'
  return `点击与 Lily 老师开始今天的${teachingPlan.value?.phaseLabel || '学习'}`
})
function goToPlanTarget() {
  const p = teachingPlan.value?.phase
  if (p === 'practice') {
    router.push('/practice')
  } else if (p === 'review') {
    router.push('/review')
  } else {
    router.push('/chat')
  }
}
function goToChat() { router.push('/chat') }

onMounted(async () => {
  const [assessRes, statsRes, streakRes, srsRes, setsRes, mistakeRes, levelsRes] = await Promise.allSettled([
    assessmentApi.status(),
    statsApi.getToday(),
    statsApi.getStreak(),
    srsApi.getStats(),
    studySetApi.list(),
    mistakeApi.list({ reviewed: false, size: 1 }),
    levelApi.list(),
  ])

  if (setsRes.status === 'fulfilled') studySets.value = setsRes.value.data.data || []
  if (statsRes.status === 'fulfilled') todayStats.value = statsRes.value.data.data
  if (streakRes.status === 'fulfilled') streakInfo.value = streakRes.value.data.data
  if (srsRes.status === 'fulfilled') srsStats.value = srsRes.value.data.data
  if (mistakeRes.status === 'fulfilled') unreviewedMistakes.value = mistakeRes.value.data.data.totalElements || 0

  if (assessRes.status === 'fulfilled') {
    const assessed = assessRes.value.data.data.assessed
    showAssessmentBanner.value = !assessed
    if (assessed) {
      // 获取教学计划
      try {
        const planRes = await orchestratorApi.getPlan()
        teachingPlan.value = planRes.data.data
      } catch { /* ignore */ }

      if (levelsRes.status === 'fulfilled') {
      try {
        const profileRes = await assessmentApi.getProfile()
        const p: StudentProfile = profileRes.data.data
        const grade = userStore.user?.grade || 'junior'
        const targetCode = mapVocabToLevel(p.vocabularyLevel, grade)
        if (targetCode) {
          const levels = levelsRes.value.data.data
          const found = levels.find((l: any) => l.code === targetCode)
          if (found) {
            // 找到当前正在学的 unit
            try {
              const detailRes = await levelApi.getDetail(found.id)
              const units = detailRes.data.data.units || []
              const current = units.find((u: any) => (u.progress || 0) < 100)
              if (current) {
                currentUnitName.value = `${found.name} · ${current.name}`
                currentUnitPath.value = `/learn/${current.id}`
              } else {
                currentUnitName.value = found.name
                currentUnitPath.value = `/levels/${found.id}`
              }
            } catch {
              currentUnitName.value = found.name
              currentUnitPath.value = `/levels/${found.id}`
            }
          }
        }
      } catch { /* ignore */ }
    }
  }
})

function goToAssessment() { router.push('/assessment') }
</script>

<style scoped lang="scss">
.dashboard {
  max-width: 960px;
  margin: 0 auto;
  animation: fadeUp 0.4s ease-out;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ====== Banners ====== */
.banner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  border-radius: 18px;
  cursor: pointer;
  overflow: hidden;
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
    .banner-arrow { transform: translateX(4px); }
  }

  &--pink {
    background: linear-gradient(135deg, #FDF2F8, #FBCFE8, #F9A8D4);
    h3, p, .banner-arrow { color: #831843; }
  }
}

.banner-glow {
  position: absolute;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;

  &--pink { background: #F9A8D4; right: -30px; top: -30px; }
}

.banner-emoji {
  font-size: 36px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.banner-text {
  flex: 1;
  position: relative;
  z-index: 1;
  h3 { font-size: 15px; font-weight: 700; margin-bottom: 3px; }
  p { font-size: 13px; line-height: 1.5; opacity: 0.85; }
}

.banner-arrow {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  transition: transform 0.2s;
}

/* ====== Hero ====== */
.hero {
  position: relative;
  padding: 36px 32px;
  border-radius: 24px;
  background: linear-gradient(135deg, #4F46E5, #7C3AED, #9333EA);
  color: white;
  overflow: hidden;
  margin-bottom: 24px;
}

.hero-bg {
  position: absolute;
  inset: 0;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);

  &-1 {
    width: 200px; height: 200px;
    background: rgba(147, 51, 234, 0.5);
    top: -60px; right: -40px;
  }
  &-2 {
    width: 150px; height: 150px;
    background: rgba(99, 102, 241, 0.4);
    bottom: -40px; left: -20px;
  }
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-greeting {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 8px;
  flex-wrap: wrap;

  h1 {
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -0.5px;
  }
}

.hero-streak {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  font-size: 13px;
  font-weight: 700;
}

.hero-sub {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
}

/* ====== Daily Tasks ====== */
.daily-tasks {
  margin-bottom: 24px;
  padding: 24px;
  border-radius: 20px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
}

.daily-tasks__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;

  h2 {
    font-size: 17px;
    font-weight: 700;
    color: var(--text-primary, #0F172A);
  }
}

.daily-tasks__badge {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 100px;
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1.5px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }

  &.active {
    border-color: #6366F1;
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.04), rgba(139, 92, 246, 0.04));
    box-shadow: 0 2px 12px rgba(99, 102, 241, 0.1);

    .task-step {
      background: #6366F1;
      color: white;
    }
  }

  &.done {
    opacity: 0.65;
    cursor: default;

    &:hover {
      transform: none;
      box-shadow: none;
    }

    .task-step {
      background: #10B981;
      color: white;
    }

    .task-body h4 {
      text-decoration: line-through;
      color: var(--text-muted, #94A3B8);
    }
  }
}

.task-step {
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  background: var(--bg-secondary, #F1F5F9);
  color: var(--text-muted, #94A3B8);
  transition: all 0.2s;
}

.task-body {
  flex: 1;
  min-width: 0;

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary, #0F172A);
    margin-bottom: 2px;
  }

  p {
    font-size: 13px;
    color: var(--text-muted, #94A3B8);
  }
}

.task-count {
  text-align: center;
  min-width: 48px;

  .task-num {
    display: block;
    font-size: 22px;
    font-weight: 800;
    color: #6366F1;
    line-height: 1;
  }

  .task-unit {
    font-size: 11px;
    color: var(--text-muted, #94A3B8);
  }
}

.task-progress-ring {
  position: relative;
  width: 44px;
  height: 44px;
  min-width: 44px;

  svg {
    width: 100%;
    height: 100%;
  }
}

.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #6366F1;
}

.task-done-icon {
  color: #10B981;
  flex-shrink: 0;
}

/* ====== Mistake Nudge ====== */
.mistake-nudge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(245, 158, 11, 0.14);
  }

  span {
    font-size: 13px;
    color: #92400E;

    strong { font-weight: 700; color: #D97706; }
  }

  :deep(svg) {
    color: #D97706;
  }
}

/* ====== Quick Actions ====== */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;

  @media (max-width: 600px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  border-radius: 16px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }

  :deep(svg) { width: 22px; height: 22px; }

  &--green {
    background: rgba(16, 185, 129, 0.08);
    color: #059669;
    border-color: rgba(16, 185, 129, 0.15);
    &:hover { background: rgba(16, 185, 129, 0.15); }
  }

  &--pink {
    background: rgba(236, 72, 153, 0.08);
    color: #DB2777;
    border-color: rgba(236, 72, 153, 0.15);
    &:hover { background: rgba(236, 72, 153, 0.15); }
  }

  &--indigo {
    background: rgba(99, 102, 241, 0.08);
    color: #4F46E5;
    border-color: rgba(99, 102, 241, 0.15);
    &:hover { background: rgba(99, 102, 241, 0.15); }
  }

  &--amber {
    background: rgba(245, 158, 11, 0.08);
    color: #D97706;
    border-color: rgba(245, 158, 11, 0.15);
    &:hover { background: rgba(245, 158, 11, 0.15); }
  }
}

/* ====== Study Sets ====== */
.study-sets-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  h3 {
    font-size: 16px;
    font-weight: 700;
    color: var(--text-primary, #0F172A);
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.section-icon { font-size: 18px; }

.section-link {
  font-size: 13px;
  font-weight: 600;
  color: #6366F1;
  text-decoration: none;

  &:hover { text-decoration: underline; }
}

.study-sets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}

.study-set-card {
  padding: 18px;
  border-radius: 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
    border-color: rgba(99, 102, 241, 0.3);
  }

  h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary, #0F172A);
    margin-bottom: 6px;
  }
}

.study-set-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.study-set-status {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;

  &--ready { background: rgba(16, 185, 129, 0.1); color: #059669; }
  &--processing { background: rgba(245, 158, 11, 0.1); color: #D97706; }
  &--failed { background: rgba(239, 68, 68, 0.1); color: #DC2626; }
}

.study-set-count {
  font-size: 11px;
  color: var(--text-muted, #94A3B8);
}

.study-set-summary {
  font-size: 12px;
  color: var(--text-muted, #94A3B8);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ====== Phase 5a: 学习计划卡 ====== */
.plan-card {
  background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%);
  border-radius: 18px;
  padding: 20px 22px;
  color: #fff;
  cursor: pointer;
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(99, 102, 241, 0.3);
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  &__icon {
    font-size: 32px;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;

    h3 {
      font-size: 18px;
      font-weight: 700;
      margin: 0;
    }
    p {
      font-size: 13px;
      opacity: 0.85;
      margin: 2px 0 0;
    }
  }

  &__badge {
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(4px);
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
  }

  &__hint {
    margin-top: 12px;
    font-size: 13px;
    opacity: 0.75;
  }
}
</style>
