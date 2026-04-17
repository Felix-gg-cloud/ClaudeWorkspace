<template>
  <MainLayout>
    <div class="exam-page">
      <!-- Exam select screen -->
      <section v-if="phase === 'select'" class="exam-select">
        <h1 class="page-title text-gold">� 试炼之门</h1>
        <p class="page-desc">跨越试炼，突破等级封印，解锁更强力的学习内容</p>

        <div class="current-level dark-panel">
          <span class="level-label">当前等级</span>
          <span class="level-badge" :class="'badge-' + userStore.user.cefrLevel.toLowerCase().replace('_', '')">
            {{ userStore.user.cefrLevel }}
          </span>
        </div>

        <div class="exam-list">
          <div
            v-for="exam in exams"
            :key="exam.cefrLevel"
            class="exam-card dark-panel"
            :class="{ 'exam-locked': !exam.unlocked, 'exam-passed': exam.passed }"
          >
            <div class="exam-header">
              <span class="exam-badge" :class="'badge-' + exam.cefrLevel.toLowerCase().replace('_', '')">
                {{ exam.cefrLevel }}
              </span>
              <h3>{{ exam.title }}</h3>
            </div>
            <div class="exam-meta">
              <span>📝 {{ exam.totalQuestions }} 题</span>
              <span>✅ 通过率 {{ Math.round(exam.passRate * 100) }}%</span>
              <span v-if="exam.timeLimitPerQuestion">⏱ {{ exam.timeLimitPerQuestion }}s/题</span>
            </div>
            <div v-if="exam.passed" class="exam-result-tag tag-pass">
              ✓ 已通过 · 最佳 {{ Math.round((exam.bestScore ?? 0) * 100) }}%
            </div>
            <div v-else-if="exam.attempts > 0" class="exam-result-tag tag-fail">
              尝试 {{ exam.attempts }} 次 · 今日剩余 {{ exam.dailyAttemptsLeft }} 次
            </div>
            <button
              v-if="exam.unlocked && !exam.passed && exam.dailyAttemptsLeft > 0"
              class="btn-gold exam-start-btn"
              @click="startExam(exam)"
            >
              开始测验
            </button>
            <div v-else-if="!exam.unlocked" class="exam-lock-hint">🔒 通过上一级测验解锁</div>
            <div v-else-if="exam.dailyAttemptsLeft <= 0" class="exam-lock-hint">⏳ 今日次数已用完</div>
          </div>
        </div>
      </section>

      <!-- Exam in progress -->
      <section v-else-if="phase === 'exam'" class="exam-area">
        <!-- Top bar -->
        <div class="exam-topbar dark-panel">
          <span class="exam-topbar-level" :class="'badge-' + activeExam!.cefrLevel.toLowerCase().replace('_', '')">
            {{ activeExam!.cefrLevel }}
          </span>
          <div class="exam-progress-bar">
            <div class="exam-progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <span class="exam-progress-text">{{ examIdx + 1 }} / {{ examTasks.length }}</span>
          <div v-if="timeLeft !== null" class="exam-timer" :class="{ 'timer-warn': timeLeft <= 5 }">
            ⏱ {{ timeLeft }}s
          </div>
        </div>

        <!-- Task card -->
        <transition name="page-fade" mode="out-in">
          <component
            :is="examTaskComponent"
            :key="currentExamTask!.id"
            :task="currentExamTask!"
            @answer="onExamAnswer"
          />
        </transition>
      </section>

      <!-- Result screen -->
      <section v-else-if="phase === 'result'" class="result-area">
        <div class="result-card dark-panel">
          <div v-if="passed" class="result-icon result-pass">🏆</div>
          <div v-else class="result-icon result-fail">💔</div>

          <h2 :class="passed ? 'text-gold' : 'text-red'">
            {{ passed ? '测验通过！' : '未能通过' }}
          </h2>

          <div class="result-score">
            <div class="score-circle" :class="passed ? 'score-pass' : 'score-fail'">
              {{ Math.round(scorePercent) }}%
            </div>
            <p>正确 {{ correctCount }} / {{ examTasks.length }}</p>
            <p>通过线 {{ Math.round((activeExam?.passRate ?? 0.7) * 100) }}%</p>
          </div>

          <div v-if="passed" class="level-up-anim">
            <div class="level-up-text text-gold">
              🎊 等级提升: {{ activeExam!.cefrLevel }} → {{ nextLevel }}
            </div>
          </div>

          <div class="result-weak" v-if="weakTypes.length > 0">
            <h4>薄弱环节</h4>
            <div class="weak-tags">
              <span v-for="w in weakTypes" :key="w" class="weak-tag">{{ taskTypeLabel(w) }}</span>
            </div>
          </div>

          <div class="result-actions">
            <button class="btn-gold" @click="backToSelect">返回测验列表</button>
            <button v-if="!passed" class="btn-outline" @click="retryExam">再试一次</button>
          </div>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'
import { useSound } from '@/composables/useSound'
import { mockCefrExams, mockExamTasks } from '@/mock/data'
import type { Task, CefrExam } from '@/types'
import McqCard from '@/components/ui/McqCard.vue'
import McqReverseCard from '@/components/ui/McqReverseCard.vue'
import FlashCard from '@/components/ui/FlashCard.vue'
import SpellingCard from '@/components/ui/SpellingCard.vue'
import WordOrderCard from '@/components/ui/WordOrderCard.vue'
import FillBlankCard from '@/components/ui/FillBlankCard.vue'
import ListenPickCard from '@/components/ui/ListenPickCard.vue'
import ListenFillCard from '@/components/ui/ListenFillCard.vue'
import DialogueCard from '@/components/ui/DialogueCard.vue'
import SituationPickCard from '@/components/ui/SituationPickCard.vue'
import ErrorFixCard from '@/components/ui/ErrorFixCard.vue'

const userStore = useUserStore()
const sound = useSound()

type Phase = 'select' | 'exam' | 'result'
const phase = ref<Phase>('select')

const exams = ref<CefrExam[]>(JSON.parse(JSON.stringify(mockCefrExams)))
const activeExam = ref<CefrExam | null>(null)
const examTasks = ref<Task[]>([])
const examIdx = ref(0)
const correctCount = ref(0)
const answers = ref<{ type: string; correct: boolean }[]>([])
const timeLeft = ref<number | null>(null)
let timer: ReturnType<typeof setInterval> | null = null

const currentExamTask = computed(() => examTasks.value[examIdx.value] ?? null)
const progressPercent = computed(() => ((examIdx.value) / examTasks.value.length) * 100)
const scorePercent = computed(() => (correctCount.value / examTasks.value.length) * 100)
const passed = computed(() => scorePercent.value >= (activeExam.value?.passRate ?? 0.7) * 100)

const cefrOrder = ['PRE_A1', 'A1', 'A2', 'B1'] as const
const nextLevel = computed(() => {
  const idx = cefrOrder.indexOf(activeExam.value?.cefrLevel as any)
  return idx < cefrOrder.length - 1 ? cefrOrder[idx + 1] : cefrOrder[idx]
})

const weakTypes = computed(() => {
  const typeMap: Record<string, { total: number; wrong: number }> = {}
  for (const a of answers.value) {
    if (!typeMap[a.type]) typeMap[a.type] = { total: 0, wrong: 0 }
    typeMap[a.type].total++
    if (!a.correct) typeMap[a.type].wrong++
  }
  return Object.entries(typeMap)
    .filter(([, v]) => v.wrong > 0)
    .sort((a, b) => b[1].wrong - a[1].wrong)
    .map(([k]) => k)
})

const examTaskComponent = computed(() => {
  if (!currentExamTask.value) return null
  const map: Record<string, any> = {
    MCQ: McqCard,
    MCQ_REVERSE: McqReverseCard,
    FLASHCARD: FlashCard,
    SPELLING: SpellingCard,
    WORD_ORDER: WordOrderCard,
    FILL_BLANK: FillBlankCard,
    LISTEN_PICK: ListenPickCard,
    LISTEN_FILL: ListenFillCard,
    DIALOGUE_REVIEW: DialogueCard,
    SITUATION_PICK: SituationPickCard,
    ERROR_FIX: ErrorFixCard,
  }
  return map[currentExamTask.value.type] ?? McqCard
})

function taskTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    MCQ: '选择题',
    MCQ_REVERSE: '看义选词',
    FLASHCARD: '闪卡记忆',
    SPELLING: '拼写',
    WORD_ORDER: '排词造句',
    FILL_BLANK: '填空',
    LISTEN_PICK: '听音选词',
    LISTEN_FILL: '听音填写',
    DIALOGUE_REVIEW: '对话理解',
    SITUATION_PICK: '情景选择',
    ERROR_FIX: '纠错改正',
  }
  return labels[type] ?? type
}

function startExam(exam: CefrExam) {
  activeExam.value = exam
  examTasks.value = mockExamTasks
    .filter(t => t.cefrLevel === exam.cefrLevel)
    .sort(() => Math.random() - 0.5)
    .slice(0, exam.totalQuestions)
  examIdx.value = 0
  correctCount.value = 0
  answers.value = []
  phase.value = 'exam'
  startTimer()
}

function startTimer() {
  stopTimer()
  if (!activeExam.value?.timeLimitPerQuestion) {
    timeLeft.value = null
    return
  }
  timeLeft.value = activeExam.value.timeLimitPerQuestion
  timer = setInterval(() => {
    if (timeLeft.value !== null && timeLeft.value > 0) {
      timeLeft.value--
    } else {
      onExamAnswer(false)
    }
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function onExamAnswer(correct: boolean) {
  stopTimer()
  const task = currentExamTask.value
  if (task) {
    answers.value.push({ type: task.type, correct })
  }
  if (correct) {
    correctCount.value++
    sound.correct()
  } else {
    sound.wrong()
  }

  setTimeout(() => {
    if (examIdx.value < examTasks.value.length - 1) {
      examIdx.value++
      startTimer()
    } else {
      finishExam()
    }
  }, 600)
}

function finishExam() {
  stopTimer()
  phase.value = 'result'

  if (activeExam.value) {
    activeExam.value.attempts++
    activeExam.value.dailyAttemptsLeft = Math.max(0, activeExam.value.dailyAttemptsLeft - 1)
    const score = correctCount.value / examTasks.value.length
    if (!activeExam.value.bestScore || score > activeExam.value.bestScore) {
      activeExam.value.bestScore = score
    }
    if (passed.value) {
      activeExam.value.passed = true
      sound.victory()
      // Unlock next level
      const nextIdx = cefrOrder.indexOf(activeExam.value.cefrLevel as any) + 1
      if (nextIdx < cefrOrder.length) {
        const nextExam = exams.value.find(e => e.cefrLevel === cefrOrder[nextIdx])
        if (nextExam) nextExam.unlocked = true
      }
    }
  }
}

function backToSelect() {
  phase.value = 'select'
  activeExam.value = null
}

function retryExam() {
  if (activeExam.value && activeExam.value.dailyAttemptsLeft > 0) {
    startExam(activeExam.value)
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.exam-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 24px;
  margin-bottom: 8px;
}
.page-desc {
  color: $text-secondary;
  margin-bottom: 20px;
}

.current-level {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 24px;
  .level-label {
    color: $text-secondary;
    font-size: 14px;
  }
  .level-badge {
    font-size: 18px;
    font-weight: 700;
    padding: 4px 14px;
    border-radius: 6px;
  }
}

// CEFR badge colors
.badge-prea1 { background: #4a3560; color: #c9a0ff; border: 1px solid #7b5ea7; }
.badge-a1 { background: #2a4a2a; color: #7ddf7d; border: 1px solid #4a8a4a; }
.badge-a2 { background: #4a4a2a; color: #dfdf7d; border: 1px solid #8a8a4a; }
.badge-b1 { background: #2a3a4a; color: #7dc8df; border: 1px solid #4a7a8a; }

.exam-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-card {
  padding: 16px 20px;
  border-radius: 10px;
  transition: all .2s;

  &.exam-locked {
    opacity: 0.5;
  }
  &.exam-passed {
    border-color: #4a8a4a;
  }
}

.exam-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;

  .exam-badge {
    font-size: 13px;
    font-weight: 700;
    padding: 3px 10px;
    border-radius: 4px;
  }
  h3 {
    font-size: 16px;
    color: $text-primary;
  }
}

.exam-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 10px;
}

.exam-result-tag {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 10px;
  &.tag-pass { background: rgba(125, 223, 125, 0.15); color: #7ddf7d; }
  &.tag-fail { background: rgba(223, 125, 125, 0.15); color: #df7d7d; }
}

.exam-start-btn {
  width: 100%;
  margin-top: 4px;
}

.exam-lock-hint {
  text-align: center;
  color: $text-secondary;
  font-size: 13px;
  padding: 8px 0;
}

// Exam top bar
.exam-topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 20px;
  border-radius: 10px;
}

.exam-topbar-level {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.exam-progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(255,255,255,0.08);
  border-radius: 4px;
  overflow: hidden;
}
.exam-progress-fill {
  height: 100%;
  background: $gold;
  border-radius: 4px;
  transition: width .3s;
}

.exam-progress-text {
  font-size: 13px;
  color: $text-secondary;
  flex-shrink: 0;
}

.exam-timer {
  font-size: 14px;
  font-weight: 700;
  color: $text-primary;
  flex-shrink: 0;
  &.timer-warn {
    color: #ff5555;
    animation: pulse 0.5s ease-in-out infinite alternate;
  }
}

@keyframes pulse {
  from { opacity: 1; }
  to { opacity: 0.5; }
}

// Result
.result-area {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.result-card {
  text-align: center;
  padding: 32px 24px;
  border-radius: 14px;
  max-width: 400px;
  width: 100%;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 16px;
  &.result-pass { animation: bounce 0.6s ease; }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.text-red { color: #ff5555; }

.result-score {
  margin: 20px 0;
  p {
    color: $text-secondary;
    font-size: 14px;
    margin: 4px 0;
  }
}

.score-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  margin: 0 auto 12px;
  &.score-pass {
    border: 3px solid #7ddf7d;
    color: #7ddf7d;
    background: rgba(125, 223, 125, 0.1);
  }
  &.score-fail {
    border: 3px solid #ff5555;
    color: #ff5555;
    background: rgba(255, 85, 85, 0.1);
  }
}

.level-up-anim {
  margin: 16px 0;
  animation: glow 1s ease-in-out infinite alternate;
}

.level-up-text {
  font-size: 18px;
  font-weight: 700;
}

@keyframes glow {
  from { text-shadow: 0 0 4px rgba(255, 215, 0, 0.4); }
  to { text-shadow: 0 0 16px rgba(255, 215, 0, 0.8); }
}

.result-weak {
  margin: 20px 0;
  h4 {
    color: $text-secondary;
    font-size: 14px;
    margin-bottom: 8px;
  }
}

.weak-tags {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.weak-tag {
  background: rgba(255, 85, 85, 0.15);
  color: #ff8888;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.btn-outline {
  padding: 10px 24px;
  border: 1px solid $gold;
  color: $gold;
  background: transparent;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  &:hover { background: rgba(255, 215, 0, 0.1); }
}

// Animations
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity .25s, transform .25s;
}
.page-fade-enter-from { opacity: 0; transform: translateX(30px); }
.page-fade-leave-to { opacity: 0; transform: translateX(-30px); }
</style>
