<template>
  <div class="practice">
    <!-- 选择题库、题型、年级 -->
    <div v-if="phase === 'setup'" class="setup-card">
      <h1>{{ unitMode ? unitName : studySetMode ? studySetName : '开始练习' }}</h1>
      <p class="setup-desc">{{ unitMode ? '基于该单元知识点出题' : studySetMode ? '基于学习集内容出题' : '选择题库、年级和练习方式' }}</p>

      <div v-if="!unitMode && !studySetMode" class="form-group">
        <label>选择题库</label>
        <div v-if="banksLoading" class="loading-inline"><div class="spinner-sm" /></div>
        <div v-else class="bank-select">
          <button
            v-for="b in banks"
            :key="b.id"
            class="bank-option"
            :class="{ active: selectedBankId === b.id }"
            @click="selectedBankId = b.id"
          >
            <span class="bank-option__name">{{ b.name }}</span>
            <span class="bank-option__count">{{ b.kpCount }} 词</span>
          </button>
        </div>
      </div>

      <div v-if="!unitMode && !studySetMode" class="form-group">
        <label>年级</label>
        <div class="grade-select">
          <button
            v-for="g in grades"
            :key="g.value"
            class="grade-option"
            :class="{ active: selectedGrade === g.value }"
            @click="selectedGrade = g.value"
          >{{ g.label }}</button>
        </div>
      </div>

      <div class="form-group">
        <label>题型 <span class="multi-hint">（可多选）</span></label>
        <div class="type-select">
          <button
            v-for="t in questionTypes"
            :key="t.value"
            class="type-option"
            :class="{ active: selectedTypes.includes(t.value) }"
            @click="toggleType(t.value)"
          >
            <AppIcon :name="t.icon" :size="18" />
            <span>{{ t.label }}</span>
            <span class="type-hint">{{ typeHint(t.value) }}</span>
          </button>
        </div>
      </div>

      <div class="form-group">
        <label>题目数量</label>
        <div class="count-select">
          <button
            v-for="c in [5, 10, 15, 20]"
            :key="c"
            class="count-option"
            :class="{ active: selectedCount === c }"
            @click="selectedCount = c"
          >{{ c }} 题</button>
        </div>
      </div>

      <button
        class="btn-start"
        :disabled="(!unitMode && !studySetMode && !selectedBankId) || selectedTypes.length === 0 || starting"
        @click="startPractice"
      >
        {{ starting ? '准备中...' : '开始练习' }}
      </button>

      <div v-if="errorMsg" class="error-msg">
        <AppIcon name="alert-circle" :size="16" />
        <span>{{ errorMsg }}</span>
      </div>
    </div>

    <!-- 答题中 -->
    <div v-else-if="phase === 'answering'" class="answering">
      <!-- 进度条 -->
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }" />
      </div>
      <div class="progress-text">
        <span>{{ progress.current }} / {{ progress.total }}</span>
        <span class="correct-badge">
          <AppIcon name="check-circle" :size="14" />
          {{ progress.correctCount }}
        </span>
      </div>

      <!-- 题目卡片 -->
      <div class="question-card" :key="question?.questionId">
        <div class="question-header">
          <span class="q-type-badge">{{ typeLabel(question?.type) }}</span>
          <span class="q-grade-badge" v-if="question?.grade">{{ levelName || gradeLabel(question.grade) }}</span>
          <span class="q-diff">{{ '★'.repeat(question?.difficulty || 1) }}</span>
        </div>

        <!-- 题干 + TTS -->
        <div class="question-stem-row">
          <div class="question-stem">{{ question?.stem }}</div>
          <SpeakButton v-if="shouldShowTts" :text="ttsText" />
        </div>

        <!-- ======== 选择题（en2zh_choice / zh2en_choice(非高中) / fill_blank(非高中)） ======== -->
        <div v-if="showOptions" class="options-grid">
          <button
            v-for="(opt, idx) in question?.options"
            :key="idx"
            class="option-btn"
            :class="optionClass(opt)"
            :disabled="!!feedback"
            @click="submitChoice(opt)"
          >
            <span class="option-letter">{{ 'ABCD'[idx] }}</span>
            <span class="option-text">{{ opt }}</span>
            <AppIcon v-if="feedback && opt === feedback.correctAnswer" name="check-circle" :size="18" class="option-icon correct" />
            <AppIcon v-else-if="feedback && !feedback.correct && opt === selectedAnswer" name="x-circle" :size="18" class="option-icon wrong" />
          </button>
        </div>

        <!-- ======== 排序题（translate + primary） ======== -->
        <div v-else-if="isWordOrder" class="word-order-area">
          <div class="word-order-result">
            <span
              v-for="(w, idx) in orderedWords"
              :key="'o' + idx"
              class="word-chip ordered"
              @click="removeWord(idx)"
            >{{ w }}</span>
            <span v-if="orderedWords.length === 0" class="word-order-placeholder">点击下方单词组成句子</span>
          </div>
          <div class="word-order-pool">
            <button
              v-for="(w, idx) in availableWords"
              :key="'p' + idx"
              class="word-chip pool"
              :class="{ used: usedIndices.has(Number(idx)) }"
              :disabled="!!feedback || usedIndices.has(Number(idx))"
              @click="addWord(Number(idx))"
            >
              {{ w }}
              <span v-if="wordMeanings[w]" class="word-meaning-tip">{{ wordMeanings[w] }}</span>
            </button>
          </div>
          <button
            class="btn-submit-answer"
            :disabled="orderedWords.length === 0 || !!feedback"
            @click="submitWordOrder"
          >提交</button>
        </div>

        <!-- ======== 半填空翻译题（translate + junior） ======== -->
        <div v-else-if="isTemplateFill" class="template-fill-area">
          <div class="template-sentence">
            <template v-for="(part, idx) in templateParts" :key="idx">
              <span v-if="part !== '______'" class="tpl-text">{{ part }}</span>
              <input
                v-else
                class="tpl-blank"
                v-model="blankAnswers[templateBlankIndex(Number(idx))]"
                :placeholder="'填写...'"
                :disabled="!!feedback"
                @keyup.enter="submitTemplateFill"
              />
            </template>
          </div>
          <button
            class="btn-submit-answer"
            :disabled="!allBlanksFilled || !!feedback"
            @click="submitTemplateFill"
          >提交</button>
        </div>

        <!-- ======== 输入题（zh2en senior / fill_blank senior / translate senior） ======== -->
        <div v-else class="input-area">
          <input
            v-model="inputAnswer"
            type="text"
            :placeholder="inputPlaceholder"
            :disabled="!!feedback"
            @keyup.enter="submitInput"
          />
          <button
            class="btn-submit-answer"
            :disabled="!inputAnswer.trim() || !!feedback"
            @click="submitInput"
          >
            提交
          </button>
        </div>

        <!-- ======== AI 评判中（高中翻译） ======== -->
        <div v-if="judging" class="judging-indicator">
          <div class="spinner-sm" />
          <span>AI 评判中...</span>
        </div>

        <!-- ======== 反馈区域 ======== -->
        <Transition name="fade">
          <div v-if="feedback" class="feedback" :class="{ correct: feedback.correct, wrong: !feedback.correct }">
            <div class="feedback-header">
              <AppIcon :name="feedback.correct ? 'check-circle' : 'x-circle'" :size="20" />
              <span>{{ feedback.correct ? '回答正确！' : '回答错误' }}</span>
            </div>

            <!-- 正确答案 -->
            <div v-if="!feedback.correct" class="correct-answer-row">
              <span class="ca-label">正确答案：</span>
              <span class="ca-text">{{ feedback.correctAnswer }}</span>
              <SpeakButton v-if="isEnglishAnswer" :text="feedback.correctAnswer" :size="16" />
            </div>

            <!-- AI 翻译评判详情 -->
            <div v-if="judgeResult" class="judge-detail">
              <div class="judge-score">得分：{{ judgeResult.score }}/100</div>
              <p class="judge-feedback">{{ judgeResult.feedback }}</p>
              <ul v-if="judgeResult.corrections.length" class="judge-corrections">
                <li v-for="(c, i) in judgeResult.corrections" :key="i">{{ c }}</li>
              </ul>
            </div>

            <!-- 解析 -->
            <p class="feedback-explain">{{ feedback.explanation }}</p>

            <!-- 知识点解析面板 -->
            <div v-if="feedback.knowledgePoints" class="kp-panel">
              <div class="kp-title"><AppIcon name="sparkles" :size="14" /> 知识点解析</div>
              <p class="kp-content">{{ feedback.knowledgePoints }}</p>
            </div>

            <!-- 例句 -->
            <div v-if="feedback.exampleSentence" class="example-panel">
              <div class="example-en">
                {{ feedback.exampleSentence }}
                <SpeakButton :text="feedback.exampleSentence" :size="14" />
              </div>
              <div v-if="feedback.exampleZh" class="example-zh">{{ feedback.exampleZh }}</div>
            </div>

            <!-- 重新输入（错误的中译英/翻译题） -->
            <div v-if="needRetype && !retypeCorrect" class="retype-area">
              <p class="retype-hint">请手动输入正确答案以加深记忆：</p>
              <div class="retype-input-row">
                <input
                  v-model="retypeInput"
                  type="text"
                  :placeholder="feedback.correctAnswer"
                  @keyup.enter="checkRetype"
                />
                <button class="btn-retype" @click="checkRetype">确认</button>
              </div>
              <p v-if="retypeWrong" class="retype-wrong">输入不正确，请重试</p>
            </div>
            <div v-if="retypeCorrect" class="retype-ok">
              <AppIcon name="check-circle" :size="16" />
              <span>记忆巩固完成！</span>
            </div>

            <button class="btn-next" :disabled="needRetype && !retypeCorrect" @click="nextQuestion">
              {{ isLastQuestion ? '查看结果' : '下一题' }}
              <AppIcon name="chevron-right" :size="16" />
            </button>
          </div>
        </Transition>
      </div>
    </div>

    <!-- 结果页 -->
    <div v-else-if="phase === 'result'" class="result-page">
      <div class="result-card">
        <div class="result-icon" :class="resultGrade">
          <AppIcon :name="resultGrade === 'great' ? 'rocket' : resultGrade === 'good' ? 'zap' : 'refresh-cw'" :size="48" />
        </div>
        <h1 class="result-title">练习完成！</h1>
        <div class="result-score">
          <span class="score-number">{{ practiceResult?.correctCount }}</span>
          <span class="score-separator">/</span>
          <span class="score-total">{{ practiceResult?.totalCount }}</span>
        </div>
        <div class="result-accuracy">正确率 {{ Math.round((practiceResult?.accuracy || 0) * 100) }}%</div>
        <div class="result-duration" v-if="practiceResult?.duration">
          用时 {{ formatDuration(practiceResult.duration) }}
        </div>
        <div class="result-stats">
          <div class="result-stat correct">
            <AppIcon name="check-circle" :size="16" />
            <span>正确 {{ practiceResult?.correctCount }} 题</span>
          </div>
          <div class="result-stat wrong">
            <AppIcon name="x-circle" :size="16" />
            <span>错误 {{ (practiceResult?.totalCount || 0) - (practiceResult?.correctCount || 0) }} 题</span>
          </div>
        </div>
        <!-- AI 分析 -->
        <div class="ai-analysis-card" v-if="analysisLoading || aiAnalysis">
          <div class="analysis-header">
            <AppIcon name="sparkles" :size="18" />
            <span>Lily 老师点评</span>
          </div>
          <div v-if="analysisLoading" class="analysis-loading">
            <span class="spinner-sm"></span> 正在分析...
          </div>
          <div v-else class="analysis-content">{{ aiAnalysis }}</div>
        </div>
        <div class="result-actions">
          <button class="btn-retry" @click="resetPractice">
            <AppIcon name="refresh-cw" :size="16" />
            再练一次
          </button>
          <button class="btn-back" @click="router.push(studySetMode ? `/study-sets/${studySetId}` : unitMode ? '/levels' : '/')">
            <AppIcon :name="studySetMode ? 'book-open' : unitMode ? 'layers' : 'home'" :size="16" />
            {{ studySetMode ? '返回学习集' : unitMode ? '返回知识库' : '返回首页' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { practiceApi, type PracticeQuestion, type AnswerResult, type PracticeResult, type TranslateJudgeResult } from '@/api/practice'
import { bankApi, type QuestionBank } from '@/api/content'
import { statsApi } from '@/api/stats'
import { orchestratorApi } from '@/api/teacher'
import { levelApi } from '@/api/level'
import AppIcon from '@/components/AppIcon.vue'
import { showToast } from '@/composables/useToast'
import SpeakButton from '@/components/SpeakButton.vue'

const router = useRouter()
const route = useRoute()

// 单元练习模式
const unitMode = ref(false)
const unitId = ref<number | null>(null)
const unitName = ref('')
const levelName = ref('')

// 学习集练习模式
const studySetMode = ref(false)
const studySetId = ref<number | null>(null)
const studySetName = ref('')

// 阶段
const phase = ref<'setup' | 'answering' | 'result'>('setup')

// Setup
const banks = ref<QuestionBank[]>([])
const banksLoading = ref(false)
const selectedBankId = ref<number | null>(null)
const selectedTypes = ref<string[]>(['en2zh_choice'])
const selectedGrade = ref('junior')
const selectedCount = ref(10)
const starting = ref(false)

const grades = [
  { value: 'primary', label: '小学' },
  { value: 'junior', label: '初中' },
  { value: 'senior', label: '高中' },
]

const questionTypes = [
  { value: 'en2zh_choice', label: '英译中', icon: 'book-open' },
  { value: 'zh2en_choice', label: '中译英', icon: 'pen-line' },
  { value: 'fill_blank', label: '填空', icon: 'sparkles' },
  { value: 'translate', label: '翻译', icon: 'graduation-cap' },
]

function typeHint(type: string) {
  const g = selectedGrade.value
  const hints: Record<string, Record<string, string>> = {
    en2zh_choice: { primary: '选释义', junior: '选释义', senior: '语境选义' },
    zh2en_choice: { primary: '选单词', junior: '选单词', senior: '拼写' },
    fill_blank: { primary: '选词', junior: '选形态', senior: '开放填空' },
    translate: { primary: '排序', junior: '半填空', senior: '完整翻译' },
  }
  return hints[type]?.[g] || ''
}

// Answering
const sessionId = ref(0)
const question = ref<PracticeQuestion | null>(null)
const feedback = ref<AnswerResult | null>(null)
const wrongAnswers = ref<{ stem: string; correctAnswer: string; userAnswer: string }[]>([])

// AI 分析
const aiAnalysis = ref('')
const analysisLoading = ref(false)
const selectedAnswer = ref('')
const inputAnswer = ref('')
const progress = ref({ current: 0, total: 0, correctCount: 0 })

// Word order (primary translate)
const orderedWords = ref<string[]>([])
const usedIndices = ref<Set<number>>(new Set())
const availableWords = computed(() => question.value?.extraData?.shuffledWords || [])
const wordMeanings = computed(() => question.value?.extraData?.wordMeanings || {})

// Template fill (junior translate)
const blankAnswers = ref<string[]>([])
const templateParts = computed(() => {
  const tpl = question.value?.extraData?.template || ''
  return tpl.split(/(______)/)
})
const allBlanksFilled = computed(() => blankAnswers.value.every(a => a.trim()))

function templateBlankIndex(partIdx: number): number {
  let count = 0
  const parts = templateParts.value
  for (let i = 0; i < partIdx; i++) {
    if (parts[i] === '______') count++
  }
  return count
}

// AI judge (senior translate)
const judging = ref(false)
const judgeResult = ref<TranslateJudgeResult | null>(null)

// Retype
const retypeInput = ref('')
const retypeCorrect = ref(false)
const retypeWrong = ref(false)

// Result
const practiceResult = ref<PracticeResult | null>(null)

// Computed
const progressPercent = computed(() => {
  if (!progress.value.total) return 0
  return (progress.value.current / progress.value.total) * 100
})

const isLastQuestion = computed(() => {
  return feedback.value && feedback.value.sessionProgress.current >= feedback.value.sessionProgress.total
})

const resultGrade = computed(() => {
  const acc = practiceResult.value?.accuracy || 0
  if (acc >= 0.8) return 'great'
  if (acc >= 0.5) return 'good'
  return 'retry'
})

const showOptions = computed(() => {
  if (!question.value) return false
  const { type, options, grade } = question.value
  if (type === 'en2zh_choice') return !!options
  if (type === 'zh2en_choice') return grade !== 'senior' && !!options
  if (type === 'fill_blank') return grade !== 'senior' && !!options
  return false
})

const isWordOrder = computed(() => {
  if (!question.value) return false
  return question.value.type === 'translate' && question.value.grade === 'primary'
    && question.value.extraData?.shuffledWords
})

const isTemplateFill = computed(() => {
  if (!question.value) return false
  return question.value.type === 'translate' && question.value.grade === 'junior'
    && question.value.extraData?.template
})

const shouldShowTts = computed(() => {
  if (!question.value) return false
  const { type } = question.value
  return type === 'en2zh_choice' || type === 'fill_blank'
})

const ttsText = computed(() => {
  if (!question.value) return ''
  // For en2zh, speak the English word from stem
  // For fill_blank, speak the example sentence
  return question.value.exampleSentence || question.value.stem || ''
})

const isEnglishAnswer = computed(() => {
  if (!feedback.value) return false
  const type = question.value?.type
  return type === 'zh2en_choice' || type === 'fill_blank' || type === 'translate'
})

const needRetype = computed(() => {
  if (!feedback.value || feedback.value.correct) return false
  const type = question.value?.type
  return type === 'zh2en_choice' || type === 'translate'
})

const inputPlaceholder = computed(() => {
  if (!question.value) return ''
  const { type } = question.value
  if (type === 'zh2en_choice') return '请拼写英文单词...'
  if (type === 'fill_blank') return '请输入正确形态...'
  if (type === 'translate') return '请输入英文翻译...'
  return '请输入答案...'
})

// Methods
function typeLabel(type?: string) {
  return questionTypes.find(t => t.value === type)?.label || type || ''
}

function gradeLabel(g: string) {
  return grades.find(v => v.value === g)?.label || g
}

function formatDuration(seconds: number) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

async function loadBanks() {
  banksLoading.value = true
  try {
    const { data } = await bankApi.list({ page: 0, size: 50 })
    banks.value = data.data.content
    const qBankId = route.query.bankId
    if (qBankId) selectedBankId.value = Number(qBankId)
  } catch { /* ignore */ } finally {
    banksLoading.value = false
  }
}

// Error message
const errorMsg = ref('')

function toggleType(type: string) {
  const idx = selectedTypes.value.indexOf(type)
  if (idx >= 0) {
    selectedTypes.value.splice(idx, 1)
  } else {
    selectedTypes.value.push(type)
  }
}

async function startPractice() {
  if (selectedTypes.value.length === 0) return
  starting.value = true
  errorMsg.value = ''
  try {
    let res
    if (studySetMode.value && studySetId.value) {
      res = await practiceApi.startByStudySet({
        studySetId: studySetId.value,
        questionType: selectedTypes.value.join(','),
        grade: selectedGrade.value,
        count: selectedCount.value,
      })
    } else if (unitMode.value && unitId.value) {
      res = await practiceApi.startByUnit({
        unitId: unitId.value,
        questionType: selectedTypes.value.join(','),
        grade: selectedGrade.value,
        count: selectedCount.value,
      })
    } else {
      if (!selectedBankId.value) return
      res = await practiceApi.start({
        bankId: selectedBankId.value,
        questionType: selectedTypes.value.join(','),
        grade: selectedGrade.value,
        count: selectedCount.value,
      })
    }
    sessionId.value = res.data.data.sessionId
    progress.value = { current: 0, total: res.data.data.totalCount, correctCount: 0 }
    phase.value = 'answering'
    await loadNextQuestion()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '开始练习失败'
    errorMsg.value = msg
    console.error('开始练习失败', msg)
  } finally {
    starting.value = false
  }
}

async function loadNextQuestion() {
  feedback.value = null
  selectedAnswer.value = ''
  inputAnswer.value = ''
  orderedWords.value = []
  usedIndices.value = new Set()
  blankAnswers.value = []
  retypeInput.value = ''
  retypeCorrect.value = false
  retypeWrong.value = false
  judgeResult.value = null
  try {
    const { data } = await practiceApi.next(sessionId.value)
    question.value = data.data
    progress.value = data.data.progress
    // Init blank answers for template fill
    if (data.data.extraData?.blanks) {
      blankAnswers.value = data.data.extraData.blanks.map(() => '')
    }
  } catch {
    await finishPractice()
  }
}

function addWord(idx: number) {
  if (usedIndices.value.has(idx)) return
  orderedWords.value.push(availableWords.value[idx])
  usedIndices.value = new Set([...usedIndices.value, idx])
}

function removeWord(idx: number) {
  const word = orderedWords.value[idx]
  orderedWords.value.splice(idx, 1)
  // Find original index
  const origIdx = availableWords.value.findIndex(
    (w: string, i: number) => w === word && usedIndices.value.has(i)
  )
  if (origIdx >= 0) {
    const newSet = new Set(usedIndices.value)
    newSet.delete(origIdx)
    usedIndices.value = newSet
  }
}

async function submitWordOrder() {
  const answer = orderedWords.value.join(' ')
  selectedAnswer.value = answer
  await doAnswer(answer)
}

async function submitTemplateFill() {
  const answer = blankAnswers.value.join(',')
  selectedAnswer.value = answer
  await doAnswer(answer)
}

async function submitChoice(option: string) {
  if (feedback.value || !question.value) return
  selectedAnswer.value = option
  await doAnswer(option)
}

async function submitInput() {
  if (feedback.value || !inputAnswer.value.trim() || !question.value) return
  selectedAnswer.value = inputAnswer.value.trim()
  await doAnswer(inputAnswer.value.trim())
}

async function doAnswer(answer: string) {
  try {
    const { data } = await practiceApi.answer(sessionId.value, question.value!.questionId, answer)
    feedback.value = data.data
    progress.value = {
      current: data.data.sessionProgress.current,
      total: data.data.sessionProgress.total,
      correctCount: data.data.sessionProgress.correctCount,
    }

    // 记录错题
    if (!data.data.correct && question.value) {
      wrongAnswers.value.push({
        stem: question.value.stem,
        correctAnswer: data.data.correctAnswer,
        userAnswer: answer,
      })
    }

    // 高中翻译题：调用 AI 评判
    if (question.value?.type === 'translate' && question.value.grade === 'senior') {
      judging.value = true
      try {
        const { data: jd } = await practiceApi.judgeTranslate(
          question.value.stem,
          data.data.correctAnswer,
          answer,
          'senior'
        )
        judgeResult.value = jd.data
        // AI 判定覆盖简单比较的结果
        if (jd.data.correct !== data.data.correct) {
          feedback.value = { ...data.data, correct: jd.data.correct }
        }
      } catch (e) {
        console.warn('AI 评判失败', e)
      } finally {
        judging.value = false
      }
    }
  } catch (e) {
    showToast('提交答案失败，请重试', 'error')
  }
}

function checkRetype() {
  if (!feedback.value) return
  retypeWrong.value = false
  if (retypeInput.value.trim().toLowerCase() === feedback.value.correctAnswer.trim().toLowerCase()) {
    retypeCorrect.value = true
  } else {
    retypeWrong.value = true
  }
}

async function nextQuestion() {
  if (isLastQuestion.value) {
    await finishPractice()
  } else {
    await loadNextQuestion()
  }
}

async function finishPractice() {
  try {
    const { data } = await practiceApi.finish(sessionId.value)
    practiceResult.value = data.data
    phase.value = 'result'

    const totalCount = data.data.totalCount || 0
    const correctCount = data.data.correctCount || 0
    const wrongCount = totalCount - correctCount

    // 录入每日统计
    const durationSec = data.data.duration || 0
    const studyMinutes = Math.max(1, Math.round(durationSec / 60))
    statsApi.record({ correctCount, wrongCount, wordsLearned: 0, studyMinutes }).catch(() => {})

    // Phase 5a: 上报编排引擎 — 逐题上报汇总
    if (totalCount > 0) {
      orchestratorApi.recordAnswer(correctCount > wrongCount, `练习${totalCount}题/正确${correctCount}`).catch(() => {})
    }

    // 异步请求 AI 分析
    if (totalCount > 0) {
      analysisLoading.value = true
      try {
        const wrongDetails = wrongAnswers.value
          .map(w => `题目:${w.stem} 正确:${w.correctAnswer} 回答:${w.userAnswer}`)
          .join('; ')
        const { data: res } = await practiceApi.analyzePractice({
          totalCount,
          correctCount,
          wrongDetails: wrongDetails || '全部正确',
          grade: selectedGrade.value,
        })
        aiAnalysis.value = res.data.analysis
      } catch (e) {
        console.warn('AI 分析失败', e)
        aiAnalysis.value = ''
      } finally {
        analysisLoading.value = false
      }
    }
  } catch (e) {
    showToast('结束练习失败', 'error')
  }
}

function resetPractice() {
  phase.value = 'setup'
  question.value = null
  feedback.value = null
  practiceResult.value = null
  wrongAnswers.value = []
  aiAnalysis.value = ''
  analysisLoading.value = false
}

function optionClass(opt: string) {
  if (!feedback.value) return selectedAnswer.value === opt ? 'selected' : ''
  if (opt === feedback.value.correctAnswer) return 'correct'
  if (!feedback.value.correct && opt === selectedAnswer.value) return 'wrong'
  return 'dimmed'
}

onMounted(async () => {
  const qUnitId = route.query.unitId
  const qUnitName = route.query.unitName as string
  const qGrade = route.query.grade as string
  const qLevelName = route.query.levelName as string
  const qStudySetId = route.query.studySetId
  const qStudySetName = route.query.studySetName as string
  if (qStudySetId) {
    studySetMode.value = true
    studySetId.value = Number(qStudySetId)
    studySetName.value = qStudySetName || '学习集练习'
    if (qGrade) selectedGrade.value = qGrade
  } else if (qUnitId) {
    unitMode.value = true
    unitId.value = Number(qUnitId)
    unitName.value = qUnitName || '单元练习'
    levelName.value = qLevelName || ''
    if (qGrade) selectedGrade.value = qGrade
  } else {
    // 无参数时：从编排引擎获取推荐级别 → 自动匹配单元进入单元练习
    let autoMatched = false
    try {
      const { data: planRes } = await orchestratorApi.getPlan()
      const lc = planRes.data?.levelCode
      if (lc) {
        const num = parseInt(lc.replace('L', ''), 10)
        if (num >= 3 && num <= 6) selectedGrade.value = 'primary'
        else if (num >= 7 && num <= 9) selectedGrade.value = 'junior'
        else if (num >= 10) selectedGrade.value = 'senior'

        // 查找该级别下有知识点的单元
        const { data: levelsData } = await levelApi.list()
        const targetLevel = levelsData.data?.find((l: any) => l.code === lc)
        if (targetLevel && targetLevel.unitCount > 0) {
          const { data: detailData } = await levelApi.getDetail(targetLevel.id)
          const units = detailData.data?.units || []
          // 找第一个有知识点且未100%完成的单元
          const targetUnit = units.find((u: any) => u.kpCount > 0 && (u.progress || 0) < 100)
            || units.find((u: any) => u.kpCount > 0)
          if (targetUnit) {
            unitMode.value = true
            unitId.value = targetUnit.id
            unitName.value = targetUnit.name
            levelName.value = targetLevel.name
            autoMatched = true
          }
        }
      }
    } catch { /* 降级到题库模式 */ }

    if (!autoMatched) {
      loadBanks()
    }
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.practice {
  max-width: 680px;
  margin: 0 auto;
}

// ---- Setup ----
.setup-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-2xl;
  padding: 32px;
  animation: slideUp 0.3s ease-out;

  h1 {
    font-size: 24px;
    font-weight: 800;
    color: var(--text-primary);
    margin-bottom: 4px;
  }
}

.setup-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 28px;
}

.form-group {
  margin-bottom: 24px;

  label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 10px;

    .multi-hint {
      font-weight: 400;
      font-size: 12px;
      color: var(--text-tertiary);
    }
  }
}

.bank-select {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.bank-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: $radius-lg;
  background: var(--bg-card);
  cursor: pointer;
  transition: all $transition;

  &.active {
    border-color: var(--primary);
    background: rgba(99, 102, 241, 0.06);
  }
  &:hover:not(.active) { border-color: var(--text-muted); }
  &__name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  &__count { font-size: 12px; color: var(--text-muted); }
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
    background: rgba(99, 102, 241, 0.06);
    color: var(--primary);
    font-weight: 600;
  }
}

.type-select {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.type-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: $radius-lg;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition;
  position: relative;

  &.active {
    border-color: var(--primary);
    background: rgba(99, 102, 241, 0.06);
    color: var(--primary);
    font-weight: 600;
  }
  &:hover:not(.active) { border-color: var(--text-muted); }
}

.type-hint {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: auto;
}

.count-select {
  display: flex;
  gap: 8px;
}

.count-option {
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
    background: rgba(99, 102, 241, 0.06);
    color: var(--primary);
    font-weight: 600;
  }
}

.btn-start {
  width: 100%;
  padding: 14px;
  background: var(--gradient-primary);
  color: #fff;
  border: none;
  border-radius: $radius-lg;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all $transition;
  margin-top: 8px;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(99, 102, 241, 0.35);
  }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: $radius-lg;
  color: var(--danger);
  font-size: 14px;
  font-weight: 500;
}

// ---- Progress ----
.progress-bar {
  height: 6px;
  background: var(--border);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: 3px;
  transition: width 0.4s ease;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 20px;
}

.correct-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--success);
  font-weight: 600;
}

// ---- Question card ----
.question-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-2xl;
  padding: 28px;
  animation: slideUp 0.3s ease-out;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.q-type-badge {
  padding: 4px 12px;
  background: rgba(99, 102, 241, 0.08);
  color: var(--primary);
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
}

.q-grade-badge {
  padding: 4px 10px;
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning);
  border-radius: $radius-full;
  font-size: 11px;
  font-weight: 600;
}

.q-diff { font-size: 12px; color: var(--warning); letter-spacing: 1px; margin-left: auto; }

.question-stem-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 24px;
}

.question-stem {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.5;
  white-space: pre-line;
  flex: 1;
}

// ---- Options ----
.options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 2px solid var(--border);
  border-radius: $radius-lg;
  background: var(--bg-card);
  cursor: pointer;
  transition: all 0.25s ease;
  text-align: left;

  &:hover:not(:disabled):not(.correct):not(.wrong):not(.dimmed) {
    border-color: var(--primary);
    background: rgba(99, 102, 241, 0.04);
  }
  &.selected { border-color: var(--primary); background: rgba(99, 102, 241, 0.06); }
  &.correct {
    border-color: var(--success);
    background: rgba(16, 185, 129, 0.08);
    .option-letter { background: var(--success); color: #fff; }
    .option-text { color: var(--success); font-weight: 600; }
  }
  &.wrong {
    border-color: var(--danger);
    background: rgba(239, 68, 68, 0.06);
    animation: shake 0.3s ease;
    .option-letter { background: var(--danger); color: #fff; }
    .option-text { color: var(--danger); }
  }
  &.dimmed { opacity: 0.45; }
  &:disabled { cursor: default; }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.option-letter {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--bg-sidebar, var(--bg-page));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  flex-shrink: 0;
  transition: all 0.25s ease;
}

.option-text {
  font-size: 15px;
  color: var(--text-primary);
  flex: 1;
}

.option-icon {
  flex-shrink: 0;
  &.correct { color: var(--success); }
  &.wrong { color: var(--danger); }
}

// ---- Word order ----
.word-order-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.word-order-result {
  min-height: 48px;
  padding: 12px;
  border: 2px dashed var(--border);
  border-radius: $radius-lg;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.word-order-placeholder {
  color: var(--text-muted);
  font-size: 14px;
}

.word-order-pool {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.word-chip {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &.ordered {
    background: rgba(99, 102, 241, 0.1);
    border: 1px solid var(--primary);
    color: var(--primary);
  }
  &.pool {
    background: var(--bg-card);
    border: 1px solid var(--border);
    color: var(--text-primary);

    &:hover:not(:disabled):not(.used) {
      border-color: var(--primary);
    }
    &.used { opacity: 0.3; cursor: default; }
  }
}

.word-meaning-tip {
  display: none;
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: var(--text-primary);
  color: var(--bg-card);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
}

.word-chip.pool:hover .word-meaning-tip {
  display: block;
}

// ---- Template fill ----
.template-fill-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.template-sentence {
  font-size: 16px;
  line-height: 2;
  color: var(--text-primary);
}

.tpl-text {
  // normal text
}

.tpl-blank {
  display: inline-block;
  width: 120px;
  padding: 4px 8px;
  border: none;
  border-bottom: 2px solid var(--primary);
  background: transparent;
  color: var(--primary);
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  outline: none;
  margin: 0 4px;

  &:focus { border-bottom-color: var(--primary); }
  &:disabled { opacity: 0.6; }
}

// ---- Input area ----
.input-area {
  display: flex;
  gap: 10px;

  input {
    flex: 1;
    padding: 12px 16px;
    border: 2px solid var(--border);
    border-radius: $radius-lg;
    background: var(--bg-card);
    color: var(--text-primary);
    font-size: 16px;
    outline: none;
    transition: border-color $transition;

    &::placeholder { color: var(--text-muted); }
    &:focus { border-color: var(--primary); }
    &:disabled { opacity: 0.6; }
  }
}

.btn-submit-answer {
  padding: 12px 24px;
  background: var(--gradient-primary);
  color: #fff;
  border: none;
  border-radius: $radius-lg;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;
  white-space: nowrap;

  &:hover:not(:disabled) { box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

// ---- Judging ----
.judging-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  color: var(--text-muted);
  font-size: 14px;
}

// ---- Feedback ----
.feedback {
  margin-top: 20px;
  padding: 16px 20px;
  border-radius: $radius-lg;

  &.correct {
    background: rgba(16, 185, 129, 0.08);
    border: 1px solid rgba(16, 185, 129, 0.2);
    .feedback-header { color: var(--success); }
  }
  &.wrong {
    background: rgba(239, 68, 68, 0.06);
    border: 1px solid rgba(239, 68, 68, 0.15);
    .feedback-header { color: var(--danger); }
  }
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
}

.correct-answer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 14px;

  .ca-label { color: var(--text-muted); }
  .ca-text { font-weight: 600; color: var(--success); }
}

.judge-detail {
  background: rgba(99, 102, 241, 0.06);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.judge-score {
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 6px;
}

.judge-feedback {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 6px;
}

.judge-corrections {
  padding-left: 18px;
  font-size: 13px;
  color: var(--text-secondary);

  li { margin-bottom: 4px; }
}

.feedback-explain {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 14px;
}

.kp-panel {
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.15);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.kp-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--warning);
  margin-bottom: 6px;
}

.kp-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.example-panel {
  background: rgba(99, 102, 241, 0.04);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.example-en {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.example-zh {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

// ---- Retype ----
.retype-area {
  margin: 12px 0;
}

.retype-hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.retype-input-row {
  display: flex;
  gap: 8px;

  input {
    flex: 1;
    padding: 8px 12px;
    border: 2px solid var(--border);
    border-radius: 8px;
    background: var(--bg-card);
    color: var(--text-primary);
    font-size: 14px;
    outline: none;

    &:focus { border-color: var(--primary); }
  }
}

.btn-retype {
  padding: 8px 16px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.retype-wrong {
  font-size: 12px;
  color: var(--danger);
  margin-top: 4px;
}

.retype-ok {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--success);
  font-size: 13px;
  font-weight: 600;
  margin: 8px 0;
}

.btn-next {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 20px;
  background: var(--bg-sidebar, var(--bg-page));
  border: 1px solid var(--border);
  border-radius: calc($radius-lg - 4px);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;

  &:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

// ---- Result ----
.result-page {
  display: flex;
  justify-content: center;
  padding-top: 20px;
}

.result-card {
  text-align: center;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-2xl;
  padding: 40px 36px;
  width: 100%;
  max-width: 440px;
  animation: slideUp 0.4s ease-out;
}

.result-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  animation: bounceIn 0.5s ease-out;

  &.great { background: rgba(16, 185, 129, 0.12); color: var(--success); }
  &.good { background: rgba(245, 158, 11, 0.12); color: var(--warning); }
  &.retry { background: rgba(239, 68, 68, 0.1); color: var(--danger); }
}

@keyframes bounceIn {
  0% { transform: scale(0); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.result-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.result-score {
  font-size: 48px;
  font-weight: 800;
  margin-bottom: 8px;
}

.score-number { color: var(--primary); }
.score-separator { color: var(--text-muted); margin: 0 4px; }
.score-total { color: var(--text-secondary); }

.result-accuracy {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.result-duration {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 24px;
}

.result-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 28px;
}

.result-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;

  &.correct { color: var(--success); }
  &.wrong { color: var(--danger); }
}

.ai-analysis-card {
  background: linear-gradient(135deg, #f0f4ff, #fef9f0);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 4px;

  .analysis-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    color: var(--primary);
    margin-bottom: 10px;
    font-size: 0.95rem;
  }

  .analysis-loading {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #888;
    font-size: 0.9rem;
  }

  .analysis-content {
    font-size: 0.9rem;
    line-height: 1.6;
    color: #333;
    white-space: pre-line;
  }
}

.result-actions {
  display: flex;
  gap: 12px;
}

.btn-retry, .btn-back {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border-radius: $radius-lg;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition;
}

.btn-retry {
  background: var(--gradient-primary);
  color: #fff;
  border: none;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
  }
}

.btn-back {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border);

  &:hover { border-color: var(--primary); color: var(--primary); }
}

// ---- Shared ----
.loading-inline {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.spinner-sm {
  width: 24px;
  height: 24px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-enter-active { transition: all 0.3s ease; }
.fade-enter-from { opacity: 0; transform: translateY(8px); }

// ---- Mobile ----
@media (max-width: 767px) {
  .setup-card { padding: 24px 20px; }
  .question-card { padding: 20px; }
  .question-stem { font-size: 17px; }
  .options-grid { grid-template-columns: 1fr; }
  .type-select { grid-template-columns: 1fr 1fr; }
  .result-card { padding: 28px 20px; }
  .result-score { font-size: 36px; }
  .result-actions { flex-direction: column; }
}
</style>
