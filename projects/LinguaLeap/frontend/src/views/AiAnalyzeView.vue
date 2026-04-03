<template>
  <div class="ai-analyze">
    <h1>
      <AppIcon name="sparkles" :size="24" />
      AI 文本分析
    </h1>
    <p class="ai-desc">粘贴英文文本，AI 自动提取关键词汇并导入到指定题库</p>

    <!-- 输入区 -->
    <div class="input-section">
      <div class="form-row">
        <div class="form-group">
          <label>目标题库</label>
          <select v-model="selectedBankId" class="form-input">
            <option :value="null">不保存到题库</option>
            <option v-for="b in banks" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>难度等级</label>
          <select v-model="grade" class="form-input">
            <option value="小学">小学</option>
            <option value="初中">初中</option>
            <option value="高中">高中</option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label>英文文本</label>
        <textarea
          v-model="inputText"
          class="text-input"
          placeholder="在这里粘贴英文文本，例如一段文章、一首歌词、一篇课文..."
          rows="8"
        />
      </div>

      <button class="btn-analyze" :disabled="analyzing || !inputText.trim()" @click="handleAnalyze">
        <template v-if="analyzing">
          <div class="spinner-small" />
          AI 正在分析...
        </template>
        <template v-else>
          <AppIcon name="sparkles" :size="16" />
          开始分析
        </template>
      </button>
    </div>

    <!-- 结果区 -->
    <div v-if="result" class="result-section">
      <div class="result-header">
        <h2>分析结果</h2>
        <span class="result-summary">
          共提取 {{ result.total }} 个词汇
          <template v-if="result.saved > 0">，已导入 {{ result.saved }} 个</template>
        </span>
      </div>

      <div class="kp-result-list">
        <div
          v-for="(kp, idx) in result.knowledgePoints"
          :key="idx"
          class="kp-result-card"
          :style="{ '--delay': idx * 0.04 + 's' }"
        >
          <span class="kp-result__word">{{ kp.content }}</span>
          <span class="kp-result__meaning">{{ kp.meaningZh }}</span>
          <span class="kp-result__type">{{ kp.type }}</span>
          <span class="kp-result__diff" :class="'diff-' + kp.difficulty">
            {{ '★'.repeat(kp.difficulty) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMsg" class="error-msg">
      <AppIcon name="x-circle" :size="16" />
      {{ errorMsg }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { bankApi, type QuestionBank } from '@/api/content'
import { aiApi, type KpAnalyzeResult } from '@/api/ai'
import AppIcon from '@/components/AppIcon.vue'

const inputText = ref('')
const grade = ref('初中')
const selectedBankId = ref<number | null>(null)
const analyzing = ref(false)
const result = ref<KpAnalyzeResult | null>(null)
const errorMsg = ref('')
const banks = ref<QuestionBank[]>([])

async function loadBanks() {
  try {
    const { data } = await bankApi.list({ size: 100 })
    banks.value = data.data.content
  } catch (e) {
    console.error('加载题库列表失败', e)
  }
}

async function handleAnalyze() {
  analyzing.value = true
  result.value = null
  errorMsg.value = ''
  try {
    const { data } = await aiApi.analyzeText(inputText.value, grade.value, selectedBankId.value ?? undefined)
    result.value = data.data
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || 'AI 分析失败，请稍后重试'
  } finally {
    analyzing.value = false
  }
}

onMounted(loadBanks)
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.ai-analyze {
  max-width: 800px;
}

h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
  animation: slideUp 0.3s ease-out;
}

.ai-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 28px;
  animation: slideUp 0.3s ease-out 0.05s both;
}

.input-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-xl;
  padding: 24px;
  margin-bottom: 28px;
  animation: slideUp 0.3s ease-out 0.1s both;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 4px;
}

.form-group {
  flex: 1;
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

.text-input {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: calc($radius-lg - 4px);
  background: var(--bg-page);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  font-family: inherit;
  &:focus { border-color: var(--primary); }
  &::placeholder { color: var(--text-muted); }
}

.btn-analyze {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: white;
  border: none;
  border-radius: calc($radius-lg - 2px);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;

  &:hover:not(:disabled) { opacity: 0.9; transform: translateY(-1px); box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

// ---- Result ----
.result-section {
  animation: slideUp 0.3s ease-out;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h2 {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
}

.result-summary {
  font-size: 14px;
  color: var(--success);
  font-weight: 600;
}

.kp-result-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kp-result-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-lg;
  animation: cardStagger 0.3s ease-out calc(var(--delay)) both;
}

.kp-result__word {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  min-width: 120px;
}

.kp-result__meaning {
  flex: 1;
  font-size: 14px;
  color: var(--text-secondary);
}

.kp-result__type {
  font-size: 12px;
  color: var(--text-muted);
  padding: 2px 8px;
  background: var(--bg-page);
  border-radius: $radius-full;
}

.kp-result__diff {
  font-size: 11px;
  letter-spacing: 1px;
  &.diff-1 { color: var(--success); }
  &.diff-2 { color: #22D3EE; }
  &.diff-3 { color: var(--warning); }
  &.diff-4 { color: #F97316; }
  &.diff-5 { color: var(--danger); }
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.08);
  color: var(--danger);
  border-radius: $radius-lg;
  font-size: 14px;
  font-weight: 500;
  margin-top: 16px;
}

@keyframes spin { to { transform: rotate(360deg); } }
@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes cardStagger {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 767px) {
  .form-row { flex-direction: column; gap: 0; }
  .kp-result-card { flex-wrap: wrap; gap: 8px; }
  .kp-result__word { min-width: auto; }
}
</style>
