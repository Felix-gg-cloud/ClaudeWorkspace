<template>
  <div class="page-container">
    <div class="today-header">
      <h1 class="today-title">📝 今日学习</h1>
      <p class="today-sub" v-if="lesson">第 {{ lesson.dayIndex }} 天</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载课程中...</p>
    </div>

    <!-- No lesson -->
    <div v-else-if="!lesson" class="empty-state">
      <div class="empty-icon">🎊</div>
      <p>暂无课程，休息一下吧！</p>
    </div>

    <!-- Lesson Content -->
    <template v-else>
      <div class="lesson-info card">
        <h2 class="lesson-title">{{ lesson.title }}</h2>
        <p class="lesson-desc">{{ lesson.description }}</p>
        <div class="lesson-progress-row">
          <div class="level-progress" style="flex:1">
            <div class="level-progress-bar" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <span class="progress-text">{{ lesson.completedCount }}/{{ lesson.totalCount }}</span>
        </div>
      </div>

      <!-- All complete -->
      <div v-if="lesson.allCompleted && !currentTask" class="complete-banner animate__animated animate__bounceIn">
        <div class="complete-icon">🎉</div>
        <h3>今日课程已完成！</h3>
        <p>太棒了，明天继续加油！</p>
      </div>

      <!-- Task Card -->
      <div v-if="currentTask" class="task-area">
        <div class="task-counter">
          任务 {{ currentTaskIndex + 1 }} / {{ lesson.tasks.length }}
        </div>

        <!-- VOCAB_CARD -->
        <div v-if="currentTask.type === 'VOCAB_CARD'" class="card task-card vocab-card" :class="{ flipped: vocabFlipped }">
          <div class="vocab-front" v-if="!vocabFlipped" @click="vocabFlipped = true">
            <div class="vocab-type-badge">📖 词汇卡片</div>
            <div class="vocab-word">{{ currentTask.question }}</div>
            <p class="vocab-hint">👆 点击翻转查看释义</p>
          </div>
          <div class="vocab-back" v-else>
            <div class="vocab-type-badge">📖 词汇卡片</div>
            <div class="vocab-answer">{{ currentTask.correctAnswer }}</div>
            <div class="vocab-explain">{{ currentTask.explanation }}</div>
            <button class="btn-primary" @click="submitTask" :disabled="submitting">
              ✅ 已掌握 (+{{ currentTask.xpReward }} XP)
            </button>
          </div>
        </div>

        <!-- QUIZ_SINGLE -->
        <div v-else-if="currentTask.type === 'QUIZ_SINGLE'" class="card task-card quiz-card">
          <div class="quiz-type-badge">🧠 选择题</div>
          <h3 class="quiz-question">{{ currentTask.question }}</h3>
          <div class="quiz-options">
            <button
              v-for="(opt, i) in parsedOptions"
              :key="i"
              class="quiz-option"
              :class="{
                selected: selectedOption === opt,
                correct: showResult && opt === currentTask.correctAnswer,
                wrong: showResult && selectedOption === opt && opt !== currentTask.correctAnswer
              }"
              :disabled="showResult"
              @click="selectOption(opt)"
            >
              <span class="option-letter">{{ ['A', 'B', 'C', 'D'][i] }}</span>
              <span class="option-text">{{ opt }}</span>
              <span v-if="showResult && opt === currentTask.correctAnswer" class="option-mark">✅</span>
              <span v-if="showResult && selectedOption === opt && opt !== currentTask.correctAnswer" class="option-mark">❌</span>
            </button>
          </div>
          <div v-if="showResult" class="quiz-explain animate__animated animate__fadeInUp">
            <p class="explain-text">💡 {{ currentTask.explanation }}</p>
            <button class="btn-primary" @click="submitTask" :disabled="submitting">
              继续 (+{{ currentTask.xpReward }} XP)
            </button>
          </div>
        </div>

        <!-- SPELLING -->
        <div v-else-if="currentTask.type === 'SPELLING'" class="card task-card spelling-card">
          <div class="spelling-type-badge">✍️ 拼写题</div>
          <h3 class="spelling-question">{{ currentTask.question }}</h3>
          <div class="spelling-input-row">
            <input
              v-model="spellingAnswer"
              type="text"
              class="spelling-input"
              placeholder="输入英文拼写..."
              @keyup.enter="checkSpelling"
              :disabled="spellingChecked"
            />
            <button
              v-if="!spellingChecked"
              class="btn-primary"
              @click="checkSpelling"
            >
              检查
            </button>
          </div>
          <div v-if="spellingChecked" class="spelling-result animate__animated animate__fadeInUp">
            <div v-if="spellingCorrect" class="spelling-correct">
              <span>🎉 正确！</span>
            </div>
            <div v-else class="spelling-wrong">
              <span>❌ 正确答案: <strong>{{ currentTask.correctAnswer }}</strong></span>
            </div>
            <p class="explain-text">💡 {{ currentTask.explanation }}</p>
            <button class="btn-primary" @click="submitTask" :disabled="submitting">
              继续 (+{{ currentTask.xpReward }} XP)
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Celebration Modal -->
    <Teleport to="body">
      <div v-if="showCelebration" class="celebration-overlay" @click="closeCelebration">
        <div class="celebration-modal animate__animated animate__zoomIn">
          <div class="celebration-confetti">🎊</div>
          <h2>{{ celebrationTitle }}</h2>
          <p>{{ celebrationMsg }}</p>
          <div v-if="resultData" class="celebration-stats">
            <div v-if="resultData.xpGained" class="celebrate-stat">+{{ resultData.xpGained }} XP</div>
            <div v-if="resultData.coinsGained" class="celebrate-stat coins">+{{ resultData.coinsGained }} 🪙</div>
            <div v-if="resultData.streak" class="celebrate-stat streak">🔥 {{ resultData.streak }} 天连续</div>
          </div>
          <div v-if="resultData?.newAchievements?.length" class="new-achievements">
            <h4>🏆 新成就解锁！</h4>
            <div v-for="a in resultData.newAchievements" :key="a.id" class="achievement-item">
              {{ a.icon }} {{ a.name }}
            </div>
          </div>
          <button class="btn-primary" @click="closeCelebration">太棒了！</button>
        </div>
      </div>
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
  if (incomplete.length === 0) return null
  return incomplete[0]
})

const parsedOptions = computed(() => {
  if (!currentTask.value?.options) return []
  try {
    return JSON.parse(currentTask.value.options) as string[]
  } catch {
    return []
  }
})

const progressPercent = computed(() => {
  if (!lesson.value) return 0
  return (lesson.value.completedCount / lesson.value.totalCount) * 100
})

function selectOption(opt: string) {
  selectedOption.value = opt
  showResult.value = true
}

function checkSpelling() {
  if (!spellingAnswer.value.trim()) return
  spellingChecked.value = true
  spellingCorrect.value = spellingAnswer.value.trim().toLowerCase() === currentTask.value?.correctAnswer?.toLowerCase()
}

async function submitTask() {
  if (!currentTask.value || submitting.value) return
  submitting.value = true
  try {
    const { data } = await api.post<TaskCompleteResult>(`/tasks/${currentTask.value.id}/complete`)
    resultData.value = data
    userStore.updateXp(data.totalXp)

    // Mark this task as completed in local state
    const task = lesson.value!.tasks.find(t => t.id === currentTask.value!.id)
    if (task) task.completed = true
    lesson.value!.completedCount++

    // Reset UI state
    vocabFlipped.value = false
    selectedOption.value = ''
    showResult.value = false
    spellingAnswer.value = ''
    spellingChecked.value = false
    spellingCorrect.value = false

    // Check for celebrations
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
      // Find first incomplete task index
      const idx = data.tasks.findIndex((t: TaskDto) => !t.completed)
      currentTaskIndex.value = idx >= 0 ? idx : 0
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
  padding: 24px 0 8px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.today-title {
  font-size: 24px;
  font-weight: 800;
}

.today-sub {
  color: #667eea;
  font-weight: 600;
  font-size: 14px;
}

.loading-state {
  text-align: center;
  padding: 60px 0;
  color: #a0aec0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e2e8f0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 12px;
}

.lesson-info {
  margin-bottom: 16px;
}

.lesson-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}

.lesson-desc {
  font-size: 13px;
  color: #718096;
  margin-bottom: 12px;
}

.lesson-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-text {
  font-size: 13px;
  color: #667eea;
  font-weight: 600;
  white-space: nowrap;
}

.complete-banner {
  text-align: center;
  padding: 40px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.1);
}

.complete-icon {
  font-size: 56px;
  margin-bottom: 12px;
}

.task-counter {
  text-align: center;
  font-size: 13px;
  color: #a0aec0;
  margin-bottom: 8px;
}

.task-card {
  margin-bottom: 16px;
}

// Vocab Card
.vocab-card {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  cursor: pointer;
  transition: all 0.4s;
}

.vocab-front, .vocab-back {
  text-align: center;
}

.vocab-type-badge, .quiz-type-badge, .spelling-type-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 20px;
}

.vocab-type-badge { background: #ebf8ff; color: #3182ce; }
.quiz-type-badge { background: #fefcbf; color: #d69e2e; }
.spelling-type-badge { background: #f0fff4; color: #38a169; }

.vocab-word {
  font-size: 28px;
  font-weight: 800;
  color: #2d3748;
  margin-bottom: 16px;
}

.vocab-hint {
  color: #a0aec0;
  font-size: 14px;
}

.vocab-answer {
  font-size: 24px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 12px;
}

.vocab-explain {
  color: #718096;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.6;
}

// Quiz
.quiz-question {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.5;
  margin-bottom: 20px;
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
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  font-size: 15px;
  transition: all 0.2s;
  text-align: left;

  &:hover:not(:disabled) {
    border-color: #667eea;
    background: #f7f8fc;
  }

  &.correct {
    border-color: #48bb78;
    background: #f0fff4;
  }

  &.wrong {
    border-color: #fc8181;
    background: #fff5f5;
    animation: shake 0.4s;
  }
}

.option-letter {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #edf2f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;

  .correct & { background: #c6f6d5; }
  .wrong & { background: #fed7d7; }
}

.option-text {
  flex: 1;
}

.option-mark {
  font-size: 18px;
}

.quiz-explain {
  margin-top: 16px;
  text-align: center;
}

.explain-text {
  background: #fefcbf;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  color: #744210;
  margin-bottom: 16px;
  line-height: 1.5;
}

// Spelling
.spelling-question {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  line-height: 1.5;
}

.spelling-input-row {
  display: flex;
  gap: 10px;
}

.spelling-input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.2s;

  &:focus {
    border-color: #667eea;
  }
}

.spelling-result {
  margin-top: 16px;
  text-align: center;
}

.spelling-correct {
  font-size: 24px;
  color: #48bb78;
  margin-bottom: 12px;
}

.spelling-wrong {
  font-size: 16px;
  color: #e53e3e;
  margin-bottom: 12px;

  strong {
    color: #48bb78;
  }
}

// Celebration overlay
.celebration-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

.celebration-modal {
  background: white;
  border-radius: 24px;
  padding: 32px 24px;
  text-align: center;
  max-width: 340px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);

  h2 {
    margin-top: 8px;
    font-size: 22px;
  }

  p {
    color: #718096;
    margin: 8px 0 20px;
    font-size: 14px;
  }
}

.celebration-confetti {
  font-size: 56px;
}

.celebration-stats {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.celebrate-stat {
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 14px;
  background: #ebf8ff;
  color: #3182ce;

  &.coins { background: #fefcbf; color: #d69e2e; }
  &.streak { background: #fed7d7; color: #e53e3e; }
}

.new-achievements {
  margin: 12px 0;
  padding: 12px;
  background: #f7fafc;
  border-radius: 12px;

  h4 { margin-bottom: 8px; }
}

.achievement-item {
  font-size: 14px;
  padding: 4px 0;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  75% { transform: translateX(8px); }
}
</style>
