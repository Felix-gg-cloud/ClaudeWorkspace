<template>
  <div class="reader-page">
    <div class="reader-topbar">
      <button class="back-btn" @click="router.push(`/study-sets/${route.params.id}`)">
        <AppIcon name="arrow-left" :size="16" />
        返回学习集
      </button>
      <h1 v-if="detail" class="reader-title">{{ detail.studySet.title }}</h1>
      <div class="topbar-spacer" />
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <div v-else-if="detail" class="reader-body" ref="readerBodyRef">
      <!-- 左侧：原文 -->
      <div class="panel panel--source">
        <div class="panel-header">
          <AppIcon name="file-text" :size="16" />
          <span>原文</span>
          <span class="panel-hint">悬浮高亮词汇查看知识点</span>
        </div>
        <div
          class="source-text"
          v-html="annotatedHtml"
          @mouseover="onSourceHover"
          @mouseout="onSourceOut"
        />
      </div>

      <!-- 右侧：知识点 -->
      <div class="panel panel--knowledge">
        <div class="panel-header">
          <AppIcon name="sparkles" :size="16" />
          <span>AI 提取的知识点</span>
          <span class="panel-count">{{ allItems.length }} 项</span>
        </div>
        <div class="knowledge-list" ref="knowledgeListRef">
          <div
            v-for="item in allItems"
            :key="item.id"
            :ref="el => setItemRef(item.id, el)"
            class="kp-card"
            :class="{
              active: activeItemId === item.id,
              'cat-vocabulary': item.category === 'vocabulary',
              'cat-grammar': item.category === 'grammar',
              'cat-sentence_pattern': item.category === 'sentence_pattern',
              'cat-passage': item.category === 'passage',
            }"
            @mouseenter="onKpEnter(item.id)"
            @mouseleave="onKpLeave"
          >
            <div class="kp-card__header">
              <span class="kp-cat">{{ categoryLabel[item.category] || item.category }}</span>
              <span v-if="item.difficulty" class="kp-diff">
                {{ '★'.repeat(item.difficulty) }}{{ '☆'.repeat(5 - item.difficulty) }}
              </span>
            </div>
            <div class="kp-card__word">
              <span class="kp-content">{{ item.content }}</span>
              <span v-if="item.phonetic" class="kp-phonetic">{{ item.phonetic }}</span>
            </div>
            <div v-if="item.meaningZh" class="kp-meaning">{{ item.meaningZh }}</div>
            <div v-if="item.exampleSentence" class="kp-example">
              <p class="kp-example-en">{{ item.exampleSentence }}</p>
              <p v-if="item.exampleZh" class="kp-example-zh">{{ item.exampleZh }}</p>
            </div>
            <div v-if="item.aiNote" class="kp-note">
              <AppIcon name="sparkles" :size="11" />
              {{ item.aiNote }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="error-state">
      <p>无法加载学习集数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { studySetApi, type StudySetDetail, type LearningItem } from '@/api/studySet'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref<StudySetDetail | null>(null)
const activeItemId = ref<number | null>(null)
const knowledgeListRef = ref<HTMLElement | null>(null)
const readerBodyRef = ref<HTMLElement | null>(null)

// 存储每个知识点卡片的 DOM 引用
const itemRefs = new Map<number, HTMLElement>()
function setItemRef(id: number, el: any) {
  if (el) itemRefs.set(id, el as HTMLElement)
}

const categoryLabel: Record<string, string> = {
  vocabulary: '词汇',
  grammar: '语法',
  sentence_pattern: '句型',
  passage: '段落',
}

// 所有知识点（合并所有分类）
const allItems = computed<LearningItem[]>(() => {
  if (!detail.value) return []
  return detail.value.items || []
})

// 构建 itemId → item 索引
const itemById = computed(() => {
  const map = new Map<number, LearningItem>()
  for (const item of allItems.value) map.set(item.id, item)
  return map
})

// 将原文中的知识点词汇包裹成可交互的 <span>
const annotatedHtml = computed(() => {
  if (!detail.value?.studySet.sourceText) return '<p class="no-source">原文暂无</p>'

  const sourceText = detail.value.studySet.sourceText
  const items = allItems.value.filter(i => i.content && i.content.trim())

  if (items.length === 0) return escapeHtml(sourceText).replace(/\n/g, '<br/>')

  // 按内容长度降序排列（优先匹配更长的词组，避免短词覆盖长词）
  const sorted = [...items].sort((a, b) => b.content.length - a.content.length)

  // 找出所有匹配位置
  interface Match { start: number; end: number; itemId: number }
  const matches: Match[] = []

  for (const item of sorted) {
    // 转义正则特殊字符
    const escaped = item.content.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    // 单词边界匹配（对英文用 \b，对中文不加）
    const isEnglish = /^[a-zA-Z\s'-]+$/.test(item.content)
    const pattern = isEnglish ? `\\b${escaped}\\b` : escaped
    const regex = new RegExp(pattern, 'gi')

    let m: RegExpExecArray | null
    while ((m = regex.exec(sourceText)) !== null) {
      const start = m.index
      const end = start + m[0].length
      // 检查是否与已有匹配重叠
      const overlaps = matches.some(
        existing => start < existing.end && end > existing.start
      )
      if (!overlaps) {
        matches.push({ start, end, itemId: item.id })
      }
    }
  }

  // 按位置排序
  matches.sort((a, b) => a.start - b.start)

  // 拼装 HTML
  let html = ''
  let cursor = 0
  for (const match of matches) {
    // 匹配前的普通文本
    if (match.start > cursor) {
      html += escapeHtml(sourceText.slice(cursor, match.start))
    }
    const matchText = sourceText.slice(match.start, match.end)
    html += `<span class="hl" data-item-id="${match.itemId}">${escapeHtml(matchText)}</span>`
    cursor = match.end
  }
  // 剩余文本
  if (cursor < sourceText.length) {
    html += escapeHtml(sourceText.slice(cursor))
  }

  return html.replace(/\n/g, '<br/>')
})

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// 悬浮原文中的高亮词
function onSourceHover(e: MouseEvent) {
  const target = (e.target as HTMLElement).closest('.hl') as HTMLElement | null
  if (!target) return
  const itemId = Number(target.dataset.itemId)
  if (!itemId) return
  activeItemId.value = itemId
  scrollToKpCard(itemId)
}

function onSourceOut(e: MouseEvent) {
  const related = e.relatedTarget as HTMLElement | null
  if (related?.closest?.('.hl')) return
  activeItemId.value = null
}

// 悬浮右侧知识卡
function onKpEnter(itemId: number) {
  activeItemId.value = itemId
  // 在原文中高亮对应的 span
  highlightSourceSpans(itemId)
}

function onKpLeave() {
  activeItemId.value = null
}

// 滚动右侧列表到对应知识卡
function scrollToKpCard(itemId: number) {
  const el = itemRefs.get(itemId)
  if (el && knowledgeListRef.value) {
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }
}

// 给原文中对应的 span 添加/移除高亮 class
watch(activeItemId, (newId, oldId) => {
  const body = readerBodyRef.value
  if (!body) return

  // 切换 has-active class
  if (newId != null) {
    body.classList.add('has-active')
  } else {
    body.classList.remove('has-active')
  }

  // 移除旧高亮
  if (oldId != null) {
    body.querySelectorAll(`.hl[data-item-id="${oldId}"]`).forEach(el => {
      el.classList.remove('hl--active')
    })
  }

  // 添加新高亮
  if (newId != null) {
    body.querySelectorAll(`.hl[data-item-id="${newId}"]`).forEach(el => {
      el.classList.add('hl--active')
    })
  }
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) { loading.value = false; return }
  try {
    const res = await studySetApi.detail(id)
    detail.value = res.data.data
  } catch {
    detail.value = null
  } finally {
    loading.value = false
    await nextTick()
  }
})
</script>

<style lang="scss" scoped>
.reader-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  overflow: hidden;
}

.reader-topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  flex-shrink: 0;
  &:hover { color: var(--text-primary); background: var(--bg-input); }
}

.reader-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-spacer { flex: 1; }

// Loading / Error
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--text-muted);
  .spinner {
    width: 32px; height: 32px;
    border: 3px solid var(--border);
    border-top-color: var(--primary);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin-bottom: 12px;
  }
}

@keyframes spin { to { transform: rotate(360deg); } }

// Two-panel body
.reader-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 0;
}

.panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel--source {
  flex: 1;
  border-right: 1px solid var(--border);
}

.panel--knowledge {
  width: 400px;
  flex-shrink: 0;
  background: var(--bg-page);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
  flex-shrink: 0;

  .panel-hint {
    margin-left: auto;
    font-size: 11px;
    font-weight: 400;
    color: var(--text-muted);
  }

  .panel-count {
    margin-left: auto;
    font-size: 12px;
    font-weight: 400;
    color: var(--text-muted);
  }
}

// Source text panel
.source-text {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  font-size: 16px;
  line-height: 2;
  color: var(--text-primary);
  background: var(--bg-card);
  word-break: break-word;

  // 高亮词汇
  :deep(.hl) {
    padding: 2px 1px;
    border-radius: 3px;
    border-bottom: 2px solid rgba(99, 102, 241, 0.4);
    cursor: pointer;
    transition: all 0.15s;

    &:hover {
      background: var(--primary-glow);
      border-bottom-color: var(--primary);
    }
  }
}

// 当某个 item 被激活时，原文中对应的 span 高亮
.reader-body.has-active {
  .source-text :deep(.hl) {
    opacity: 0.3;
    border-bottom-color: transparent;
  }
  .source-text :deep(.hl.hl--active) {
    opacity: 1;
    background: var(--primary-glow);
    border-bottom-color: var(--primary);
  }
}

// Knowledge list panel
.knowledge-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kp-card {
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  transition: all 0.2s;

  &:hover {
    box-shadow: var(--shadow-sm);
  }

  &.active {
    border-color: var(--primary);
    background: var(--primary-light);
    box-shadow: 0 0 0 1px var(--primary-glow);
  }

  // 分类色条
  border-left: 3px solid transparent;
  &.cat-vocabulary { border-left-color: #6366F1; }
  &.cat-grammar { border-left-color: #10B981; }
  &.cat-sentence_pattern { border-left-color: #F59E0B; }
  &.cat-passage { border-left-color: #EC4899; }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }

  &__word {
    margin-bottom: 4px;
  }
}

.kp-cat {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 4px;
  background: var(--bg-input);
  color: var(--text-secondary);
}

.kp-diff {
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text-muted);
}

.kp-content {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-right: 8px;
}

.kp-phonetic {
  font-size: 13px;
  color: var(--text-muted);
}

.kp-meaning {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.kp-example {
  padding: 8px 10px;
  background: var(--bg-input);
  border-radius: 8px;
  margin-bottom: 6px;

  &-en {
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-primary);
    margin-bottom: 2px;
  }
  &-zh {
    font-size: 12px;
    color: var(--text-muted);
  }
}

.kp-note {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 6px 8px;
  background: var(--primary-light);
  border-radius: 6px;
}

// Mobile responsive
@media (max-width: 768px) {
  .reader-body {
    flex-direction: column;
  }
  .panel--source {
    border-right: none;
    border-bottom: 1px solid var(--border);
    max-height: 45vh;
  }
  .panel--knowledge {
    width: 100%;
  }
}
</style>
