<template>
  <div class="mistakes">
    <div class="mistakes__header">
      <div class="header-left">
        <h1>错题本</h1>
        <span class="mistakes__count" v-if="totalCount > 0">{{ totalCount }} 条记录</span>
      </div>
      <button v-if="totalCount > 0" class="btn-practice-mistakes" @click="router.push('/practice')">
        <AppIcon name="pen-line" :size="15" />
        <span>错题练习</span>
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filters">
      <div class="filter-chips">
        <button
          v-for="t in typeOptions"
          :key="t.value"
          class="chip"
          :class="{ active: filters.questionType === t.value }"
          @click="filters.questionType = t.value; loadMistakes(0)"
        >{{ t.label }}</button>
      </div>
      <div class="filter-status">
        <button
          v-for="s in statusOptions"
          :key="s.value"
          class="chip chip--status"
          :class="{ active: reviewedFilter === s.value }"
          @click="reviewedFilter = s.value; loadMistakes(0)"
        >{{ s.label }}</button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">
      <div class="spinner" />
    </div>

    <!-- 空状态 -->
    <div v-else-if="mistakes.length === 0" class="empty-state">
      <div class="empty-icon">🎉</div>
      <h3>太棒了，没有错题！</h3>
      <p>做练习时答错的题目会出现在这里</p>
      <router-link to="/levels" class="btn-go-practice">去做练习</router-link>
    </div>

    <!-- 错题列表 -->
    <div v-else class="mistake-list">
      <div
        v-for="(m, idx) in mistakes"
        :key="m.id"
        class="mistake-card"
        :class="{ reviewed: m.reviewed, expanded: expandedId === m.id }"
        :style="{ '--delay': idx * 0.04 + 's' }"
      >
        <div class="card-header" @click="toggleDetail(m.id)">
          <div class="card-header__left">
            <span class="type-tag" :class="'type--' + m.questionType">
              {{ typeLabel(m.questionType) }}
            </span>
            <span class="card-time">{{ formatTime(m.createdAt) }}</span>
          </div>
          <span class="expand-icon" :class="{ rotated: expandedId === m.id }">
            <AppIcon name="chevron-right" :size="16" />
          </span>
        </div>

        <div class="card-body" @click="toggleDetail(m.id)">
          <div class="card-stem" v-if="m.stem">{{ m.stem }}</div>
          <div class="answer-compare">
            <div class="answer-col wrong">
              <span class="answer-col__label">
                <AppIcon name="x-circle" :size="12" /> 你的答案
              </span>
              <span class="answer-col__text">{{ m.userAnswer || '—' }}</span>
            </div>
            <div class="answer-divider">
              <span class="arrow">→</span>
            </div>
            <div class="answer-col correct">
              <span class="answer-col__label">
                <AppIcon name="check-circle" :size="12" /> 正确答案
              </span>
              <span class="answer-col__text">{{ m.correctAnswer }}</span>
            </div>
          </div>
        </div>

        <!-- 展开详情 -->
        <transition name="detail">
          <div v-if="expandedId === m.id && m.explanation" class="card-detail">
            <div class="detail-row">
              <span class="detail-label">💡 解析</span>
              <span class="detail-content">{{ m.explanation }}</span>
            </div>
          </div>
        </transition>

        <div class="card-footer">
          <button v-if="!m.reviewed" class="btn-mark" @click.stop="markReviewed(m)">
            <AppIcon name="check-circle" :size="14" /> 标记已复习
          </button>
          <span v-else class="reviewed-badge">
            <AppIcon name="check-circle" :size="13" /> 已复习
          </span>
          <button class="btn-del" @click.stop="deleteMistake(m.id)">
            <AppIcon name="x-circle" :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="currentPage === 0" @click="loadMistakes(currentPage - 1)">
        <AppIcon name="chevron-left" :size="16" />
      </button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="loadMistakes(currentPage + 1)">
        <AppIcon name="chevron-right" :size="16" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mistakeApi, type MistakeRecord } from '@/api/mistakes'
import AppIcon from '@/components/AppIcon.vue'
import { showToast } from '@/composables/useToast'

const router = useRouter()

const loading = ref(false)
const mistakes = ref<MistakeRecord[]>([])
const totalCount = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const expandedId = ref<number | null>(null)

const filters = reactive({ questionType: '', bankId: undefined as number | undefined })
const reviewedFilter = ref('')

const typeOptions = [
  { value: '', label: '全部' },
  { value: 'en2zh_choice', label: '英译中' },
  { value: 'zh2en_choice', label: '中译英' },
  { value: 'fill_blank', label: '填空' },
  { value: 'translate', label: '翻译' },
]
const statusOptions = [
  { value: '', label: '全部' },
  { value: 'false', label: '待复习' },
  { value: 'true', label: '已复习' },
]

const typeLabels: Record<string, string> = {
  en2zh_choice: '英译中',
  zh2en_choice: '中译英',
  fill_blank: '填空',
  translate: '翻译',
}
function typeLabel(type: string) { return typeLabels[type] || type }

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadMistakes(page: number) {
  loading.value = true
  currentPage.value = page
  try {
    const res = await mistakeApi.list({
      questionType: filters.questionType || undefined,
      bankId: filters.bankId,
      reviewed: reviewedFilter.value === '' ? undefined : reviewedFilter.value === 'true',
      page,
      size: 15,
    })
    const data = res.data.data
    mistakes.value = data.content
    totalCount.value = data.totalElements
    totalPages.value = data.totalPages
  } catch (e) {
    console.error('加载错题失败', e)
  } finally {
    loading.value = false
  }
}

function toggleDetail(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

async function markReviewed(m: MistakeRecord) {
  try {
    await mistakeApi.markReviewed(m.id)
    m.reviewed = true
  } catch (e) {
    showToast('标记失败', 'error')
  }
}

async function deleteMistake(id: number) {
  try {
    await mistakeApi.delete(id)
    mistakes.value = mistakes.value.filter(m => m.id !== id)
    totalCount.value--
  } catch (e) {
    showToast('删除失败', 'error')
  }
}

onMounted(() => loadMistakes(0))
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.mistakes {
  max-width: 800px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
  }

  &__count {
    font-size: 13px;
    color: var(--text-muted);
    margin-left: 10px;
  }
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 0;

  h1 {
    font-size: 24px;
    font-weight: 800;
    color: var(--text-primary);
  }
}

.btn-practice-mistakes {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: $radius-full;
  background: linear-gradient(135deg, #EF4444, #DC2626);
  color: #fff;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.25);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(239, 68, 68, 0.35);
  }

  &:active {
    transform: translateY(0);
  }
}

// ---- Filters ----
.filters {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

.filter-chips,
.filter-status {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  padding: 6px 16px;
  border-radius: $radius-full;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition;

  &:hover {
    border-color: var(--primary);
    color: var(--primary);
  }

  &.active {
    background: var(--primary);
    border-color: var(--primary);
    color: #fff;
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  }
}

.chip--status.active {
  background: #10B981;
  border-color: #10B981;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

// ---- Mistake list ----
.mistake-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mistake-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-xl;
  overflow: hidden;
  transition: all $transition;
  animation: cardIn 0.35s ease-out calc(var(--delay)) both;

  &:hover {
    border-color: color-mix(in srgb, var(--primary) 30%, var(--border));
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }

  &.reviewed {
    opacity: 0.55;

    &:hover { opacity: 0.75; }
  }

  &.expanded {
    border-color: color-mix(in srgb, var(--primary) 40%, var(--border));
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  }
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px 0;
  cursor: pointer;

  &__left {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.type-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: $radius-full;

  &.type--en2zh_choice {
    background: rgba(99, 102, 241, 0.12);
    color: #818CF8;
  }
  &.type--zh2en_choice {
    background: rgba(236, 72, 153, 0.12);
    color: #F472B6;
  }
  &.type--fill_blank {
    background: rgba(245, 158, 11, 0.12);
    color: #F59E0B;
  }
  &.type--translate {
    background: rgba(16, 185, 129, 0.12);
    color: #10B981;
  }
}

.card-time {
  font-size: 12px;
  color: var(--text-muted);
}

.expand-icon {
  color: var(--text-muted);
  transition: transform 0.3s ease;
  display: flex;

  &.rotated {
    transform: rotate(90deg);
  }
}

.card-body {
  padding: 12px 18px 14px;
  cursor: pointer;
}

.card-stem {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  line-height: 1.5;
}

.answer-compare {
  display: flex;
  align-items: center;
  gap: 0;
  background: color-mix(in srgb, var(--bg-main) 60%, transparent);
  border-radius: $radius-lg;
  padding: 12px 16px;
}

.answer-col {
  flex: 1;

  &__label {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    font-weight: 500;
    margin-bottom: 6px;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }

  &__text {
    font-size: 15px;
    font-weight: 600;
    word-break: break-word;
  }

  &.wrong {
    .answer-col__label { color: #EF4444; }
    .answer-col__text { color: #EF4444; }
  }

  &.correct {
    text-align: right;
    .answer-col__label { justify-content: flex-end; color: #10B981; }
    .answer-col__text { color: #10B981; }
  }
}

.answer-divider {
  flex-shrink: 0;
  padding: 0 14px;

  .arrow {
    font-size: 18px;
    color: var(--text-muted);
    opacity: 0.4;
  }
}

// ---- Detail ----
.detail-enter-active,
.detail-leave-active {
  transition: all 0.3s ease;
  max-height: 200px;
  overflow: hidden;
}
.detail-enter-from,
.detail-leave-to {
  opacity: 0;
  max-height: 0;
}

.card-detail {
  padding: 0 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  background: color-mix(in srgb, var(--primary) 5%, var(--bg-card));
  border-radius: $radius-md;
  padding: 12px 14px;

  .detail-label {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-muted);
    margin-bottom: 6px;
  }

  .detail-content {
    font-size: 14px;
    line-height: 1.6;
    color: var(--text-primary);
  }
}

// ---- Footer ----
.card-footer {
  display: flex;
  align-items: center;
  padding: 0 18px 14px;
  gap: 8px;
}

.btn-mark {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: $radius-full;
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
  border: 1px solid rgba(16, 185, 129, 0.2);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;

  &:hover {
    background: rgba(16, 185, 129, 0.18);
    border-color: rgba(16, 185, 129, 0.35);
    transform: translateY(-1px);
  }
}

.reviewed-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 500;
  color: #10B981;
  opacity: 0.6;
  padding: 7px 14px;
}

.btn-del {
  margin-left: auto;
  padding: 7px 10px;
  border-radius: $radius-md;
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  transition: all $transition;
  opacity: 0.4;

  &:hover {
    color: #EF4444;
    background: rgba(239, 68, 68, 0.08);
    opacity: 1;
  }
}

// ---- Pagination ----
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 28px;
}

.page-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-md;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all $transition;

  &:not(:disabled):hover {
    border-color: var(--primary);
    color: var(--primary);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }
}

.page-info {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
}

// ---- Empty state ----
.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    font-size: 56px;
    margin-bottom: 16px;
  }

  h3 {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: var(--text-muted);
    margin-bottom: 24px;
  }
}

.btn-go-practice {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: $radius-full;
  background: var(--primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all $transition;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
  }
}

// ---- Loading ----
.loading {
  display: flex;
  justify-content: center;
  padding: 80px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
