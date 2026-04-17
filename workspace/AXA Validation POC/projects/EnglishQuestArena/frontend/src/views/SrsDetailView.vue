<template>
  <MainLayout>
    <div class="srs-detail-page">
      <div class="page-header">
        <button class="back-link" @click="$router.push('/stats')">← 返回统计</button>
        <h2 class="page-title">🧠 记忆词库</h2>
      </div>

      <!-- Summary -->
      <div class="srs-summary dark-panel">
        <div class="sum-item">
          <span class="sum-num text-green">{{ masteredCount }}</span>
          <span class="sum-label">已掌握</span>
        </div>
        <div class="sum-item">
          <span class="sum-num text-gold">{{ reviewingCount }}</span>
          <span class="sum-label">复习中</span>
        </div>
        <div class="sum-item">
          <span class="sum-num text-red">{{ newCount }}</span>
          <span class="sum-label">新学</span>
        </div>
        <div class="sum-item">
          <span class="sum-num text-blue">{{ srsStore.dueCards.length }}</span>
          <span class="sum-label">今日待复习</span>
        </div>
      </div>

      <!-- Filter tabs -->
      <div class="filter-tabs">
        <button v-for="f in filters" :key="f.key" class="filter-tab"
                :class="{ 'tab--active': activeFilter === f.key }"
                @click="activeFilter = f.key">
          {{ f.label }} ({{ f.count }})
        </button>
      </div>

      <!-- Word list -->
      <div class="word-list">
        <div v-for="word in filteredWords" :key="word.code"
             class="word-card dark-panel"
             :class="'mastery-' + word.mastery"
             @click="toggleExpand(word.code)">
          <div class="word-main">
            <div class="word-left">
              <span class="mastery-dot" :class="'dot-' + word.mastery"></span>
              <span class="word-en">{{ word.en }}</span>
              <span class="word-phonetic">{{ word.phonetic }}</span>
            </div>
            <div class="word-right">
              <span class="word-zh">{{ word.zh }}</span>
              <span class="word-interval">{{ intervalLabel(word.interval) }}</span>
            </div>
          </div>
          <transition name="expand">
            <div v-if="expandedCode === word.code" class="word-detail">
              <div class="detail-row">
                <span class="detail-label">例句</span>
                <span class="detail-value">{{ word.sentence }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">翻译</span>
                <span class="detail-value">{{ word.sentenceZh }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">复习次数</span>
                <span class="detail-value">{{ word.reviewCount }} 次</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">连续正确</span>
                <span class="detail-value">{{ word.correctStreak }} 次</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">下次复习</span>
                <span class="detail-value">{{ word.nextReviewAt }}</span>
              </div>
            </div>
          </transition>
        </div>

        <div v-if="filteredWords.length === 0" class="empty-hint">
          暂无词汇数据
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useSrsStore } from '@/stores/srs'
import { chapterConfigs } from '@/mock/chapters'
import type { ContentItem } from '@/types'

const srsStore = useSrsStore()
const activeFilter = ref<'all' | 'mastered' | 'reviewing' | 'new'>('all')
const expandedCode = ref<string | null>(null)

onMounted(() => srsStore.loadAll())

// Build word lookup from chapter configs
const wordLookup = computed(() => {
  const map: Record<string, ContentItem> = {}
  for (const ch of chapterConfigs) {
    for (const w of ch.words) {
      map[w.code] = w
    }
  }
  return map
})

interface WordDisplay {
  code: string
  en: string
  zh: string
  phonetic: string
  sentence: string
  sentenceZh: string
  mastery: 'mastered' | 'reviewing' | 'new'
  interval: number
  reviewCount: number
  correctStreak: number
  nextReviewAt: string
}

function getMastery(interval: number, correctStreak: number): 'mastered' | 'reviewing' | 'new' {
  if (correctStreak >= 3 && interval >= 7) return 'mastered'
  if (correctStreak >= 1) return 'reviewing'
  return 'new'
}

const allWords = computed<WordDisplay[]>(() => {
  return Object.values(srsStore.cards).map(card => {
    const info = wordLookup.value[card.wordCode]
    return {
      code: card.wordCode,
      en: info?.en ?? card.wordCode.replace('W_', '').toLowerCase(),
      zh: info?.zh ?? '',
      phonetic: info?.phonetic ?? '',
      sentence: info?.sentence ?? '',
      sentenceZh: info?.sentenceZh ?? '',
      mastery: getMastery(card.interval, card.correctStreak),
      interval: card.interval,
      reviewCount: card.reviewCount,
      correctStreak: card.correctStreak,
      nextReviewAt: card.nextReviewAt,
    }
  }).sort((a, b) => {
    const order = { new: 0, reviewing: 1, mastered: 2 }
    return order[a.mastery] - order[b.mastery]
  })
})

const masteredCount = computed(() => allWords.value.filter(w => w.mastery === 'mastered').length)
const reviewingCount = computed(() => allWords.value.filter(w => w.mastery === 'reviewing').length)
const newCount = computed(() => allWords.value.filter(w => w.mastery === 'new').length)

const filters = computed(() => [
  { key: 'all' as const, label: '全部', count: allWords.value.length },
  { key: 'mastered' as const, label: '已掌握', count: masteredCount.value },
  { key: 'reviewing' as const, label: '复习中', count: reviewingCount.value },
  { key: 'new' as const, label: '新学', count: newCount.value },
])

const filteredWords = computed(() => {
  if (activeFilter.value === 'all') return allWords.value
  return allWords.value.filter(w => w.mastery === activeFilter.value)
})

function toggleExpand(code: string) {
  expandedCode.value = expandedCode.value === code ? null : code
}

function intervalLabel(days: number): string {
  if (days <= 1) return '1天'
  if (days < 7) return days + '天'
  if (days < 30) return Math.round(days / 7) + '周'
  return Math.round(days / 30) + '月'
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.srs-detail-page {
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

.srs-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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
}

.sum-label {
  font-size: 11px;
  color: $text-muted;
}

.text-green { color: $life-green; }
.text-gold { color: $gold; }
.text-red { color: $hp-red; }
.text-blue { color: $mana-blue; }

.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
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
    background: rgba($gold, 0.15);
    border-color: rgba($gold, 0.4);
    color: $gold;
  }
}

.word-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.word-card {
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;

  &.mastery-mastered { border-left-color: $life-green; }
  &.mastery-reviewing { border-left-color: $gold; }
  &.mastery-new { border-left-color: $hp-red; }
}

.word-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.word-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mastery-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.dot-mastered { background: $life-green; }
  &.dot-reviewing { background: $gold; }
  &.dot-new { background: $hp-red; }
}

.word-en {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.word-phonetic {
  font-size: 12px;
  color: $text-muted;
}

.word-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.word-zh {
  font-size: 14px;
  color: $text-secondary;
}

.word-interval {
  font-size: 11px;
  color: $text-muted;
  background: rgba(255, 255, 255, 0.06);
  padding: 2px 8px;
  border-radius: 10px;
}

.word-detail {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid $border-dim;
}

.detail-row {
  display: flex;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}

.detail-label {
  color: $text-muted;
  width: 64px;
  flex-shrink: 0;
}

.detail-value {
  color: $text-secondary;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 200px;
}

.empty-hint {
  text-align: center;
  padding: 40px;
  color: $text-muted;
  font-size: 14px;
}
</style>
