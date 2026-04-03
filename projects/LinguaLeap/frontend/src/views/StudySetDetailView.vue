<template>
  <div class="study-set-detail">
    <button class="back-btn" @click="router.push('/levels')">
      <AppIcon name="arrow-left" :size="16" />
      返回知识库
    </button>

    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <template v-else-if="detail">
      <!-- Header -->
      <div class="detail-header">
        <div class="detail-header__info">
          <div class="detail-header__meta">
            <span class="status-badge" :class="detail.studySet.status">
              {{ statusLabel[detail.studySet.status] }}
            </span>
            <span class="grade-badge">{{ detail.studySet.grade }}</span>
            <span class="item-count">{{ detail.studySet.itemCount }} 个知识点</span>
          </div>
          <h1>{{ detail.studySet.title }}</h1>
        </div>
        <div class="detail-header__actions">
          <button class="btn-reader" @click="router.push(`/study-sets/${route.params.id}/reader`)">
            <AppIcon name="book-open" :size="16" />
            原文阅读
          </button>
          <button class="btn-practice" @click="startPractice">
            <AppIcon name="pen-line" :size="16" />
            开始练习
          </button>
          <button class="btn-delete" @click="handleDelete">
            <AppIcon name="trash-2" :size="16" />
          </button>
        </div>
      </div>

      <!-- AI 分析概要 -->
      <div v-if="detail.studySet.aiSummary || strategy" class="strategy-card">
        <h3><AppIcon name="sparkles" :size="16" /> AI 分析</h3>
        <div class="strategy-content">
          <p v-if="detail.studySet.aiSummary" class="strategy-summary">
            {{ detail.studySet.aiSummary }}
          </p>
          <!-- 知识点分布气泡图 -->
          <div v-if="detail.categoryCounts && Object.keys(detail.categoryCounts).length" class="distribution">
            <span class="distribution-title">知识点分布</span>
            <div class="bubble-chart">
              <div
                v-for="b in bubbles"
                :key="b.key"
                class="bubble"
                :style="{
                  width: b.size + 'px',
                  height: b.size + 'px',
                  background: b.color + '18',
                  borderColor: b.color + '50',
                  boxShadow: `0 0 24px ${b.color}15`,
                }"
              >
                <span class="bubble-count" :style="{ color: b.color }">{{ b.count }}</span>
                <span class="bubble-label">{{ b.label }}</span>
                <span class="bubble-pct" :style="{ color: b.color }">{{ b.pct }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Category Tabs -->
      <div class="category-section">
        <div class="category-tabs">
          <button
            v-for="cat in categories"
            :key="cat.key"
            class="cat-tab"
            :class="{ active: activeCategory === cat.key }"
            @click="activeCategory = cat.key"
          >
            <AppIcon :name="cat.icon" :size="16" />
            {{ cat.label }}
            <span class="cat-count">{{ detail.categoryCounts[cat.key] || 0 }}</span>
          </button>
        </div>

        <!-- Items List -->
        <div class="items-list">
          <div v-if="!currentItems.length" class="empty-cat">
            此分类暂无知识点
          </div>
          <div
            v-for="item in currentItems"
            :key="item.id"
            class="item-card"
            :class="{ expanded: expandedId === item.id }"
            @click="expandedId = expandedId === item.id ? null : item.id"
          >
            <div class="item-card__main">
              <div class="item-card__content">
                <span class="item-word">{{ item.content }}</span>
                <span v-if="item.phonetic" class="item-phonetic">{{ item.phonetic }}</span>
              </div>
              <span class="item-meaning">{{ item.meaningZh }}</span>
              <AppIcon name="chevron-down" :size="14" class="expand-icon" />
            </div>
            <Transition name="expand">
              <div v-if="expandedId === item.id" class="item-card__detail">
                <div v-if="item.exampleSentence" class="example">
                  <p class="example-en">{{ item.exampleSentence }}</p>
                  <p v-if="item.exampleZh" class="example-zh">{{ item.exampleZh }}</p>
                </div>
                <div v-if="item.aiNote" class="ai-note">
                  <AppIcon name="sparkles" :size="12" /> {{ item.aiNote }}
                </div>
                <div class="item-meta">
                  <span v-if="item.difficulty" class="difficulty">
                    难度: {{ '★'.repeat(item.difficulty) }}{{ '☆'.repeat(5 - item.difficulty) }}
                  </span>
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="error-state">
      <AppIcon name="alert-circle" :size="48" />
      <p>学习集不存在或已删除</p>
      <button class="back-btn" @click="router.push('/levels')">返回知识库</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { studySetApi, type StudySetDetail, type LearningItem } from '@/api/studySet'
import AppIcon from '@/components/AppIcon.vue'
import { showToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref<StudySetDetail | null>(null)
const activeCategory = ref('vocabulary')
const expandedId = ref<number | null>(null)

const statusLabel: Record<string, string> = {
  processing: '处理中',
  ready: '可用',
  failed: '失败',
}

const categoryLabel: Record<string, string> = {
  vocabulary: '词汇',
  grammar: '语法',
  sentence_pattern: '句型',
  passage: '段落',
}

const categories = [
  { key: 'vocabulary', label: '词汇', icon: 'type' },
  { key: 'grammar', label: '语法', icon: 'layers' },
  { key: 'sentence_pattern', label: '句型', icon: 'message-square' },
  { key: 'passage', label: '段落', icon: 'file-text' },
]

const strategy = computed(() => {
  if (!detail.value?.studySet.aiStrategy) return null
  try { return JSON.parse(detail.value.studySet.aiStrategy) } catch { return null }
})

const categoryColors: Record<string, string> = {
  vocabulary: '#6366F1',
  grammar: '#10B981',
  sentence_pattern: '#F59E0B',
  passage: '#EC4899',
}

const bubbles = computed(() => {
  if (!detail.value?.categoryCounts) return []
  const counts = detail.value.categoryCounts
  const total = Object.values(counts).reduce((s, n) => s + (n as number), 0) as number
  if (total === 0) return []

  const maxCount = Math.max(...categories.map(c => (counts[c.key] as number) || 0))
  const minSize = 76
  const maxSize = 116

  return categories
    .map(cat => {
      const count = (counts[cat.key] as number) || 0
      if (count === 0) return null
      const pct = Math.round((count / total) * 100)
      const size = minSize + (maxSize - minSize) * Math.sqrt(count / maxCount)
      return {
        key: cat.key,
        label: cat.label,
        count,
        pct,
        color: categoryColors[cat.key] || '#888',
        size: Math.round(size),
      }
    })
    .filter(Boolean) as { key: string; label: string; count: number; pct: number; color: string; size: number }[]
})

const currentItems = computed<LearningItem[]>(() => {
  return detail.value?.groupedItems[activeCategory.value] || []
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) { loading.value = false; return }
  try {
    const res = await studySetApi.detail(id)
    detail.value = res.data.data
    // 自动选中第一个有内容的分类
    for (const cat of categories) {
      if ((detail.value?.categoryCounts[cat.key] || 0) > 0) {
        activeCategory.value = cat.key
        break
      }
    }
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
})

function startPractice() {
  const d = detail.value
  const params = new URLSearchParams({
    studySetId: String(route.params.id),
    studySetName: d?.title || '学习集练习',
    ...(d?.grade ? { grade: d.grade } : {}),
  })
  router.push(`/practice?${params.toString()}`)
}

async function handleDelete() {
  if (!confirm('确定删除此学习集？所有知识点也将一并删除。')) return
  try {
    await studySetApi.delete(Number(route.params.id))
    router.push('/levels')
  } catch {
    showToast('删除失败', 'error')
  }
}
</script>

<style lang="scss" scoped>
.study-set-detail {
  max-width: 960px;
  margin: 0 auto;
  animation: fadeUp 0.4s ease-out;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-secondary, #64748B);
  font-size: 13px;
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.2s;
  &:hover { color: var(--text-primary, #0F172A); border-color: rgba(99, 102, 241, 0.3); }
}

.loading-state, .error-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted, #94A3B8);
  .spinner {
    width: 32px; height: 32px;
    border: 3px solid var(--border, #E2E8F0);
    border-top-color: #6366F1;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin: 0 auto 12px;
  }
}

@keyframes spin { to { transform: rotate(360deg); } }

// Header
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;

  &__meta {
    display: flex; align-items: center; gap: 8px;
    margin-bottom: 10px;
  }

  h1 { font-size: 24px; font-weight: 800; color: var(--text-primary, #0F172A); }

  &__actions {
    display: flex; gap: 8px; flex-shrink: 0;
  }
}

.status-badge {
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px; font-weight: 600;
  &.ready { background: rgba(16, 185, 129, 0.1); color: #059669; }
  &.processing { background: rgba(245, 158, 11, 0.1); color: #D97706; }
  &.failed { background: rgba(239, 68, 68, 0.1); color: #DC2626; }
}
.grade-badge {
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  background: var(--bg-card, #F1F5F9);
  border: 1px solid var(--border, #E2E8F0);
  color: var(--text-secondary, #64748B);
}
.item-count {
  font-size: 12px;
  color: var(--text-muted, #94A3B8);
}

.btn-reader {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
  border: 1px solid rgba(99, 102, 241, 0.25);
  background: rgba(99, 102, 241, 0.06);
  color: #6366F1;
  font-size: 14px; font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  &:hover { background: rgba(99, 102, 241, 0.12); transform: translateY(-1px); }
}
.btn-practice {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: #fff;
  font-size: 14px; font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  transition: all 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4); }
}
.btn-delete {
  padding: 10px; border-radius: 12px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-muted, #94A3B8);
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: #EF4444; border-color: rgba(239, 68, 68, 0.3); background: rgba(239, 68, 68, 0.04); }
}

// Strategy
.strategy-card {
  margin-bottom: 24px;
  padding: 22px;
  border-radius: 18px;
  border: 1px solid rgba(99, 102, 241, 0.12);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.04), var(--bg-card, #fff));

  h3 {
    display: flex; align-items: center; gap: 8px;
    font-size: 15px; font-weight: 700; margin-bottom: 16px;
    color: #6366F1;
  }
}
.strategy-content {
  display: flex; flex-direction: column; gap: 12px;
}
.strategy-summary {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary, #64748B);
  padding: 12px 16px;
  background: rgba(99, 102, 241, 0.04);
  border-radius: 12px;
}

// Bubble chart
.distribution {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.distribution-title {
  font-size: 12px;
  color: var(--text-muted, #94A3B8);
  font-weight: 600;
}
.bubble-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 12px 0;
  flex-wrap: wrap;
}
.bubble {
  border-radius: 50%;
  border: 2px solid;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: default;
  &:hover {
    transform: scale(1.08);
  }
}
.bubble-count {
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}
.bubble-label {
  font-size: 12px;
  color: var(--text-secondary, #64748B);
  margin-top: 3px;
}
.bubble-pct {
  font-size: 11px;
  font-weight: 600;
  margin-top: 1px;
  opacity: 0.7;
}

// Categories
.category-section { margin-top: 8px; }
.category-tabs {
  display: flex; gap: 4px;
  border-bottom: 1px solid var(--border, #E2E8F0);
  margin-bottom: 16px;
  overflow-x: auto;
}
.cat-tab {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 16px;
  border: none;
  background: none;
  color: var(--text-muted, #94A3B8);
  font-size: 14px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: all 0.2s;

  &:hover { color: var(--text-secondary, #64748B); }
  &.active {
    color: #6366F1;
    border-bottom-color: #6366F1;
    font-weight: 600;
  }
}
.cat-count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  background: var(--bg-card, #F1F5F9);
  border: 1px solid var(--border, #E2E8F0);
}
.cat-tab.active .cat-count {
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.2);
  color: #6366F1;
}

// Items
.empty-cat {
  text-align: center;
  padding: 40px;
  color: var(--text-muted, #94A3B8);
  font-size: 14px;
}

.items-list { display: flex; flex-direction: column; gap: 6px; }

.item-card {
  border-radius: 14px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  cursor: pointer;
  transition: all 0.2s;

  &:hover { border-color: rgba(99, 102, 241, 0.2); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04); }
  &.expanded { border-color: rgba(99, 102, 241, 0.25); background: linear-gradient(135deg, rgba(99, 102, 241, 0.02), var(--bg-card, #fff)); }

  &__main {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
  }

  &__content { flex: 1; min-width: 0; }
  &__detail {
    padding: 0 16px 14px;
    border-top: 1px solid var(--border, #E2E8F0);
    margin: 0 16px;
    padding-top: 12px;
  }
}

.item-word {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  margin-right: 8px;
}
.item-phonetic {
  font-size: 13px;
  color: var(--text-muted, #94A3B8);
}
.item-meaning {
  font-size: 14px;
  color: var(--text-secondary, #64748B);
  text-align: right;
  flex-shrink: 0;
  max-width: 40%;
}
.expand-icon {
  color: var(--text-muted, #CBD5E1);
  transition: transform 0.2s;
  flex-shrink: 0;
  .expanded & { transform: rotate(180deg); }
}

.example {
  margin-bottom: 10px;
  &-en {
    font-size: 14px;
    line-height: 1.6;
    color: var(--text-primary, #0F172A);
    margin-bottom: 4px;
  }
  &-zh {
    font-size: 13px;
    color: var(--text-muted, #94A3B8);
  }
}

.ai-note {
  display: flex; align-items: flex-start; gap: 4px;
  font-size: 13px;
  color: var(--text-secondary, #64748B);
  padding: 8px 12px;
  background: rgba(99, 102, 241, 0.04);
  border-radius: 10px;
  margin-bottom: 8px;
}

.item-meta {
  font-size: 12px;
  color: var(--text-muted, #94A3B8);
}
.difficulty { letter-spacing: 1px; }

.expand-enter-active, .expand-leave-active {
  transition: all 0.2s;
  overflow: hidden;
}
.expand-enter-from, .expand-leave-to {
  opacity: 0;
  max-height: 0;
}

@media (max-width: 640px) {
  .detail-header { flex-direction: column; }
  .detail-header__actions { width: 100%; }
  .btn-reader, .btn-practice { flex: 1; justify-content: center; }
  .category-tabs { gap: 0; }
}
</style>
