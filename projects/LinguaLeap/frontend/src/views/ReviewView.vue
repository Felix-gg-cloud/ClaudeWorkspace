<template>
  <div class="review">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card due" :class="{ active: stats.dueCount > 0 }">
        <div class="stat-card__icon"><AppIcon name="refresh-cw" :size="24" /></div>
        <div class="stat-card__info">
          <span class="stat-card__value">{{ stats.dueCount }}</span>
          <span class="stat-card__label">今日待复习</span>
        </div>
      </div>
      <div class="stat-card mastered">
        <div class="stat-card__icon"><AppIcon name="check-circle" :size="24" /></div>
        <div class="stat-card__info">
          <span class="stat-card__value">{{ stats.mastered }}</span>
          <span class="stat-card__label">已掌握</span>
        </div>
      </div>
      <div class="stat-card learning">
        <div class="stat-card__icon"><AppIcon name="book-open" :size="24" /></div>
        <div class="stat-card__info">
          <span class="stat-card__value">{{ stats.learning }}</span>
          <span class="stat-card__label">学习中</span>
        </div>
      </div>
    </div>

    <!-- 下次复习提示 -->
    <div v-if="!reviewing && !reviewDone && stats.dueCount === 0 && stats.nextDueAt" class="next-due-banner">
      <AppIcon name="clock" :size="16" />
      <span>下次复习：{{ formatNextDue(stats.nextDueAt) }}</span>
    </div>

    <!-- 复习流程 -->
    <div v-if="reviewing" class="review-flow">
      <div class="review-progress">
        <div class="review-progress__bar">
          <div class="review-progress__fill" :style="{ width: reviewProgress + '%' }" />
        </div>
        <span class="review-progress__text">{{ currentIdx + 1 }} / {{ dueCards.length }}</span>
      </div>

      <div class="review-card" :key="currentCard?.kpId">
        <div class="review-card__word">{{ currentCard?.content }}</div>
        <div v-if="showAnswer" class="review-card__answer">
          <div class="review-card__phonetic">{{ currentCard?.phonetic }}</div>
          <div class="review-card__meaning">{{ currentCard?.meaningZh }}</div>
        </div>
        <button v-if="!showAnswer" class="btn-show" @click="showAnswer = true">
          <AppIcon name="sparkles" :size="16" /> 显示答案
        </button>
        <div v-else class="review-card__actions">
          <button class="btn-forgot" @click="reviewCard(false)">
            <AppIcon name="x-circle" :size="16" /> 忘记了
          </button>
          <button class="btn-remember" @click="reviewCard(true)">
            <AppIcon name="check-circle" :size="16" /> 记住了
          </button>
        </div>
      </div>
    </div>

    <!-- 复习完成 -->
    <div v-else-if="reviewDone" class="review-done">
      <AppIcon name="rocket" :size="48" />
      <h2>复习完成！</h2>
      <p>今日 {{ dueCards.length }} 个知识点已全部复习</p>
      <div class="review-done__stats">
        <span class="correct">记住 {{ correctCount }} 个</span>
        <span class="wrong">忘记 {{ forgotCount }} 个</span>
      </div>
      <button class="btn-back" @click="resetAndReload">继续查看</button>
    </div>

    <!-- 主内容区 -->
    <div v-else>
      <!-- 待复习区 -->
      <div v-if="dueCards.length > 0" class="section">
        <div class="section__header">
          <h2>
            <AppIcon name="refresh-cw" :size="18" />
            待复习
            <span class="badge badge--due">{{ dueCards.length }}</span>
          </h2>
          <button class="btn-start-review" @click="startReview">
            <AppIcon name="play" :size="16" /> 开始复习
          </button>
        </div>
        <div class="card-grid">
          <div v-for="card in dueCards" :key="card.kpId" class="word-card word-card--due">
            <div class="word-card__content">{{ card.content }}</div>
            <div class="word-card__meaning">{{ card.meaningZh }}</div>
            <div class="word-card__meta">
              <span><AppIcon name="zap" :size="11" /> {{ card.correctStreak }}连对</span>
              <span>间隔 {{ card.intervalDays }}天</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 学习中的卡片 -->
      <div v-if="upcomingCards.length > 0" class="section">
        <div class="section__header">
          <h2>
            <AppIcon name="book-open" :size="18" />
            学习中
            <span class="badge badge--learning">{{ upcomingCards.length }}</span>
          </h2>
          <div class="tab-chips">
            <button
              v-for="opt in viewOptions"
              :key="opt.value"
              class="chip"
              :class="{ active: viewMode === opt.value }"
              @click="viewMode = opt.value"
            >{{ opt.label }}</button>
          </div>
        </div>
        <div class="card-grid">
          <div v-for="card in displayedUpcoming" :key="card.kpId" class="word-card word-card--upcoming">
            <div class="word-card__content">{{ card.content }}</div>
            <div class="word-card__meaning">{{ card.meaningZh }}</div>
            <div class="word-card__meta">
              <span><AppIcon name="zap" :size="11" /> {{ card.correctStreak }}连对</span>
              <span>{{ formatNextDue(card.nextReviewAt) }}</span>
            </div>
          </div>
        </div>
        <button v-if="viewMode === 'partial' && upcomingCards.length > 12" class="btn-showmore" @click="viewMode = 'all'">
          查看全部 {{ upcomingCards.length }} 个
        </button>
      </div>

      <!-- 已掌握的卡片 -->
      <div v-if="masteredCards.length > 0" class="section">
        <div class="section__header">
          <h2>
            <AppIcon name="check-circle" :size="18" />
            已掌握
            <span class="badge badge--mastered">{{ masteredCards.length }}</span>
          </h2>
        </div>
        <div class="card-grid">
          <div v-for="card in masteredCards" :key="card.kpId" class="word-card word-card--mastered">
            <div class="word-card__content">{{ card.content }}</div>
            <div class="word-card__meaning">{{ card.meaningZh }}</div>
            <div class="word-card__meta">
              <span><AppIcon name="zap" :size="11" /> {{ card.correctStreak }}连对</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 完全空状态 -->
      <div v-if="!loading && allCards.length === 0" class="empty-state">
        <AppIcon name="book-open" :size="48" />
        <h3>还没有学习记录</h3>
        <p>完成练习后，知识点会自动加入复习计划</p>
        <button class="btn-practice" @click="router.push('/practice')">
          <AppIcon name="pen-line" :size="16" /> 去练习
        </button>
      </div>

      <div v-if="loading" class="loading"><div class="spinner" /></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { srsApi, type SrsCard, type SrsStats } from '@/api/srs'
import { orchestratorApi } from '@/api/teacher'
import AppIcon from '@/components/AppIcon.vue'
import { showToast } from '@/composables/useToast'

const router = useRouter()

const loading = ref(false)
const dueCards = ref<SrsCard[]>([])
const allCards = ref<SrsCard[]>([])
const stats = ref<SrsStats>({ total: 0, dueCount: 0, mastered: 0, learning: 0 })

const viewMode = ref<'partial' | 'all'>('partial')
const viewOptions = [
  { label: '部分', value: 'partial' as const },
  { label: '全部', value: 'all' as const },
]

// review flow
const reviewing = ref(false)
const reviewDone = ref(false)
const currentIdx = ref(0)
const showAnswer = ref(false)
const correctCount = ref(0)
const forgotCount = ref(0)

const currentCard = computed(() => dueCards.value[currentIdx.value])
const reviewProgress = computed(() => {
  if (!dueCards.value.length) return 0
  return (currentIdx.value / dueCards.value.length) * 100
})

const upcomingCards = computed(() => allCards.value.filter(c => c.status === 'upcoming'))
const masteredCards = computed(() => allCards.value.filter(c => c.status === 'mastered'))
const displayedUpcoming = computed(() =>
  viewMode.value === 'partial' ? upcomingCards.value.slice(0, 12) : upcomingCards.value
)

function formatNextDue(dateStr?: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = d.getTime() - now.getTime()
  const diffH = Math.round(diffMs / 3600000)
  if (diffH <= 0) return '已到期'
  if (diffH < 24) return `${diffH}小时后`
  const diffD = Math.round(diffH / 24)
  if (diffD === 1) return '明天'
  if (diffD < 30) return `${diffD}天后`
  return `${Math.round(diffD / 30)}个月后`
}

async function loadData() {
  loading.value = true
  try {
    const [dueRes, cardsRes, statsRes] = await Promise.all([
      srsApi.getDueCards(),
      srsApi.getAllCards(),
      srsApi.getStats(),
    ])
    dueCards.value = dueRes.data.data
    allCards.value = cardsRes.data.data
    stats.value = statsRes.data.data
  } catch (e) {
    console.error('加载复习数据失败', e)
  } finally {
    loading.value = false
  }
}

function startReview() {
  currentIdx.value = 0
  showAnswer.value = false
  correctCount.value = 0
  forgotCount.value = 0
  reviewing.value = true
  reviewDone.value = false
}

async function reviewCard(correct: boolean) {
  const card = currentCard.value
  if (!card) return

  try {
    await srsApi.review(card.kpId, correct)
    if (correct) correctCount.value++
    else forgotCount.value++

    // Phase 5a: 上报编排引擎
    orchestratorApi.recordAnswer(correct, card.content || '').catch(() => {})
  } catch (e) {
    showToast('提交复习结果失败', 'error')
  }

  if (currentIdx.value + 1 >= dueCards.value.length) {
    reviewing.value = false
    reviewDone.value = true
  } else {
    currentIdx.value++
    showAnswer.value = false
  }
}

function resetAndReload() {
  reviewDone.value = false
  loadData()
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.review {
  max-width: 960px;
  margin: 0 auto;
  animation: fadeUp 0.4s ease-out;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px;
  border-radius: 18px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
  transition: all 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  }

  &__icon {
    width: 48px; height: 48px;
    display: flex; align-items: center; justify-content: center;
    border-radius: 14px;
  }
  &__info { display: flex; flex-direction: column; }
  &__value { font-size: 26px; font-weight: 800; color: var(--text-primary, #0F172A); }
  &__label { font-size: 13px; color: var(--text-muted, #94A3B8); margin-top: 2px; }

  &.due .stat-card__icon { background: rgba(245, 158, 11, 0.12); color: #F59E0B; }
  &.due.active {
    border-color: rgba(245, 158, 11, 0.25);
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.04), var(--bg-card, #fff));
  }
  &.mastered .stat-card__icon { background: rgba(16, 185, 129, 0.12); color: #10B981; }
  &.learning .stat-card__icon { background: rgba(99, 102, 241, 0.12); color: #6366F1; }
}

.next-due-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  margin-bottom: 24px;
  border-radius: 14px;
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.12);
  color: #D97706;
  font-size: 14px;
  font-weight: 500;
}

// Sections
.section {
  margin-bottom: 32px;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 17px;
      font-weight: 700;
      color: var(--text-primary, #0F172A);
    }
  }
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  font-size: 12px;
  font-weight: 600;

  &--due { background: rgba(245, 158, 11, 0.12); color: #D97706; }
  &--learning { background: rgba(99, 102, 241, 0.12); color: #6366F1; }
  &--mastered { background: rgba(16, 185, 129, 0.12); color: #059669; }
}

.tab-chips {
  display: flex;
  gap: 6px;
}

.chip {
  padding: 5px 14px;
  border-radius: 16px;
  border: 1px solid var(--border, #E2E8F0);
  background: transparent;
  color: var(--text-secondary, #64748B);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: rgba(99, 102, 241, 0.1);
    border-color: rgba(99, 102, 241, 0.3);
    color: #6366F1;
    font-weight: 500;
  }
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.word-card {
  padding: 18px;
  border-radius: 14px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
  transition: all 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  }

  &__content { font-size: 16px; font-weight: 700; color: var(--text-primary, #0F172A); margin-bottom: 4px; }
  &__meaning { font-size: 13px; color: var(--text-muted, #94A3B8); margin-bottom: 10px; }
  &__meta {
    display: flex;
    gap: 10px;
    font-size: 11px;
    color: var(--text-muted, #94A3B8);
    span { display: flex; align-items: center; gap: 3px; }
  }

  &--due {
    border-color: rgba(245, 158, 11, 0.2);
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.03), var(--bg-card, #fff));
  }
  &--mastered {
    border-color: rgba(16, 185, 129, 0.15);
    .word-card__content { color: #059669; }
  }
}

.btn-showmore {
  display: block;
  margin: 12px auto 0;
  padding: 8px 20px;
  border-radius: 10px;
  border: 1px solid var(--border, #E2E8F0);
  background: transparent;
  color: var(--text-secondary, #64748B);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: var(--bg-card, #fff); }
}

// Buttons
.btn-start-review, .btn-practice, .btn-back {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
  border: none;
  font-size: 14px; font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-start-review {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: #fff;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.25);
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(245, 158, 11, 0.35); }
}
.btn-practice {
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: #fff;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  &:hover { transform: translateY(-2px); }
}
.btn-back {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
  color: var(--text-secondary, #64748B);
  margin-top: 16px;
  &:hover { border-color: rgba(99, 102, 241, 0.3); color: var(--text-primary, #0F172A); }
}

// Review flow
.review-flow { max-width: 500px; margin: 0 auto; }

.review-progress {
  display: flex; align-items: center; gap: 12px; margin-bottom: 24px;
  &__bar {
    flex: 1; height: 6px; background: var(--border, #E2E8F0); border-radius: 3px; overflow: hidden;
  }
  &__fill {
    height: 100%; background: linear-gradient(90deg, #F59E0B, #D97706);
    border-radius: 3px; transition: width 0.3s;
  }
  &__text { font-size: 13px; color: var(--text-muted, #94A3B8); white-space: nowrap; }
}

.review-card {
  text-align: center;
  padding: 48px 32px;
  border-radius: 24px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);

  &__word { font-size: 32px; font-weight: 800; color: var(--text-primary, #0F172A); margin-bottom: 24px; }
  &__phonetic { font-size: 16px; color: var(--text-muted, #94A3B8); margin-bottom: 8px; }
  &__meaning { font-size: 20px; font-weight: 600; color: var(--text-primary, #0F172A); margin-bottom: 24px; }
  &__actions { display: flex; gap: 16px; justify-content: center; }
}

.btn-show {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 12px 32px; border-radius: 14px;
  background: var(--bg-card, #fff); border: 1px solid var(--border, #E2E8F0);
  color: var(--text-secondary, #64748B); font-size: 15px; font-weight: 500; cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: rgba(99, 102, 241, 0.3); color: #6366F1; }
}

.btn-forgot, .btn-remember {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 12px 28px; border-radius: 14px;
  border: none; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}
.btn-forgot {
  background: rgba(239, 68, 68, 0.08); color: #EF4444;
  border: 1px solid rgba(239, 68, 68, 0.15);
  &:hover { background: rgba(239, 68, 68, 0.15); transform: translateY(-1px); }
}
.btn-remember {
  background: rgba(16, 185, 129, 0.08); color: #10B981;
  border: 1px solid rgba(16, 185, 129, 0.15);
  &:hover { background: rgba(16, 185, 129, 0.15); transform: translateY(-1px); }
}

// Review done
.review-done {
  text-align: center;
  padding: 48px;
  max-width: 400px;
  margin: 40px auto;
  border-radius: 24px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #E2E8F0);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);

  h2 { font-size: 22px; font-weight: 800; color: var(--text-primary, #0F172A); margin: 16px 0 8px; }
  p { color: var(--text-muted, #94A3B8); margin-bottom: 16px; }

  &__stats {
    display: flex; gap: 24px; justify-content: center; margin-bottom: 16px;
    font-weight: 600;
    .correct { color: #10B981; }
    .wrong { color: #EF4444; }
  }
}

// Empty & loading
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted, #94A3B8);
  h3 { margin: 16px 0 8px; color: var(--text-primary, #0F172A); }
  p { margin-bottom: 20px; }
}

.loading {
  display: flex; justify-content: center; padding: 60px;
}

.spinner {
  width: 32px; height: 32px;
  border: 3px solid var(--border, #E2E8F0);
  border-top-color: #F59E0B;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .stats-cards { grid-template-columns: 1fr; }
  .card-grid { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 400px) {
  .card-grid { grid-template-columns: 1fr; }
}
</style>
