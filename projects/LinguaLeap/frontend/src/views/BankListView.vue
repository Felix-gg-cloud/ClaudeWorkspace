<template>
  <div class="bank-list">
    <div class="page-header">
      <div class="page-header__left">
        <h1>题库中心</h1>
        <p class="page-header__desc">选择题库开始学习，或创建自己的题库</p>
      </div>
      <button class="btn-create" @click="showCreate = true">
        <AppIcon name="plus" :size="18" />
        创建题库
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filters">
      <div class="grade-tabs">
        <button
          v-for="g in grades"
          :key="g.value"
          class="grade-tab"
          :class="{ active: filters.grade === g.value }"
          @click="setGrade(g.value)"
        >
          {{ g.label }}
        </button>
      </div>
      <div class="search-box">
        <AppIcon name="search" :size="16" class="search-icon" />
        <input
          v-model="searchInput"
          type="text"
          placeholder="搜索题库..."
          @input="onSearchDebounce"
        />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="banks.length === 0" class="empty-state">
      <AppIcon name="book-open" :size="48" class="empty-icon" />
      <p>暂无题库</p>
      <span class="empty-hint">{{ filters.keyword ? '换个关键词试试' : '系统预制题库加载中，请稍后刷新' }}</span>
    </div>

    <!-- 题库卡片列表 -->
    <div v-else class="bank-grid">
      <div
        v-for="(bank, idx) in banks"
        :key="bank.id"
        class="bank-card"
        :style="{ '--delay': idx * 0.04 + 's' }"
        @click="goDetail(bank.id)"
      >
        <div class="bank-card__top">
          <span class="bank-badge" :class="bank.type">
            {{ bank.type === 'preset' ? '官方' : '自建' }}
          </span>
          <span class="bank-grade">{{ gradeLabel(bank.grade) }}</span>
        </div>
        <h3 class="bank-card__name">{{ bank.name }}</h3>
        <p class="bank-card__desc">{{ bank.description || '暂无描述' }}</p>
        <div class="bank-card__stats">
          <div class="stat-item">
            <AppIcon name="book-open" :size="14" />
            <span>{{ bank.kpCount }} 词</span>
          </div>
          <div class="stat-item">
            <AppIcon name="pen-line" :size="14" />
            <span>{{ bank.questionCount }} 题</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button :disabled="page === 0" @click="changePage(page - 1)">
        <AppIcon name="chevron-left" :size="16" />
      </button>
      <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages - 1" @click="changePage(page + 1)">
        <AppIcon name="chevron-right" :size="16" />
      </button>
    </div>

    <!-- 创建题库弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal-card">
        <div class="modal-header">
          <h2>创建题库</h2>
          <button class="modal-close" @click="showCreate = false">
            <AppIcon name="x-circle" :size="20" />
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>题库名称</label>
            <input v-model="form.name" type="text" placeholder="例如：七年级上册 Unit 1" />
          </div>
          <div class="form-group">
            <label>年级</label>
            <div class="grade-select">
              <button
                v-for="g in gradeOptions"
                :key="g.value"
                class="grade-option"
                :class="{ active: form.grade === g.value }"
                @click="form.grade = g.value"
              >
                {{ g.label }}
              </button>
            </div>
          </div>
          <div class="form-group">
            <label>描述（可选）</label>
            <textarea v-model="form.description" rows="3" placeholder="简要描述题库内容..." />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showCreate = false">取消</button>
          <button class="btn-submit" :disabled="!form.name || !form.grade || creating" @click="handleCreate">
            {{ creating ? '创建中...' : '创建题库' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { bankApi, type QuestionBank } from '@/api/content'
import AppIcon from '@/components/AppIcon.vue'
import { showToast } from '@/composables/useToast'

const router = useRouter()

const banks = ref<QuestionBank[]>([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const searchInput = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const filters = reactive({
  grade: '' as string,
  keyword: '' as string,
})

const grades = [
  { value: '', label: '全部' },
  { value: 'primary', label: '小学' },
  { value: 'junior', label: '初中' },
  { value: 'senior', label: '高中' },
]

const gradeOptions = [
  { value: 'primary', label: '小学' },
  { value: 'junior', label: '初中' },
  { value: 'senior', label: '高中' },
]

function gradeLabel(grade: string) {
  return grades.find(g => g.value === grade)?.label || grade
}

async function fetchBanks() {
  loading.value = true
  try {
    const params: Record<string, string | number> = { page: page.value, size: 12 }
    if (filters.grade) params.grade = filters.grade
    if (filters.keyword) params.keyword = filters.keyword
    const { data } = await bankApi.list(params)
    banks.value = data.data.content
    totalPages.value = data.data.totalPages
  } catch (e) {
    console.error('加载题库失败', e)
  } finally {
    loading.value = false
  }
}

function setGrade(val: string) {
  filters.grade = val
  page.value = 0
  fetchBanks()
}

function onSearchDebounce() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    filters.keyword = searchInput.value.trim()
    page.value = 0
    fetchBanks()
  }, 300)
}

function changePage(p: number) {
  page.value = p
  fetchBanks()
}

function goDetail(id: number) {
  router.push(`/banks/${id}`)
}

// 创建题库
const showCreate = ref(false)
const creating = ref(false)
const form = reactive({ name: '', grade: '', description: '' })

async function handleCreate() {
  if (!form.name || !form.grade) return
  creating.value = true
  try {
    const { data } = await bankApi.create({
      name: form.name,
      grade: form.grade,
      description: form.description,
    })
    showCreate.value = false
    form.name = ''
    form.grade = ''
    form.description = ''
    router.push(`/banks/${data.data.id}`)
  } catch (e: any) {
    showToast(e?.response?.data?.message || '创建题库失败', 'error')
  } finally {
    creating.value = false
  }
}

onMounted(fetchBanks)
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.bank-list {
  max-width: 960px;
}

// ---- Page header ----
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  animation: slideUp 0.3s ease-out;

  h1 {
    font-size: 26px;
    font-weight: 800;
    color: var(--text-primary);
  }

  &__desc {
    font-size: 14px;
    color: var(--text-muted);
    margin-top: 4px;
  }
}

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--gradient-primary);
  color: #fff;
  border: none;
  border-radius: $radius-lg;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;
  white-space: nowrap;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
  }
}

// ---- Filters ----
.filters {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  animation: slideUp 0.3s ease-out 0.05s both;
}

.grade-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-sidebar);
  border-radius: $radius-lg;
  padding: 4px;
}

.grade-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: calc($radius-lg - 4px);
  transition: all $transition;

  &.active {
    background: var(--bg-card);
    color: var(--primary);
    font-weight: 600;
    box-shadow: var(--shadow-sm);
  }

  &:hover:not(.active) {
    color: var(--text-primary);
  }
}

.search-box {
  position: relative;
  flex: 1;
  max-width: 280px;

  .search-icon {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    color: var(--text-muted);
  }

  input {
    width: 100%;
    padding: 9px 12px 9px 36px;
    border: 1px solid var(--border);
    border-radius: calc($radius-lg - 4px);
    background: var(--bg-card);
    color: var(--text-primary);
    font-size: 14px;
    outline: none;
    transition: border-color $transition;

    &::placeholder { color: var(--text-muted); }
    &:focus { border-color: var(--primary); }
  }
}

// ---- Bank grid ----
.bank-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 28px;
}

.bank-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-xl;
  padding: 20px;
  cursor: pointer;
  transition: all $transition;
  animation: cardStagger 0.35s ease-out calc(var(--delay)) both;

  &:hover {
    border-color: var(--primary);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
    transform: translateY(-3px);
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__name {
    font-size: 17px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 6px;
  }

  &__desc {
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.5;
    margin-bottom: 16px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__stats {
    display: flex;
    gap: 16px;
  }
}

.bank-badge {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;

  &.preset {
    background: var(--primary-light, rgba(99, 102, 241, 0.1));
    color: var(--primary);
  }

  &.user_upload {
    background: var(--success-light, rgba(16, 185, 129, 0.1));
    color: var(--success);
  }
}

.bank-grade {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--text-secondary);
}

// ---- Pagination ----
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;

  button {
    width: 36px;
    height: 36px;
    border: 1px solid var(--border);
    border-radius: $radius-lg;
    background: var(--bg-card);
    color: var(--text-secondary);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all $transition;

    &:hover:not(:disabled) {
      border-color: var(--primary);
      color: var(--primary);
    }
    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }
}

.page-info {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

// ---- Loading / Empty ----
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: var(--text-muted);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  color: var(--text-muted);
  opacity: 0.4;
  margin-bottom: 12px;
}

.empty-state p {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.empty-hint {
  font-size: 13px;
}

// ---- Modal ----
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

.modal-card {
  background: var(--bg-card);
  border-radius: $radius-2xl;
  width: 90%;
  max-width: 480px;
  box-shadow: var(--shadow-lg);
  animation: modalSlide 0.3s ease-out;
}

@keyframes modalSlide {
  from { opacity: 0; transform: translateY(20px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);

  h2 {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
}

.modal-close {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  transition: color $transition;
  &:hover { color: var(--text-primary); }
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;

  &:last-child { margin-bottom: 0; }

  label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  input, textarea {
    width: 100%;
    padding: 10px 14px;
    border: 1px solid var(--border);
    border-radius: calc($radius-lg - 4px);
    background: var(--bg-input, var(--bg-page));
    color: var(--text-primary);
    font-size: 14px;
    font-family: inherit;
    outline: none;
    transition: border-color $transition;
    resize: vertical;

    &::placeholder { color: var(--text-muted); }
    &:focus { border-color: var(--primary); }
  }

  textarea { min-height: 80px; }
}

.grade-select {
  display: flex;
  gap: 8px;
}

.grade-option {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: calc($radius-lg - 4px);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition;

  &.active {
    border-color: var(--primary);
    background: var(--primary-light, rgba(99, 102, 241, 0.08));
    color: var(--primary);
    font-weight: 600;
  }

  &:hover:not(.active) {
    border-color: var(--text-muted);
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}

.btn-cancel {
  padding: 9px 20px;
  border: 1px solid var(--border);
  border-radius: calc($radius-lg - 4px);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all $transition;
  &:hover { border-color: var(--text-muted); }
}

.btn-submit {
  padding: 9px 24px;
  border: none;
  border-radius: calc($radius-lg - 4px);
  background: var(--gradient-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;

  &:hover:not(:disabled) {
    box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
  }
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

// ---- Animations ----
@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cardStagger {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

// ---- Mobile ----
@media (max-width: 767px) {
  .page-header {
    flex-direction: column;
    gap: 12px;

    h1 { font-size: 22px; }
  }

  .btn-create { width: 100%; justify-content: center; }

  .filters {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box { max-width: 100%; }

  .bank-grid {
    grid-template-columns: 1fr;
  }
}
</style>
