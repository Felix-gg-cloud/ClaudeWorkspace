<template>
  <div class="page-container">
    <div class="today-header">
      <h1 class="today-title">
        <span class="title-icon">📝</span>
        <span class="text-gradient">今日学习</span>
      </h1>
      <div class="day-badge" v-if="lesson">Day {{ lesson.dayIndex }}</div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loader">
        <div class="loader-ring"></div>
        <div class="loader-ring"></div>
        <div class="loader-ring"></div>
      </div>
      <p>加载课程中...</p>
    </div>

    <!-- No lesson -->
    <div v-else-if="!lesson" class="empty-state glass-card">
      <div class="empty-icon">🎊</div>
      <h3>暂无课程</h3>
      <p>休息一下，明天继续加油！</p>
      <button class="btn-primary" @click="$router.push('/stages')" style="margin-top: 16px">
        🗺️ 查看学习旅程
      </button>
    </div>

    <!-- Lesson Content -->
    <template v-else>
      <!-- Lesson Info Card -->
      <div class="glass-card lesson-info">
        <h2 class="lesson-title">{{ lesson.title }}</h2>
        <p class="lesson-desc">{{ lesson.description }}</p>

        <!-- Progress dots -->
        <div class="progress-dots">
          <div
            v-for="(task, i) in lesson.tasks"
            :key="task.id"
            class="dot"
            :class="{
              completed: task.completed,
              current: !task.completed && i === currentTaskAbsIndex,
              upcoming: !task.completed && i !== currentTaskAbsIndex
            }"
          >
            <span v-if="task.completed">✓</span>
            <span v-else>{{ i + 1 }}</span>
          </div>
        </div>

        <div class="progress-row">
          <div class="progress-bar" style="flex:1">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <span class="progress-label">{{ lesson.completedCount }}/{{ lesson.totalCount }}</span>
        </div>
      </div>

      <!-- All Complete -->
      <div v-if="lesson.allCompleted && !currentTask" class="glass-card complete-banner">
        <div class="complete-particles">
          <span v-for="i in 12" :key="i" class="particle" :style="particleStyle(i)">✨</span>
        </div>
        <div class="complete-icon">🎉</div>
        <h3 class="text-gradient">今日课程已完成！</h3>
        <p>太棒了，已自动打卡成功</p>
        <div class="complete-stats">
          <div class="cs-item">
            <span class="cs-val text-gradient">{{ lesson.totalCount }}</span>
            <span class="cs-label">任务完成</span>
          </div>
          <div class="cs-divider"></div>
          <div class="cs-item">
            <span class="cs-val text-gradient-warm">{{ totalXpEarned }}</span>
            <span class="cs-label">获得 XP</span>
          </div>
        </div>
      </div>

      <!-- Task Card -->
      <div v-if="currentTask" class="task-area">

        <!-- VOCAB_CARD -->
        <div v-if="currentTask.type === 'VOCAB_CARD'" class="glass-card task-card vocab-card" @click="!vocabFlipped && (vocabFlipped = true)">
          <div class="task-type-chip vocab">📖 词汇卡片</div>

          <Transition name="card-flip" mode="out-in">
            <div v-if="!vocabFlipped" key="front" class="vocab-content">
              <div class="vocab-word">{{ currentTask.question }}</div>
              <div class="tap-hint">
                <span class="tap-icon">👆</span> 点击翻转查看释义
              </div>
            </div>
            <div v-else key="back" class="vocab-content">
              <div class="vocab-answer text-gradient">{{ currentTask.correctAnswer }}</div>
              <div class="vocab-explain">{{ currentTask.explanation }}</div>
              <button class="btn-primary" @click.stop="submitTask" :disabled="submitting">
                ✅ 已掌握 <span class="xp-tag">+{{ currentTask.xpReward }} XP</span>
              </button>
            </div>
          </Transition>
        </div>

        <!-- QUIZ_SINGLE -->
        <div v-else-if="currentTask.type === 'QUIZ_SINGLE'" class="glass-card task-card quiz-card">
          <div class="task-type-chip quiz">🧠 选择题</div>
          <h3 class="quiz-question">{{ currentTask.question }}</h3>
          <div class="quiz-options">
            <button
              v-for="(opt, i) in parsedOptions"
              :key="i"
              class="quiz-option"
              :class="{
                selected: selectedOption === opt && !showResult,
                correct: showResult && opt === currentTask.correctAnswer,
                wrong: showResult && selectedOption === opt && opt !== currentTask.correctAnswer
              }"
              :disabled="showResult"
              @click="selectOption(opt)"
            >
              <span class="opt-letter">{{ ['A', 'B', 'C', 'D'][i] }}</span>
              <span class="opt-text">{{ opt }}</span>
              <span v-if="showResult && opt === currentTask.correctAnswer" class="opt-icon">✅</span>
              <span v-if="showResult && selectedOption === opt && opt !== currentTask.correctAnswer" class="opt-icon">❌</span>
            </button>
          </div>
          <Transition name="page-slide">
            <div v-if="showResult" class="explain-box">
              <p>💡 {{ currentTask.explanation }}</p>
              <button class="btn-primary" @click="submitTask" :disabled="submitting">
                继续 <span class="xp-tag">+{{ currentTask.xpReward }} XP</span>
              </button>
            </div>
          </Transition>
        </div>

        <!-- SPELLING -->
        <div v-else-if="currentTask.type === 'SPELLING'" class="glass-card task-card spelling-card">
          <div class="task-type-chip spelling">✍️ 拼写题</div>
          <h3 class="spelling-question">{{ currentTask.question }}</h3>

          <div class="spelling-input-area">
            <input
              v-model="spellingAnswer"
              type="text"
              class="input-dark spelling-input"
              placeholder="输入英文拼写..."
              @keyup.enter="checkSpelling"
              :disabled="spellingChecked"
              autocomplete="off"
              autocapitalize="off"
            />
            <button
              v-if="!spellingChecked"
              class="btn-primary check-btn"
              @click="checkSpelling"
              :disabled="!spellingAnswer.trim()"
            >
              检查
            </button>
          </div>

          <Transition name="page-slide">
            <div v-if="spellingChecked" class="spelling-result">
              <div v-if="spellingCorrect" class="result-correct">
                <span class="result-icon">🎉</span> 正确！
              </div>
              <div v-else class="result-wrong">
                <span class="result-icon">❌</span>
                正确答案: <strong class="text-gradient">{{ currentTask.correctAnswer }}</strong>
              </div>
              <div class="explain-box">
                <p>💡 {{ currentTask.explanation }}</p>
                <button class="btn-primary" @click="submitTask" :disabled="submitting">
                  继续 <span class="xp-tag">+{{ currentTask.xpReward }} XP</span>
                </button>
              </div>
            </div>
          </Transition>
        </div>

        <!-- Skip Button -->
        <button class="btn-ghost skip-btn" @click="skipTask" v-if="currentTask && !submitting">
          跳过此题 ›
        </button>
      </div>
    </template>

    <!-- Celebration Modal -->
    <Teleport to="body">
      <Transition name="scale-in">
        <div v-if="showCelebration" class="celebration-overlay" @click="closeCelebration">
          <div class="celebration-modal" @click.stop>
            <div class="confetti-icon">{{ celebrationTitle.includes('升级') ? '🎊' : '🎉' }}</div>
            <h2>{{ celebrationTitle }}</h2>
            <p>{{ celebrationMsg }}</p>
            <div v-if="resultData" class="celebrate-chips">
              <span v-if="resultData.xpGained" class="c-chip xp">+{{ resultData.xpGained }} XP</span>
              <span v-if="resultData.coinsGained" class="c-chip coins">+{{ resultData.coinsGained }} 💎</span>
              <span v-if="resultData.streak" class="c-chip streak">🔥 {{ resultData.streak }} 天</span>
            </div>
            <div v-if="resultData?.newAchievements?.length" class="new-achievements">
              <h4>🏆 新成就解锁！</h4>
              <div v-for="a in resultData.newAchievements" :key="a.id" class="ach-item">
                {{ a.icon }} {{ a.name }}
              </div>
            </div>
            <button class="btn-primary" @click="closeCelebration">太棒了！</button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api'
import { useUserStore } from '@/stores/user'
import type { TodayLessonDto, TaskDto, TaskCompleteResult } from '@/types'

const userStore = useUserStore()
const lesson = ref<TodayLessonDto | null>(null)
const loading = ref(true)
const currentTaskIndex = ref(0)
const submitting = ref(false)
const totalXpEarned = ref(0)

// Vocab
const vocabFlipped = ref(false)

// Quiz
const selectedOption = ref('')
const showResult = ref(false)

// Spelling
const spellingAnswer = ref('')
const spellingChecked = ref(false)
const spellingCorrect = ref(false)

// Celebration
const showCelebration = ref(false)
const celebrationTitle = ref('')
const celebrationMsg = ref('')
const resultData = ref<TaskCompleteResult | null>(null)

const currentTask = computed<TaskDto | null>(() => {
  if (!lesson.value) return null
  const incomplete = lesson.value.tasks.filter(t => !t.completed)
  return incomplete.length > 0 ? incomplete[0] : null
})

const currentTaskAbsIndex = computed(() => {
  if (!lesson.value || !currentTask.value) return -1
  return lesson.value.tasks.findIndex(t => t.id === currentTask.value!.id)
})

const parsedOptions = computed(() => {
  if (!currentTask.value?.options) return []
  try { return JSON.parse(currentTask.value.options) as string[] }
  catch { return [] }
})

const progressPercent = computed(() => {
  if (!lesson.value) return 0
  return (lesson.value.completedCount / lesson.value.totalCount) * 100
})

function particleStyle(i: number) {
  const angle = (i / 12) * 360
  const r = 40 + Math.random() * 30
  return {
    left: `${50 + r * Math.cos(angle * Math.PI / 180)}%`,
    top: `${50 + r * Math.sin(angle * Math.PI / 180)}%`,
    animationDelay: `${i * 0.15}s`,
  }
}

function selectOption(opt: string) {
  selectedOption.value = opt
  showResult.value = true
}

function checkSpelling() {
  if (!spellingAnswer.value.trim()) return
  spellingChecked.value = true
  spellingCorrect.value = spellingAnswer.value.trim().toLowerCase() === currentTask.value?.correctAnswer?.toLowerCase()
}

function skipTask() {
  if (!currentTask.value) return
  const task = lesson.value!.tasks.find(t => t.id === currentTask.value!.id)
  if (task) task.completed = true
  lesson.value!.completedCount++
  resetTaskState()
}

function resetTaskState() {
  vocabFlipped.value = false
  selectedOption.value = ''
  showResult.value = false
  spellingAnswer.value = ''
  spellingChecked.value = false
  spellingCorrect.value = false
}

async function submitTask() {
  if (!currentTask.value || submitting.value) return
  submitting.value = true
  try {
    const { data } = await api.post<TaskCompleteResult>(`/tasks/${currentTask.value.id}/complete`)
    resultData.value = data
    totalXpEarned.value += data.xpGained
    userStore.updateXp(data.totalXp)

    const task = lesson.value!.tasks.find(t => t.id === currentTask.value!.id)
    if (task) task.completed = true
    lesson.value!.completedCount++
    resetTaskState()

    if (data.lessonCompleted) {
      celebrationTitle.value = '🎉 今日课程完成！'
      celebrationMsg.value = '你完成了所有任务，已自动打卡！'
      lesson.value!.allCompleted = true
      showCelebration.value = true
    } else if (data.levelUp) {
      celebrationTitle.value = '🎊 升级了！'
      celebrationMsg.value = `恭喜达到 ${data.newLevelTitle}！`
      showCelebration.value = true
    }

    await userStore.fetchMe()
  } catch (e) {
    console.error('Failed to complete task', e)
  } finally {
    submitting.value = false
  }
}

function closeCelebration() {
  showCelebration.value = false
  resultData.value = null
}

onMounted(async () => {
  try {
    const { data } = await api.get('/today')
    if (data.lessonId) {
      lesson.value = data
      const idx = data.tasks.findIndex((t: TaskDto) => !t.completed)
      currentTaskIndex.value = idx >= 0 ? idx : 0
      totalXpEarned.value = data.tasks.filter((t: TaskDto) => t.completed).reduce((sum: number, t: TaskDto) => sum + t.xpReward, 0)
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.today-header {
  padding: 28px 0 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.today-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 800;
}

.title-icon { font-size: 26px; }

.day-badge {
  padding: 6px 14px;
  background: rgba(0, 212, 255, 0.1);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  color: #00d4ff;
}

// Loading
.loading-state {
  text-align: center;
  padding: 80px 0;
  color: #5a6480;
}

.loader {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.loader-ring {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00d4ff, #a855f7);
  animation: typing-dots 1.2s infinite;
  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

// Empty
.empty-state {
  text-align: center;
  padding: 48px 24px;
  margin-top: 20px;
}
.empty-icon { font-size: 60px; margin-bottom: 12px; }
.empty-state h3 { font-size: 20px; margin-bottom: 6px; color: #e8ecf4; }
.empty-state p { color: #5a6480; font-size: 14px; }

// Lesson Info
.lesson-info { margin-bottom: 16px; }

.lesson-title {
  font-size: 18px;
  font-weight: 700;
  color: #e8ecf4;
  margin-bottom: 4px;
}

.lesson-desc {
  font-size: 13px;
  color: #5a6480;
  margin-bottom: 14px;
}

// Progress dots
.progress-dots {
  display: flex;
  gap: 6px;
  justify-content: center;
  margin-bottom: 14px;
}

.dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  transition: all 0.3s;

  &.completed {
    background: linear-gradient(135deg, #10b981, #06b6d4);
    color: white;
    box-shadow: 0 0 10px rgba(16, 185, 129, 0.3);
  }

  &.current {
    background: linear-gradient(135deg, #00d4ff, #a855f7);
    color: white;
    animation: pulse-glow 2s infinite;
    box-shadow: 0 0 15px rgba(0, 212, 255, 0.4);
  }

  &.upcoming {
    background: rgba(255, 255, 255, 0.06);
    color: #5a6480;
    border: 1px solid rgba(255, 255, 255, 0.08);
  }
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-label {
  font-size: 13px;
  color: #00d4ff;
  font-weight: 700;
  white-space: nowrap;
}

// Complete
.complete-banner {
  text-align: center;
  padding: 40px 24px;
  position: relative;
  overflow: hidden;
}

.complete-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  font-size: 14px;
  animation: float 4s infinite ease-in-out;
}

.complete-icon { font-size: 56px; margin-bottom: 12px; }
.complete-banner h3 { font-size: 22px; margin-bottom: 6px; }
.complete-banner p { color: #5a6480; font-size: 14px; margin-bottom: 20px; }

.complete-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.cs-item { text-align: center; }
.cs-val { font-size: 24px; font-weight: 800; display: block; }
.cs-label { font-size: 11px; color: #5a6480; }
.cs-divider {
  width: 1px;
  height: 36px;
  background: rgba(255,255,255,0.08);
}

// Task area
.task-area {
  animation: slide-up 0.3s ease-out;
}

// Task type chips
.task-type-chip {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 18px;

  &.vocab {
    background: rgba(0, 212, 255, 0.1);
    color: #00d4ff;
    border: 1px solid rgba(0, 212, 255, 0.15);
  }
  &.quiz {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.15);
  }
  &.spelling {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
    border: 1px solid rgba(16, 185, 129, 0.15);
  }
}

// Vocab
.vocab-card {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  cursor: pointer;
}

.vocab-content {
  text-align: center;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.vocab-word {
  font-size: 32px;
  font-weight: 800;
  color: #e8ecf4;
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.tap-hint {
  color: #5a6480;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tap-icon { animation: pulse-glow 2s infinite; }

.vocab-answer {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 12px;
}

.vocab-explain {
  color: #8b95b0;
  font-size: 14px;
  margin-bottom: 24px;
  line-height: 1.7;
}

// Card flip transition
.card-flip-enter-active,
.card-flip-leave-active {
  transition: all 0.3s ease;
}
.card-flip-enter-from { opacity: 0; transform: rotateY(90deg) scale(0.9); }
.card-flip-leave-to { opacity: 0; transform: rotateY(-90deg) scale(0.9); }

// Quiz
.quiz-question {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.6;
  margin-bottom: 20px;
  color: #e8ecf4;
}

.quiz-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quiz-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  font-size: 15px;
  color: #e8ecf4;
  transition: all 0.25s;
  text-align: left;

  &:hover:not(:disabled) {
    border-color: rgba(0, 212, 255, 0.3);
    background: rgba(0, 212, 255, 0.05);
  }

  &.selected {
    border-color: rgba(0, 212, 255, 0.4);
    background: rgba(0, 212, 255, 0.08);
  }

  &.correct {
    border-color: rgba(16, 185, 129, 0.5);
    background: rgba(16, 185, 129, 0.1);
    box-shadow: 0 0 15px rgba(16, 185, 129, 0.15);
  }

  &.wrong {
    border-color: rgba(239, 68, 68, 0.5);
    background: rgba(239, 68, 68, 0.1);
    animation: shake 0.4s;
  }
}

.opt-letter {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;

  .correct & { background: rgba(16, 185, 129, 0.2); color: #10b981; }
  .wrong & { background: rgba(239, 68, 68, 0.2); color: #ef4444; }
}

.opt-text { flex: 1; }
.opt-icon { font-size: 18px; }

// Explain box
.explain-box {
  margin-top: 16px;
  text-align: center;

  p {
    background: rgba(245, 158, 11, 0.08);
    border: 1px solid rgba(245, 158, 11, 0.15);
    padding: 12px 16px;
    border-radius: 12px;
    font-size: 14px;
    color: #f59e0b;
    margin-bottom: 16px;
    line-height: 1.6;
  }
}

// Spelling
.spelling-question {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  line-height: 1.6;
  color: #e8ecf4;
}

.spelling-input-area {
  display: flex;
  gap: 10px;
}

.spelling-input {
  flex: 1;
  font-size: 16px;
  letter-spacing: 1px;
}

.check-btn {
  padding: 12px 24px;
  white-space: nowrap;
}

.spelling-result {
  margin-top: 16px;
}

.result-correct, .result-wrong {
  text-align: center;
  padding: 12px;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.result-correct {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.result-wrong {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
  font-size: 15px;

  strong { font-size: 18px; }
}

.result-icon { margin-right: 6px; }

// XP tag
.xp-tag {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(255,255,255,0.15);
  border-radius: 8px;
  font-size: 12px;
  margin-left: 6px;
}

// Skip button
.skip-btn {
  display: block;
  margin: 12px auto 0;
  font-size: 13px;
}

// Celebration
.confetti-icon { font-size: 56px; }

.celebrate-chips {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.c-chip {
  padding: 6px 16px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 14px;

  &.xp {
    background: rgba(0, 212, 255, 0.1);
    color: #00d4ff;
    border: 1px solid rgba(0, 212, 255, 0.2);
  }
  &.coins {
    background: rgba(168, 85, 247, 0.1);
    color: #c084fc;
    border: 1px solid rgba(168, 85, 247, 0.2);
  }
  &.streak {
    background: rgba(249, 115, 22, 0.1);
    color: #fb923c;
    border: 1px solid rgba(249, 115, 22, 0.2);
  }
}

.new-achievements {
  margin: 12px 0 16px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;

  h4 { margin-bottom: 8px; font-size: 14px; }
}

.ach-item {
  font-size: 14px;
  padding: 4px 0;
  color: #8b95b0;
}

// Scale-in transition
.scale-in-enter-active,
.scale-in-leave-active {
  transition: all 0.3s ease;
}
.scale-in-enter-from,
.scale-in-leave-to {
  opacity: 0;
  transform: scale(0.85);
}
</style>
