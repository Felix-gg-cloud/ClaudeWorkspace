<template>
  <div class="upload-page">
    <div class="page-header">
      <h1>上传学习材料</h1>
      <p class="subtitle">上传你的课本、文章或词表，AI 自动提取知识点并生成个性化练习</p>
    </div>

    <!-- 步骤指示器 -->
    <div class="steps">
      <div class="step" :class="{ active: step >= 1, done: step > 1 }">
        <span class="step__num">1</span>
        <span class="step__label">选择内容</span>
      </div>
      <div class="step__line" :class="{ active: step > 1 }" />
      <div class="step" :class="{ active: step >= 2, done: step > 2 }">
        <span class="step__num">2</span>
        <span class="step__label">填写信息</span>
      </div>
      <div class="step__line" :class="{ active: step > 2 }" />
      <div class="step" :class="{ active: step >= 3 }">
        <span class="step__num">3</span>
        <span class="step__label">AI 处理</span>
      </div>
    </div>

    <!-- Step 1: 选择输入方式 -->
    <div v-if="step === 1" class="step-content">
      <div class="input-tabs">
        <button
          v-for="tab in inputTabs"
          :key="tab.value"
          class="input-tab"
          :class="{ active: inputMode === tab.value }"
          @click="inputMode = tab.value"
        >
          <AppIcon :name="tab.icon" :size="20" />
          <span>{{ tab.label }}</span>
          <small>{{ tab.desc }}</small>
        </button>
      </div>

      <div class="input-area">
        <textarea
          v-if="inputMode === 'text'"
          v-model="textContent"
          class="text-input"
          placeholder="粘贴英文课文、文章、段落...&#10;&#10;例如：&#10;The family is planning a trip to Beijing next month. They will visit the Great Wall and try local food..."
          rows="10"
        />
        <textarea
          v-else-if="inputMode === 'wordlist'"
          v-model="textContent"
          class="text-input"
          placeholder="每行一个单词或词组（可带中文释义）&#10;&#10;例如：&#10;apple 苹果&#10;beautiful 美丽的&#10;go shopping 去购物&#10;look forward to 期待&#10;&#10;💡 粘贴一段文章也会自动拆分、去重"
          rows="10"
          @paste="onWordlistPaste"
        />
        <div v-else class="file-upload-area">
          <label class="upload-dropzone" @dragover.prevent @drop.prevent="onFileDrop">
            <input type="file" accept=".pdf" class="file-input-hidden" @change="onFileSelect" />
            <template v-if="!selectedFile">
              <AppIcon name="upload" :size="40" />
              <p>点击或拖拽 PDF 文件到这里</p>
              <small>支持 .pdf 格式，最大 10MB</small>
            </template>
            <template v-else>
              <AppIcon name="file-text" :size="40" />
              <p>{{ selectedFile.name }}</p>
              <small>{{ (selectedFile.size / 1024 / 1024).toFixed(1) }} MB</small>
              <button class="btn-remove-file" @click.prevent="selectedFile = null">重新选择</button>
            </template>
          </label>
        </div>
      </div>

      <div class="step-actions">
        <button
          class="btn-next"
          :disabled="inputMode === 'pdf' ? !selectedFile : !textContent.trim()"
          @click="step = 2"
        >
          下一步 <AppIcon name="arrow-right" :size="16" />
        </button>
      </div>
    </div>

    <!-- Step 2: 填写信息 -->
    <div v-if="step === 2" class="step-content">
      <div class="form-group">
        <label>标题</label>
        <input v-model="title" class="form-input" placeholder="给这个学习集起个名字（如: 七年级 Unit 3）" />
      </div>

      <div class="form-group">
        <label>
          备注 / 学习诉求
          <span class="label-hint">AI 会根据你的需求定制练习策略</span>
        </label>
        <textarea
          v-model="userNote"
          class="form-textarea"
          placeholder="告诉 AI 你想重点练什么...&#10;&#10;例如：&#10;• 下周单元考试，重点背单词&#10;• 语法总搞混，帮我多练语法题&#10;• 阅读理解太差&#10;• 重点掌握过去时态"
          rows="4"
        />
      </div>

      <div class="step-actions">
        <button class="btn-back" @click="step = 1">
          <AppIcon name="arrow-left" :size="16" /> 上一步
        </button>
        <button
          class="btn-submit"
          :disabled="!title.trim() || submitting"
          @click="handleSubmit"
        >
          <AppIcon name="sparkles" :size="16" />
          {{ submitting ? 'AI 处理中...' : '开始 AI 分析' }}
        </button>
      </div>
    </div>

    <!-- Step 3: AI 处理中 / 完成 -->
    <div v-if="step === 3" class="step-content">
      <div v-if="submitting" class="processing">
        <div class="processing__spinner" />
        <h2>AI 正在分析你的材料...</h2>
        <p>提取知识点、自动分类、生成学习策略</p>
        <div class="processing__steps">
          <div class="pstep" :class="{ done: processStep >= 1 }">
            <AppIcon :name="processStep >= 1 ? 'check-circle' : 'loader'" :size="16" />
            提取知识点
          </div>
          <div class="pstep" :class="{ done: processStep >= 2 }">
            <AppIcon :name="processStep >= 2 ? 'check-circle' : 'loader'" :size="16" />
            自动分类
          </div>
          <div class="pstep" :class="{ done: processStep >= 3 }">
            <AppIcon :name="processStep >= 3 ? 'check-circle' : 'loader'" :size="16" />
            生成学习策略
          </div>
        </div>
      </div>

      <div v-else-if="createdSet" class="complete">
        <div v-if="createdSet.status === 'ready'" class="complete--success">
          <AppIcon name="check-circle" :size="48" />
          <h2>学习集创建成功！</h2>
          <p>AI 从你的材料中提取了 <strong>{{ createdSet.itemCount }}</strong> 个知识点</p>
          <p v-if="createdSet.aiSummary" class="ai-summary">{{ createdSet.aiSummary }}</p>
          <div class="complete__actions">
            <button class="btn-view" @click="router.push(`/study-sets/${createdSet.id}`)">
              <AppIcon name="eye" :size="16" /> 查看学习集
            </button>
            <button class="btn-another" @click="resetForm">
              <AppIcon name="plus" :size="16" /> 继续上传
            </button>
          </div>
        </div>
        <div v-else class="complete--failed">
          <AppIcon name="alert-circle" :size="48" />
          <h2>处理失败</h2>
          <p>{{ createdSet.aiSummary || 'AI 未能从文本中提取到有效内容' }}</p>
          <button class="btn-back" @click="step = 1">重新输入</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { studySetApi, type StudySet } from '@/api/studySet'
import { useUserStore } from '@/stores/user'
import AppIcon from '@/components/AppIcon.vue'
import { showToast } from '@/composables/useToast'

const router = useRouter()
const userStore = useUserStore()

const step = ref(1)
const inputMode = ref<'text' | 'wordlist' | 'pdf'>('text')
const textContent = ref('')
const title = ref('')
const userNote = ref('')
const grade = ref(userStore.user?.grade || 'junior')
const submitting = ref(false)
const processStep = ref(0)
const createdSet = ref<StudySet | null>(null)
const selectedFile = ref<File | null>(null)

const inputTabs = [
  { value: 'text' as const, icon: 'file-text', label: '粘贴文本', desc: '课文/文章/段落' },
  { value: 'wordlist' as const, icon: 'list', label: '词表输入', desc: '单词/词组列表' },
  { value: 'pdf' as const, icon: 'upload', label: 'PDF 上传', desc: '上传 PDF 文件' },
]

async function handleSubmit() {
  if (inputMode.value === 'pdf') {
    if (!selectedFile.value || !title.value.trim()) return
  } else {
    if (!textContent.value.trim() || !title.value.trim()) return
  }

  submitting.value = true
  step.value = 3
  processStep.value = 0

  // 模拟进度
  const timer = setInterval(() => {
    if (processStep.value < 2) processStep.value++
  }, 3000)

  try {
    let res
    if (inputMode.value === 'pdf' && selectedFile.value) {
      res = await studySetApi.upload(
        selectedFile.value,
        title.value,
        userNote.value || undefined,
        grade.value,
      )
    } else {
      res = await studySetApi.create({
        title: title.value,
        text: textContent.value,
        sourceType: inputMode.value === 'wordlist' ? 'wordlist' : 'text',
        userNote: userNote.value || undefined,
        grade: grade.value,
      })
    }
    processStep.value = 3
    createdSet.value = res.data.data
  } catch (e: any) {
    createdSet.value = {
      status: 'failed',
      aiSummary: e.response?.data?.message || '网络错误',
    } as StudySet
  } finally {
    clearInterval(timer)
    submitting.value = false
  }
}

function onFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    if (file.size > 10 * 1024 * 1024) {
      showToast('文件大小不能超过 10MB', 'error')
      return
    }
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      showToast('仅支持 PDF 文件', 'error')
      return
    }
    selectedFile.value = file
  }
}

function onFileDrop(e: DragEvent) {
  const file = e.dataTransfer?.files[0]
  if (file) {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      showToast('仅支持 PDF 文件', 'error')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      showToast('文件大小不能超过 10MB', 'error')
      return
    }
    selectedFile.value = file
  }
}

function resetForm() {
  step.value = 1
  textContent.value = ''
  title.value = ''
  userNote.value = ''
  createdSet.value = null
  processStep.value = 0
  selectedFile.value = null
}

function onWordlistPaste(e: ClipboardEvent) {
  const raw = e.clipboardData?.getData('text/plain') || ''
  // 判断是否像一段文章（包含多个空格分隔的连续单词，而非已按行整理好的词表）
  const lines = raw.split(/\n/).filter(l => l.trim())
  const avgWordsPerLine = lines.reduce((s, l) => s + l.trim().split(/\s+/).length, 0) / Math.max(lines.length, 1)
  if (avgWordsPerLine <= 3) return // 已经是词表格式，不干预

  e.preventDefault()
  // 提取英文单词，保留带连字符的词
  const words = raw.match(/[a-zA-Z]+(?:[-'][a-zA-Z]+)*/g) || []
  // 过滤停用词
  const stopWords = new Set([
    'a', 'an', 'the', 'is', 'are', 'was', 'were', 'am', 'be', 'been', 'being',
    'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could', 'should',
    'may', 'might', 'shall', 'can', 'must', 'need', 'dare',
    'i', 'you', 'he', 'she', 'it', 'we', 'they', 'me', 'him', 'her', 'us', 'them',
    'my', 'your', 'his', 'its', 'our', 'their', 'mine', 'yours', 'hers', 'ours', 'theirs',
    'this', 'that', 'these', 'those',
    'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by', 'from', 'up', 'about',
    'into', 'through', 'during', 'before', 'after', 'above', 'below', 'between',
    'and', 'but', 'or', 'nor', 'not', 'so', 'yet', 'both', 'either', 'neither',
    'if', 'then', 'than', 'when', 'while', 'as', 'because', 'since', 'until',
    'what', 'which', 'who', 'whom', 'where', 'how', 'why',
    'there', 'here', 'all', 'each', 'every', 'any', 'some', 'no', 'more', 'most',
    'other', 'another', 'such', 'only', 'own', 'same', 'also', 'very', 'just',
    'too', 'now', 'still', 'already', 'even', 'much', 'many',
  ])
  // 去重（不区分大小写），过滤停用词和单字母
  const seen = new Set<string>()
  const unique: string[] = []
  for (const w of words) {
    const lower = w.toLowerCase()
    if (lower.length <= 1) continue
    if (stopWords.has(lower)) continue
    if (!seen.has(lower)) {
      seen.add(lower)
      unique.push(lower)
    }
  }
  // 合并到已有内容
  const existing = textContent.value
    .split(/\n/)
    .map(l => l.trim())
    .filter(Boolean)
  for (const line of existing) {
    const key = line.split(/\s+/)[0]?.toLowerCase()
    if (key) seen.add(key)
  }
  const merged = [...existing]
  for (const w of unique) {
    if (!existing.some(l => l.split(/\s+/)[0]?.toLowerCase() === w)) {
      merged.push(w)
    }
  }
  textContent.value = merged.join('\n')
}
</script>

<style lang="scss" scoped>
.upload-page {
  max-width: 720px;
  margin: 0 auto;
  animation: fadeUp 0.4s ease-out;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  margin-bottom: 28px;
  h1 { font-size: 24px; font-weight: 800; color: var(--text-primary, #0F172A); margin-bottom: 6px; }
  .subtitle { font-size: 14px; color: var(--text-muted, #94A3B8); }
}

// Steps indicator
.steps {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 32px;
}
.step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted, #94A3B8);
  transition: all 0.3s;

  &.active { color: var(--text-primary, #0F172A); }
  &.done .step__num {
    background: #10B981;
    border-color: #10B981;
    color: #fff;
  }

  &__num {
    width: 30px; height: 30px;
    border-radius: 50%;
    border: 2px solid var(--border, #E2E8F0);
    display: flex; align-items: center; justify-content: center;
    font-size: 13px; font-weight: 700;
  }
  &.active .step__num {
    border-color: #6366F1;
    background: rgba(99, 102, 241, 0.1);
    color: #6366F1;
  }
  &__label { font-size: 13px; font-weight: 600; }
  &__line {
    flex: 1;
    height: 2px;
    background: var(--border, #E2E8F0);
    margin: 0 12px;
    transition: background 0.3s;
    &.active { background: #10B981; }
  }
}

.step-content { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }

// Input tabs
.input-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.input-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 12px;
  border-radius: 14px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-muted, #94A3B8);
  cursor: pointer;
  transition: all 0.2s;

  &:hover { color: var(--text-secondary, #64748B); border-color: rgba(99, 102, 241, 0.2); }
  &.active {
    color: #6366F1;
    border-color: rgba(99, 102, 241, 0.4);
    background: rgba(99, 102, 241, 0.06);
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
  }
  span { font-size: 14px; font-weight: 600; }
  small { font-size: 11px; color: var(--text-muted, #94A3B8); }
}

.text-input {
  width: 100%;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-primary, #0F172A);
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s;
  &:focus { outline: none; border-color: #6366F1; box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08); }
  &::placeholder { color: var(--text-muted, #CBD5E1); }
}

.file-upload-area {
  .upload-dropzone {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 48px 20px;
    border: 2px dashed var(--border, #E2E8F0);
    border-radius: 14px;
    color: var(--text-muted, #94A3B8);
    cursor: pointer;
    transition: border-color 0.2s, background 0.2s;
    &:hover { border-color: var(--primary, #6366F1); background: rgba(99, 102, 241, 0.04); }
    p { margin: 12px 0 4px; font-size: 15px; color: var(--text-primary, #0F172A); }
    small { font-size: 13px; }
  }
  .file-input-hidden { display: none; }
  .btn-remove-file {
    margin-top: 12px;
    padding: 4px 16px;
    font-size: 13px;
    color: var(--primary, #6366F1);
    background: none;
    border: 1px solid var(--primary, #6366F1);
    border-radius: 6px;
    cursor: pointer;
    &:hover { background: rgba(99, 102, 241, 0.08); }
  }
}

// Form
.form-group {
  margin-bottom: 20px;
  label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary, #0F172A);
    margin-bottom: 8px;
  }
  .label-hint {
    font-weight: 400;
    color: var(--text-muted, #94A3B8);
    font-size: 12px;
    margin-left: 8px;
  }
}
.form-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-primary, #0F172A);
  font-size: 15px;
  transition: border-color 0.2s;
  &:focus { outline: none; border-color: #6366F1; box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08); }
}
.form-textarea {
  @extend .text-input;
  font-size: 14px;
}
.grade-select {
  display: flex; gap: 8px;
}
.grade-btn {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-secondary, #64748B);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  &.active {
    background: rgba(99, 102, 241, 0.1);
    border-color: rgba(99, 102, 241, 0.4);
    color: #6366F1;
    font-weight: 600;
  }
}

// Action buttons
.step-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
}
.btn-next, .btn-submit {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 12px 24px;
  border-radius: 12px;
  border: none;
  font-size: 15px; font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto;
  &:disabled { opacity: 0.35; cursor: not-allowed; }
}
.btn-next {
  background: rgba(99, 102, 241, 0.1);
  color: #6366F1;
  &:hover:not(:disabled) { background: rgba(99, 102, 241, 0.18); transform: translateY(-1px); }
}
.btn-submit {
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: #fff;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4); }
}
.btn-back {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 12px 20px;
  border-radius: 12px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-secondary, #64748B);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: var(--text-primary, #0F172A); border-color: rgba(99, 102, 241, 0.3); }
}

// Processing
.processing {
  text-align: center;
  padding: 48px 20px;

  &__spinner {
    width: 48px; height: 48px;
    border: 3px solid var(--border, #E2E8F0);
    border-top-color: #6366F1;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin: 0 auto 20px;
  }
  h2 { font-size: 20px; font-weight: 700; color: var(--text-primary, #0F172A); margin-bottom: 8px; }
  p { color: var(--text-muted, #94A3B8); margin-bottom: 24px; }

  &__steps {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-width: 240px;
    margin: 0 auto;
    text-align: left;
  }
}
.pstep {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px;
  color: var(--text-muted, #94A3B8);
  transition: all 0.3s;
  &.done { color: #10B981; font-weight: 600; }
}

@keyframes spin { to { transform: rotate(360deg); } }

// Complete
.complete {
  text-align: center;
  padding: 48px 20px;

  h2 { font-size: 22px; font-weight: 700; color: var(--text-primary, #0F172A); margin: 16px 0 8px; }
  p { color: var(--text-secondary, #64748B); margin-bottom: 8px; }
  strong { color: #6366F1; }

  .ai-summary {
    margin: 16px auto;
    max-width: 400px;
    padding: 12px 16px;
    border-radius: 12px;
    background: rgba(99, 102, 241, 0.06);
    border: 1px solid rgba(99, 102, 241, 0.12);
    font-size: 14px;
    color: var(--text-secondary, #64748B);
  }

  &__actions {
    display: flex; gap: 12px; justify-content: center; margin-top: 24px;
  }

  &--failed { color: #EF4444; }
}

.btn-view {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 12px 24px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: #fff;
  font-size: 15px; font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  transition: all 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4); }
}
.btn-another {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 12px 20px;
  border-radius: 12px;
  border: 1px solid var(--border, #E2E8F0);
  background: var(--bg-card, #fff);
  color: var(--text-secondary, #64748B);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: var(--text-primary, #0F172A); border-color: rgba(99, 102, 241, 0.3); }
}

@media (max-width: 640px) {
  .input-tabs { grid-template-columns: 1fr; }
  .grade-select { flex-direction: column; }
}
</style>
