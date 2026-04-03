<template>
  <div class="bank-detail">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="router.push('/banks')">
      <AppIcon name="arrow-left" :size="16" />
      返回题库列表
    </button>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <template v-else-if="bank">
      <!-- 头部信息 -->
      <div class="detail-header">
        <div class="detail-header__info">
          <div class="detail-header__meta">
            <span class="bank-badge" :class="bank.type">
              {{ bank.type === 'preset' ? '官方' : '自建' }}
            </span>
            <span class="bank-grade">{{ gradeLabel(bank.grade) }}</span>
          </div>
          <h1>{{ bank.name }}</h1>
          <p class="detail-desc" v-if="bank.description">{{ bank.description }}</p>
        </div>
        <div class="detail-header__actions" v-if="bank.type !== 'preset'">
          <button class="btn-outline btn-danger" @click="handleDelete">
            <AppIcon name="x-circle" :size="16" />
            删除
          </button>
        </div>
      </div>

      <!-- 统计条 -->
      <div class="stats-bar">
        <div class="stats-item">
          <span class="stats-number">{{ bank.kpCount }}</span>
          <span class="stats-label">知识点</span>
        </div>
        <div class="stats-divider" />
        <div class="stats-item">
          <span class="stats-number">{{ bank.questionCount }}</span>
          <span class="stats-label">题目</span>
        </div>
        <div class="stats-divider" />
        <div class="stats-item">
          <span class="stats-number">{{ bank.status === 'active' ? '可用' : bank.status }}</span>
          <span class="stats-label">状态</span>
        </div>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <button class="btn-primary" @click="router.push(`/practice?bankId=${bankId}`)">
          <AppIcon name="pen-line" :size="16" />
          开始练习
        </button>
        <button class="btn-ai" @click="showAiDialog = true">
          <AppIcon name="sparkles" :size="16" />
          AI 出题
        </button>
      </div>

      <!-- AI 出题弹窗 -->
      <Transition name="fade">
        <div v-if="showAiDialog" class="modal-overlay" @click.self="showAiDialog = false">
          <div class="modal-content">
            <div class="modal-header">
              <h3><AppIcon name="sparkles" :size="18" /> AI 智能出题</h3>
              <button class="modal-close" @click="showAiDialog = false">
                <AppIcon name="x-circle" :size="20" />
              </button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label>题型（可多选）</label>
                <div class="type-checkboxes">
                  <label v-for="t in questionTypes" :key="t.value" class="checkbox-label">
                    <input type="checkbox" v-model="aiTypes" :value="t.value" />
                    {{ t.label }}
                  </label>
                </div>
              </div>
              <div class="form-group">
                <label>生成数量</label>
                <input type="number" v-model.number="aiCount" min="1" max="20" class="form-input" />
              </div>
              <div class="form-group">
                <label>难度等级</label>
                <select v-model="aiGrade" class="form-input">
                  <option value="小学">小学</option>
                  <option value="初中">初中</option>
                  <option value="高中">高中</option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn-outline" @click="showAiDialog = false">取消</button>
              <button class="btn-primary" :disabled="aiGenerating || aiTypes.length === 0" @click="handleAiGenerate">
                <template v-if="aiGenerating">
                  <div class="spinner-small" />
                  AI 正在出题...
                </template>
                <template v-else>
                  <AppIcon name="sparkles" :size="16" />
                  开始生成
                </template>
              </button>
            </div>
            <!-- 生成结果 -->
            <div v-if="aiResult" class="ai-result">
              <div class="ai-result__summary" :class="{ success: aiResult.failed === 0 }">
                <AppIcon name="check-circle" :size="16" />
                成功 {{ aiResult.success }} 题
                <template v-if="aiResult.failed > 0">，失败 {{ aiResult.failed }} 题</template>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 知识点筛选 -->
      <div class="kp-toolbar">
        <h2>知识点列表</h2>
        <div class="kp-filters">
          <div class="search-box">
            <AppIcon name="search" :size="16" class="search-icon" />
            <input
              v-model="kpSearch"
              type="text"
              placeholder="搜索单词或释义..."
              @input="onKpSearch"
            />
          </div>
          <select v-model="kpDifficulty" class="diff-select" @change="fetchKps">
            <option value="">全部难度</option>
            <option value="1">难度 1</option>
            <option value="2">难度 2</option>
            <option value="3">难度 3</option>
            <option value="4">难度 4</option>
            <option value="5">难度 5</option>
          </select>
        </div>
      </div>

      <!-- 知识点列表 -->
      <div v-if="kpLoading" class="loading-state small">
        <div class="spinner" />
      </div>
      <div v-else-if="kps.length === 0" class="empty-state small">
        <p>暂无知识点</p>
      </div>
      <div v-else class="kp-list">
        <div
          v-for="(kp, idx) in kps"
          :key="kp.id"
          class="kp-card"
          :style="{ '--delay': idx * 0.03 + 's' }"
          @click="toggleExpand(kp.id)"
        >
          <div class="kp-card__main">
            <div class="kp-word">
              <span class="kp-content">{{ kp.content }}</span>
              <span class="kp-phonetic" v-if="kp.phonetic">{{ kp.phonetic }}</span>
            </div>
            <span class="kp-meaning">{{ kp.meaningZh }}</span>
            <div class="kp-card__right">
              <span class="kp-diff" :class="'diff-' + kp.difficulty">
                {{ '★'.repeat(kp.difficulty) }}
              </span>
              <AppIcon
                :name="expandedId === kp.id ? 'chevron-down' : 'chevron-right'"
                :size="14"
                class="kp-expand-icon"
              />
            </div>
          </div>
          <Transition name="expand">
            <div v-if="expandedId === kp.id" class="kp-card__detail">
              <div class="kp-example" v-if="kp.exampleSentence">
                <span class="kp-example__en">{{ kp.exampleSentence }}</span>
                <span class="kp-example__zh">{{ kp.exampleZh }}</span>
              </div>
              <div class="kp-tags" v-if="parseTags(kp.tags).length">
                <span v-for="tag in parseTags(kp.tags)" :key="tag" class="kp-tag">{{ tag }}</span>
              </div>
            </div>
          </Transition>
        </div>
      </div>

      <!-- 知识点分页 -->
      <div v-if="kpTotalPages > 1" class="pagination">
        <button :disabled="kpPage === 0" @click="changeKpPage(kpPage - 1)">
          <AppIcon name="chevron-left" :size="16" />
        </button>
        <span class="page-info">{{ kpPage + 1 }} / {{ kpTotalPages }}</span>
        <button :disabled="kpPage >= kpTotalPages - 1" @click="changeKpPage(kpPage + 1)">
          <AppIcon name="chevron-right" :size="16" />
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bankApi, kpApi, type QuestionBank, type KnowledgePoint } from '@/api/content'
import { aiApi, type AiBatchResult } from '@/api/ai'
import AppIcon from '@/components/AppIcon.vue'
import { showToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const bankId = Number(route.params.id)

const bank = ref<QuestionBank | null>(null)
const loading = ref(true)

const kps = ref<KnowledgePoint[]>([])
const kpLoading = ref(false)
const kpPage = ref(0)
const kpTotalPages = ref(0)
const kpSearch = ref('')
const kpDifficulty = ref('')
const expandedId = ref<number | null>(null)
let kpSearchTimer: ReturnType<typeof setTimeout> | null = null

// AI 出题相关
const showAiDialog = ref(false)
const aiGenerating = ref(false)
const aiResult = ref<AiBatchResult | null>(null)
const aiTypes = ref<string[]>(['en2zh_choice', 'zh2en_choice'])
const aiCount = ref(5)
const aiGrade = ref('初中')

const questionTypes = [
  { value: 'en2zh_choice', label: '英译中选择' },
  { value: 'zh2en_choice', label: '中译英选择' },
  { value: 'fill_blank', label: '填空题' },
  { value: 'translate', label: '翻译题' },
]

const grades = [
  { value: 'primary', label: '小学' },
  { value: 'junior', label: '初中' },
  { value: 'senior', label: '高中' },
]

function gradeLabel(grade: string) {
  return grades.find(g => g.value === grade)?.label || grade
}

function parseTags(tags: string): string[] {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

async function fetchBank() {
  loading.value = true
  try {
    const { data } = await bankApi.getById(bankId)
    bank.value = data.data
  } catch {
    router.push('/banks')
  } finally {
    loading.value = false
  }
}

async function fetchKps() {
  kpLoading.value = true
  try {
    const params: Record<string, string | number> = { page: kpPage.value, size: 20 }
    if (kpSearch.value.trim()) params.keyword = kpSearch.value.trim()
    if (kpDifficulty.value) params.difficulty = Number(kpDifficulty.value)
    const { data } = await kpApi.list(bankId, params)
    kps.value = data.data.content
    kpTotalPages.value = data.data.totalPages
  } catch (e) {
    console.error('加载知识点失败', e)
  } finally {
    kpLoading.value = false
  }
}

function onKpSearch() {
  if (kpSearchTimer) clearTimeout(kpSearchTimer)
  kpSearchTimer = setTimeout(() => {
    kpPage.value = 0
    fetchKps()
  }, 300)
}

function changeKpPage(p: number) {
  kpPage.value = p
  fetchKps()
}

async function handleDelete() {
  if (!confirm('确定要删除这个题库吗？此操作不可恢复。')) return
  try {
    await bankApi.delete(bankId)
    router.push('/banks')
  } catch (e) {
    showToast('删除失败', 'error')
  }
}

async function handleAiGenerate() {
  aiGenerating.value = true
  aiResult.value = null
  try {
    const { data } = await aiApi.generateBatch(bankId, aiTypes.value, aiCount.value, aiGrade.value)
    aiResult.value = data.data
    // 刷新题库信息和知识点
    await fetchBank()
    await fetchKps()
  } catch (e: any) {
    showToast(e?.response?.data?.message || 'AI 出题失败', 'error')
  } finally {
    aiGenerating.value = false
  }
}

onMounted(async () => {
  await fetchBank()
  if (bank.value) fetchKps()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.bank-detail {
  max-width: 860px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  margin-bottom: 16px;
  transition: color $transition;

  &:hover { color: var(--primary); }
}

// ---- Header ----
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  animation: slideUp 0.3s ease-out;

  &__meta {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  h1 {
    font-size: 26px;
    font-weight: 800;
    color: var(--text-primary);
    margin-bottom: 6px;
  }
}

.detail-desc {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
}

.bank-badge {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;

  &.preset {
    background: rgba(99, 102, 241, 0.1);
    color: var(--primary);
  }
  &.user_upload {
    background: rgba(16, 185, 129, 0.1);
    color: var(--success);
  }
}

.bank-grade {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: calc($radius-lg - 4px);
  background: var(--bg-card);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition;
}

.btn-danger {
  color: var(--danger);
  &:hover {
    background: rgba(239, 68, 68, 0.08);
    border-color: var(--danger);
  }
}

// ---- Stats bar ----
.stats-bar {
  display: flex;
  align-items: center;
  gap: 0;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-xl;
  padding: 20px;
  margin-bottom: 28px;
  animation: slideUp 0.3s ease-out 0.05s both;
}

.stats-item {
  flex: 1;
  text-align: center;
}

.stats-number {
  display: block;
  font-size: 24px;
  font-weight: 800;
  color: var(--primary);
  margin-bottom: 4px;
}

.stats-label {
  font-size: 13px;
  color: var(--text-muted);
}

.stats-divider {
  width: 1px;
  height: 36px;
  background: var(--border);
}

// ---- KP toolbar ----
.kp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  animation: slideUp 0.3s ease-out 0.1s both;

  h2 {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
}

.kp-filters {
  display: flex;
  gap: 10px;
}

.search-box {
  position: relative;

  .search-icon {
    position: absolute;
    left: 10px;
    top: 50%;
    transform: translateY(-50%);
    color: var(--text-muted);
  }

  input {
    width: 200px;
    padding: 8px 10px 8px 32px;
    border: 1px solid var(--border);
    border-radius: calc($radius-lg - 4px);
    background: var(--bg-card);
    color: var(--text-primary);
    font-size: 13px;
    outline: none;
    transition: border-color $transition;

    &::placeholder { color: var(--text-muted); }
    &:focus { border-color: var(--primary); }
  }
}

.diff-select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: calc($radius-lg - 4px);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  outline: none;
  cursor: pointer;

  &:focus { border-color: var(--primary); }
}

// ---- KP List ----
.kp-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.kp-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-lg;
  padding: 14px 18px;
  cursor: pointer;
  transition: all $transition;
  animation: cardStagger 0.3s ease-out calc(var(--delay)) both;

  &:hover {
    border-color: var(--primary-light, rgba(99, 102, 241, 0.3));
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }

  &__main {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-left: auto;
  }

  &__detail {
    padding-top: 12px;
    margin-top: 12px;
    border-top: 1px solid var(--border);
  }
}

.kp-word {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 160px;
}

.kp-content {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.kp-phonetic {
  font-size: 13px;
  color: var(--text-muted);
}

.kp-meaning {
  font-size: 14px;
  color: var(--text-secondary);
  flex: 1;
}

.kp-diff {
  font-size: 11px;
  letter-spacing: 1px;

  &.diff-1 { color: var(--success); }
  &.diff-2 { color: #22D3EE; }
  &.diff-3 { color: var(--warning); }
  &.diff-4 { color: #F97316; }
  &.diff-5 { color: var(--danger); }
}

.kp-expand-icon {
  color: var(--text-muted);
  transition: transform $transition;
}

.kp-example {
  margin-bottom: 10px;

  &__en {
    display: block;
    font-size: 14px;
    color: var(--text-primary);
    margin-bottom: 4px;
    font-style: italic;
  }

  &__zh {
    display: block;
    font-size: 13px;
    color: var(--text-muted);
  }
}

.kp-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.kp-tag {
  padding: 2px 10px;
  background: var(--bg-sidebar, var(--bg-page));
  border-radius: $radius-full;
  font-size: 12px;
  color: var(--text-secondary);
}

// ---- Expand animation ----
.expand-enter-active, .expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.expand-enter-from, .expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  margin-top: 0;
}
.expand-enter-to, .expand-leave-from {
  opacity: 1;
  max-height: 200px;
}

// ---- Shared ----
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: var(--text-muted);

  &.small { padding: 32px 0; }
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

@keyframes spin { to { transform: rotate(360deg); } }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 8px;

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
    &:disabled { opacity: 0.4; cursor: not-allowed; }
  }
}

.page-info {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cardStagger {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

// ---- Action bar ----
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: slideUp 0.3s ease-out 0.08s both;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: calc($radius-lg - 2px);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;

  &:hover:not(:disabled) { opacity: 0.9; transform: translateY(-1px); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.btn-ai {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: white;
  border: none;
  border-radius: calc($radius-lg - 2px);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;

  &:hover { opacity: 0.9; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3); }
}

// ---- Modal ----
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-content {
  background: var(--bg-card);
  border-radius: $radius-xl;
  width: 480px;
  max-width: 92vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border);

  h3 {
    display: flex;
    align-items: center;
    gap: 8px;
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
  &:hover { color: var(--text-primary); }
}

.modal-body {
  padding: 20px 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}

.form-group {
  margin-bottom: 16px;

  label {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-secondary);
    margin-bottom: 8px;
  }
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: calc($radius-lg - 4px);
  background: var(--bg-page);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  &:focus { border-color: var(--primary); }
}

.type-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;

  input[type="checkbox"] {
    width: 16px;
    height: 16px;
    accent-color: var(--primary);
  }
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.ai-result {
  padding: 16px 24px 20px;

  &__summary {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    border-radius: calc($radius-lg - 4px);
    font-size: 14px;
    font-weight: 600;
    background: rgba(239, 68, 68, 0.08);
    color: var(--danger);

    &.success {
      background: rgba(16, 185, 129, 0.08);
      color: var(--success);
    }
  }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

// ---- Mobile ----
@media (max-width: 767px) {
  .action-bar { flex-direction: column; }

  .detail-header {
    flex-direction: column;
    gap: 12px;

    h1 { font-size: 22px; }
  }

  .stats-bar { padding: 16px; }
  .stats-number { font-size: 20px; }

  .kp-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .kp-filters {
    flex-direction: column;
  }

  .search-box input { width: 100%; }

  .kp-card__main {
    flex-wrap: wrap;
    gap: 6px;
  }

  .kp-word { min-width: auto; }
  .kp-meaning { width: 100%; }
  .kp-card__right { width: 100%; justify-content: flex-end; }
}
</style>
