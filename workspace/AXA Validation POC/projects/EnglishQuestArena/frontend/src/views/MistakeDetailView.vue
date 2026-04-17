<template>
  <MainLayout>
    <div class="mistake-detail-page">
      <div class="page-header">
        <button class="back-link" @click="$router.push('/stats')">← 返回统计</button>
        <h2 class="page-title">❌ 错题本</h2>
      </div>

      <!-- Summary -->
      <div class="mistake-summary dark-panel">
        <div class="sum-item">
          <span class="sum-num">{{ mistakeStore.totalCount }}</span>
          <span class="sum-label">错题总数</span>
        </div>
        <div class="sum-item">
          <span class="sum-num text-red">{{ mistakeStore.unreviewedCount }}</span>
          <span class="sum-label">待复习</span>
        </div>
        <div class="sum-item">
          <span class="sum-num text-green">{{ reviewedCount }}</span>
          <span class="sum-label">已复习</span>
        </div>
      </div>

      <!-- Filter tabs -->
      <div class="filter-tabs">
        <button class="filter-tab" :class="{ 'tab--active': activeFilter === 'unreviewed' }"
                @click="activeFilter = 'unreviewed'">
          待复习 ({{ mistakeStore.unreviewedCount }})
        </button>
        <button class="filter-tab" :class="{ 'tab--active': activeFilter === 'reviewed' }"
                @click="activeFilter = 'reviewed'">
          已复习 ({{ reviewedCount }})
        </button>
        <button class="filter-tab" :class="{ 'tab--active': activeFilter === 'all' }"
                @click="activeFilter = 'all'">
          全部 ({{ mistakeStore.totalCount }})
        </button>
      </div>

      <!-- Mistake list -->
      <div class="mistake-list">
        <div v-for="m in filteredMistakes" :key="m.id"
             class="mistake-card dark-panel"
             :class="{ 'reviewed': m.reviewed }">
          <div class="mistake-header">
            <div class="mistake-word-info">
              <span class="mistake-word-en">{{ wordInfo(m.wordCode).en }}</span>
              <span class="mistake-word-zh">{{ wordInfo(m.wordCode).zh }}</span>
            </div>
            <div class="mistake-meta">
              <span class="mistake-type">{{ taskTypeLabel(m.taskType) }}</span>
              <span v-if="m.reviewed" class="reviewed-badge">✓ 已复习</span>
            </div>
          </div>

          <div class="mistake-body">
            <div class="mistake-prompt">
              <span class="prompt-label">题目</span>
              <span class="prompt-text">{{ m.promptEn }}</span>
            </div>
            <div v-if="m.promptZhHint" class="mistake-hint">
              <span class="prompt-label">提示</span>
              <span class="prompt-text">{{ m.promptZhHint }}</span>
            </div>
            <div class="answer-row">
              <div v-if="m.userAnswer" class="answer-item answer-wrong">
                <span class="answer-label">你的答案</span>
                <span class="answer-text">{{ m.userAnswer }}</span>
              </div>
              <div class="answer-item answer-correct">
                <span class="answer-label">正确答案</span>
                <span class="answer-text">{{ m.correctAnswer }}</span>
              </div>
            </div>
          </div>

          <div class="mistake-footer">
            <span class="mistake-time">{{ formatTime(m.timestamp) }}</span>
            <button v-if="!m.reviewed" class="btn-review" @click.stop="markReviewed(m.id)">
              标记已复习
            </button>
            <span v-else class="review-count">复习 {{ m.reviewCount }} 次</span>
          </div>
        </div>

        <div v-if="filteredMistakes.length === 0" class="empty-hint">
          {{ activeFilter === 'unreviewed' ? '🎉 没有待复习的错题！' : '暂无错题记录' }}
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useMistakeStore } from '@/stores/mistakes'
import { chapterConfigs } from '@/mock/chapters'
import type { ContentItem } from '@/types'

const mistakeStore = useMistakeStore()
const activeFilter = ref<'all' | 'unreviewed' | 'reviewed'>('unreviewed')

// Build word lookup
const wordLookup = computed(() => {
  const map: Record<string, ContentItem> = {}
  for (const ch of chapterConfigs) {
    for (const w of ch.words) {
      map[w.code] = w
    }
  }
  return map
})

function wordInfo(code: string): { en: string; zh: string } {
  const w = wordLookup.value[code]
  if (w) return { en: w.en, zh: w.zh }
  return { en: code.replace('W_', '').toLowerCase(), zh: '' }
}

const reviewedCount = computed(() =>
  mistakeStore.mistakes.filter(m => m.reviewed).length
)

const filteredMistakes = computed(() => {
  const sorted = [...mistakeStore.mistakes].sort(
    (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
  )
  if (activeFilter.value === 'unreviewed') return sorted.filter(m => !m.reviewed)
  if (activeFilter.value === 'reviewed') return sorted.filter(m => m.reviewed)
  return sorted
})

function markReviewed(id: string) {
  mistakeStore.markReviewed(id)
}

function taskTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    MCQ: '选择题',
    MCQ_REVERSE: '反向选择',
    FLASHCARD: '闪卡',
    SPELLING: '拼写',
    WORD_ORDER: '词序',
    FILL_BLANK: '填空',
    LISTEN_PICK: '听力选择',
    LISTEN_FILL: '听力填空',
    DIALOGUE_REVIEW: '对话',
    SITUATION_PICK: '情景选择',
    ERROR_FIX: '纠错',
    CAMP_ENCOUNTER: '营地遭遇',
  }
  return labels[type] ?? type
}

function formatTime(iso: string): string {
  const d = new Date(iso)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const h = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}/${day} ${h}:${min}`
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.mistake-detail-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 16px 60px;
}

.page-header {
  margin-bottom: 20px;
}

.back-link {
  background: none;
  border: none;
  color: $text-muted;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
  margin-bottom: 8px;
  display: block;
  &:hover { color: $text-primary; }
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
}

.mistake-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 16px;
  margin-bottom: 16px;
  text-align: center;
}

.sum-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sum-num {
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
}

.sum-label {
  font-size: 11px;
  color: $text-muted;
}

.text-red { color: $hp-red; }
.text-green { color: $life-green; }

.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-tab {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid $border-dim;
  color: $text-secondary;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;

  &.tab--active {
    background: rgba($hp-red, 0.15);
    border-color: rgba($hp-red, 0.4);
    color: $hp-red;
  }
}

.mistake-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mistake-card {
  padding: 14px 16px;
  border-left: 3px solid $hp-red;
  transition: all 0.2s;

  &.reviewed {
    border-left-color: $life-green;
    opacity: 0.7;
  }
}

.mistake-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.mistake-word-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.mistake-word-en {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.mistake-word-zh {
  font-size: 13px;
  color: $text-muted;
}

.mistake-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mistake-type {
  font-size: 11px;
  color: $text-muted;
  background: rgba(255, 255, 255, 0.06);
  padding: 2px 8px;
  border-radius: 10px;
}

.reviewed-badge {
  font-size: 11px;
  color: $life-green;
}

.mistake-body {
  margin-bottom: 10px;
}

.mistake-prompt,
.mistake-hint {
  display: flex;
  gap: 8px;
  padding: 3px 0;
  font-size: 13px;
}

.prompt-label {
  color: $text-muted;
  width: 32px;
  flex-shrink: 0;
}

.prompt-text {
  color: $text-secondary;
}

.answer-row {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.answer-item {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
}

.answer-wrong {
  background: rgba($hp-red, 0.1);
  border: 1px solid rgba($hp-red, 0.3);
}

.answer-correct {
  background: rgba($life-green, 0.1);
  border: 1px solid rgba($life-green, 0.3);
}

.answer-label {
  display: block;
  font-size: 11px;
  color: $text-muted;
  margin-bottom: 2px;
}

.answer-text {
  font-weight: 600;
  color: $text-primary;
}

.mistake-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mistake-time {
  font-size: 11px;
  color: $text-muted;
}

.btn-review {
  padding: 5px 14px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  background: rgba($gold, 0.15);
  border: 1px solid rgba($gold, 0.4);
  color: $gold;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: rgba($gold, 0.25); }
}

.review-count {
  font-size: 11px;
  color: $text-muted;
}

.empty-hint {
  text-align: center;
  padding: 40px;
  color: $text-muted;
  font-size: 14px;
}
</style>
