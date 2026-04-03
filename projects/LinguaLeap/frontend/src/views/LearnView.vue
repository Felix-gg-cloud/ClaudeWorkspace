<template>
  <div class="learn-view">
    <button class="back-btn" @click="router.back()">
      <AppIcon name="arrow-left" :size="16" />
      返回
    </button>

    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <template v-else-if="unitData">
      <!-- 头部 -->
      <div class="learn-header">
        <h1>{{ unitData.unitName }}</h1>
        <div class="learn-progress-bar">
          <div class="learn-progress-fill" :style="{ width: progressPercent + '%' }" />
        </div>
        <span class="learn-progress-text">{{ currentIndex + 1 }} / {{ unitData.totalCount }}</span>
      </div>

      <!-- 卡片区域 -->
      <div v-if="currentCard" class="card-container">
        <div
          class="flash-card"
          :class="{ flipped: isFlipped, word: currentCard.type === 'word', phrase: currentCard.type === 'phrase', sentence: currentCard.type === 'sentence' }"
          @click="flipCard"
        >
          <!-- 正面：英文 -->
          <div class="card-front">
            <span class="card-type-tag">{{ typeLabel(currentCard.type) }}</span>
            <div class="card-content-main">{{ currentCard.content }}</div>
            <div v-if="currentCard.phonetic" class="card-phonetic">{{ currentCard.phonetic }}</div>
            <SpeakButton :text="currentCard.content" :size="20" class="card-speak" />
            <p class="card-flip-hint">点击卡片查看释义</p>
          </div>

          <!-- 背面：中文 + 例句 -->
          <div class="card-back">
            <span class="card-type-tag">{{ typeLabel(currentCard.type) }}</span>
            <div class="card-meaning">{{ currentCard.meaningZh }}</div>
            <div v-if="currentCard.exampleSentence" class="card-example">
              <div class="example-en">
                {{ currentCard.exampleSentence }}
                <SpeakButton :text="currentCard.exampleSentence" :size="14" />
              </div>
              <div class="example-zh">{{ currentCard.exampleZh }}</div>
            </div>
            <div class="card-difficulty">
              {{ '★'.repeat(currentCard.difficulty || 1) }}{{ '☆'.repeat(5 - (currentCard.difficulty || 1)) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="learn-actions">
        <button class="btn-prev" :disabled="currentIndex === 0" @click="prevCard">
          <AppIcon name="chevron-left" :size="20" />
        </button>

        <div class="mastery-buttons" v-if="isFlipped">
          <div class="mastery-hint">👇 请选择掌握程度</div>
          <div class="mastery-btn-row">
            <button class="btn-again" @click="markAndNext('learning')">
              <AppIcon name="refresh-cw" :size="16" />
              再看看
            </button>
            <button class="btn-got-it" @click="markAndNext('mastered')">
              <AppIcon name="check-circle" :size="16" />
              已掌握
            </button>
          </div>
        </div>
        <div v-else class="flip-hint-action">
          <button class="btn-flip" @click="flipCard">翻转查看</button>
        </div>

        <button class="btn-next" :disabled="currentIndex >= unitData.totalCount - 1" @click="nextCard">
          <AppIcon name="chevron-right" :size="20" />
        </button>
      </div>

      <!-- 完成弹窗 -->
      <Transition name="fade">
        <div v-if="showComplete" class="complete-overlay">
          <div class="complete-card">
            <AppIcon name="rocket" :size="48" class="complete-icon" />
            <h2>学习完成！</h2>
            <p>本单元 {{ unitData.totalCount }} 个知识点已全部浏览</p>
            <div class="complete-actions">
              <button class="btn-practice" @click="goToPractice">
                <AppIcon name="pen-line" :size="16" />
                去练习
              </button>
              <button class="btn-secondary" @click="router.back()">返回</button>
            </div>
          </div>
        </div>
      </Transition>
    </template>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <AppIcon name="book-open" :size="48" />
      <h3>该单元暂无学习内容</h3>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { levelApi, type UnitCards, type LearningCard } from '@/api/level'
import { showToast } from '@/composables/useToast'
import AppIcon from '@/components/AppIcon.vue'
import SpeakButton from '@/components/SpeakButton.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const unitData = ref<UnitCards | null>(null)
const currentIndex = ref(0)
const isFlipped = ref(false)
const showComplete = ref(false)

const unitId = Number(route.params.unitId)

const currentCard = computed<LearningCard | null>(() => {
  if (!unitData.value || !unitData.value.cards.length) return null
  return unitData.value.cards[currentIndex.value] || null
})

const progressPercent = computed(() => {
  if (!unitData.value || !unitData.value.totalCount) return 0
  return ((currentIndex.value + 1) / unitData.value.totalCount) * 100
})

function typeLabel(type: string) {
  const labels: Record<string, string> = {
    word: '单词',
    phrase: '短语',
    sentence: '日常用语',
  }
  return labels[type] || type
}

function flipCard() {
  isFlipped.value = !isFlipped.value
}

function goToPractice() {
  router.push({
    path: '/practice',
    query: {
      unitId: String(unitId),
      unitName: unitData.value?.unitName || '',
    },
  })
}

function prevCard() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    isFlipped.value = false
  }
}

function nextCard() {
  if (unitData.value && currentIndex.value < unitData.value.totalCount - 1) {
    currentIndex.value++
    isFlipped.value = false
  }
}

async function markAndNext(status: string) {
  if (!currentCard.value) return
  try {
    await levelApi.markProgress(currentCard.value.id, status)
  } catch (e) {
    showToast('进度保存失败', 'error')
  }

  if (unitData.value && currentIndex.value >= unitData.value.totalCount - 1) {
    // 最后一张卡片
    try { await levelApi.completeUnit(unitId) } catch { showToast('完成记录保存失败', 'error') }
    showComplete.value = true
  } else {
    nextCard()
  }
}

async function loadCards() {
  loading.value = true
  try {
    const { data } = await levelApi.getUnitCards(unitId)
    unitData.value = data.data
  } catch (e) {
    console.error('加载学习卡片失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadCards)
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.learn-view {
  max-width: 520px;
  margin: 0 auto;
  min-height: 80vh;
  display: flex;
  flex-direction: column;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
  cursor: pointer;
  background: none;
  border: none;
  &:hover { color: var(--primary); }
}

.learn-header {
  text-align: center;
  margin-bottom: 24px;

  h1 {
    font-size: 20px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 12px;
  }
}

.learn-progress-bar {
  height: 6px;
  background: var(--bg-secondary);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.learn-progress-fill {
  height: 100%;
  background: var(--primary);
  border-radius: 3px;
  transition: width 0.3s;
}

.learn-progress-text {
  font-size: 13px;
  color: var(--text-tertiary);
}

// Card
.card-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  perspective: 1000px;
  min-height: 320px;
}

.flash-card {
  width: 100%;
  max-width: 420px;
  min-height: 280px;
  position: relative;
  cursor: pointer;
  transform-style: preserve-3d;
  transition: transform 0.5s;

  &.flipped {
    transform: rotateY(180deg);
  }
}

.card-front, .card-back {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  border-radius: $radius-2xl;
  border: 1px solid var(--border);
  background: var(--bg-card);
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.card-back {
  transform: rotateY(180deg);
}

.card-type-tag {
  position: absolute;
  top: 16px;
  left: 16px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: $radius-md;
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.card-content-main {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  word-break: break-word;
}

.card-phonetic {
  margin-top: 8px;
  font-size: 16px;
  color: var(--text-tertiary);
  font-family: serif;
}

.card-speak {
  margin-top: 12px;
}

.card-flip-hint {
  margin-top: 24px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.card-meaning {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 20px;
}

.card-example {
  text-align: center;
  margin-bottom: 12px;

  .example-en {
    font-size: 15px;
    color: var(--text-primary);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }

  .example-zh {
    font-size: 13px;
    color: var(--text-tertiary);
    margin-top: 4px;
  }
}

.card-difficulty {
  color: #ffc107;
  font-size: 14px;
  letter-spacing: 2px;
}

// Actions
.learn-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 0;
  gap: 12px;
}

.btn-prev, .btn-next {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-secondary);
  flex-shrink: 0;

  &:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}

.mastery-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.mastery-hint {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.mastery-btn-row {
  display: flex;
  gap: 12px;
}

.btn-again, .btn-got-it {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border-radius: $radius-lg;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: 2px solid;
  transition: all 0.2s;
}

.btn-again {
  background: white;
  color: #e65100;
  border-color: #ffcc80;
  &:hover { background: #fff3e0; border-color: #e65100; }
}

.btn-got-it {
  background: white;
  color: #2e7d32;
  border-color: #a5d6a7;
  &:hover { background: #e8f5e9; border-color: #2e7d32; transform: scale(1.03); }
}

.flip-hint-action {
  flex: 1;
  display: flex;
  justify-content: center;

  .btn-flip {
    padding: 10px 28px;
    border-radius: $radius-lg;
    background: var(--primary);
    color: white;
    font-weight: 600;
    border: none;
    cursor: pointer;
    &:hover { opacity: 0.9; }
  }
}

// Complete
.complete-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.complete-card {
  background: var(--bg-card);
  border-radius: $radius-2xl;
  padding: 40px 48px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);

  .complete-icon { color: var(--primary); margin-bottom: 16px; }
  h2 { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
  p { color: var(--text-tertiary); margin-bottom: 24px; }
}

.complete-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-practice {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--primary);
  color: white;
  padding: 10px 28px;
  border-radius: $radius-lg;
  font-weight: 600;
  border: none;
  cursor: pointer;
  &:hover { opacity: 0.9; }
}

.btn-secondary {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  padding: 10px 28px;
  border-radius: $radius-lg;
  font-weight: 600;
  border: none;
  cursor: pointer;
  &:hover { background: var(--border); }
}

.btn-primary {
  background: var(--primary);
  color: white;
  padding: 10px 32px;
  border-radius: $radius-lg;
  font-weight: 600;
  border: none;
  cursor: pointer;
  &:hover { opacity: 0.9; }
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: var(--text-tertiary);

  h3 { margin-top: 12px; color: var(--text-secondary); }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
