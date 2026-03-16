<template>
  <MainLayout>
    <div class="quest-page">
      <!-- Dungeon corridor progress -->
      <section class="corridor">
        <div class="corridor-track">
          <div
            v-for="(task, idx) in tasks"
            :key="task.taskId"
            class="corridor-node"
            :class="{
              'corridor-node--done': task._done,
              'corridor-node--active': idx === currentIdx,
              'corridor-node--locked': idx > currentIdx && !task._done
            }"
            @click="jumpTo(idx)"
          >
            <div class="node-pip">
              <span v-if="task._done">✓</span>
              <span v-else>{{ idx + 1 }}</span>
            </div>
          </div>
          <div class="corridor-line"></div>
          <!-- Character marker -->
          <div class="corridor-char" :style="{ left: charPos }">🧙</div>
        </div>
        <div class="corridor-info">
          <span class="text-gold">{{ doneCount }}</span> / {{ tasks.length }} 完成
        </div>
      </section>

      <!-- Combo bar -->
      <div v-if="combo > 0" class="combo-bar dark-panel">
        <span class="combo-label">🔥 连击</span>
        <span class="combo-count" :class="{ 'combo-hot': combo >= 5 }">x{{ combo }}</span>
        <div class="combo-fill" :style="{ width: Math.min(combo / 10 * 100, 100) + '%' }"></div>
      </div>

      <!-- XP float animation -->
      <transition-group name="xp-anim">
        <div v-for="f in xpFloats" :key="f.id" class="xp-float" :style="{ left: f.x + 'px' }">
          +{{ f.amount }} XP
        </div>
      </transition-group>

      <!-- Screen flash on wrong -->
      <div v-if="flashRed" class="screen-flash-red"></div>

      <!-- Current task card -->
      <section v-if="currentTask" class="task-area">
        <transition name="page-fade" mode="out-in">
          <component
            :is="taskComponent"
            :key="currentTask.code"
            :task="currentTask"
            @answer="onAnswer"
          />
        </transition>
      </section>

      <!-- All done -->
      <section v-else-if="allDone" class="victory-area">
        <div class="victory-card dark-panel">
          <div class="victory-icon">🎉</div>
          <h2 class="text-gold">任务完成!</h2>
          <p>今日主线全部通过</p>
          <div class="victory-stats">
            <span>✅ {{ doneCount }} 题</span>
            <span>🔥 最高连击 x{{ maxCombo }}</span>
          </div>
          <button class="btn-gold" @click="$router.push('/dashboard')">
            返回大厅
          </button>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useChapterStore } from '@/stores/chapter'
import { useSrsStore } from '@/stores/srs'
import { useMistakeStore } from '@/stores/mistakes'
import { useAchievementStore } from '@/stores/achievements'
import { useDailyGoalStore } from '@/stores/dailyGoal'
import { useSound } from '@/composables/useSound'
import { resolveTaskComponent } from '@/composables/useTaskCard'
import { grantReward } from '@/composables/useReward'
import type { Task } from '@/types'

interface TaskExt extends Task { _done: boolean }

const chapterStore = useChapterStore()
const srsStore = useSrsStore()
const mistakeStore = useMistakeStore()
const achievementStore = useAchievementStore()
const dailyStore = useDailyGoalStore()
const sound = useSound()

const tasks = ref<TaskExt[]>([])
const currentIdx = ref(0)
const combo = ref(0)
const maxCombo = ref(0)
const flashRed = ref(false)
const dataLoading = ref(true)

onMounted(async () => {
  const code = chapterStore.currentChapterCode
  const day = chapterStore.currentQuestDayIndex
  await chapterStore.loadQuestTasks(code, day)
  tasks.value = chapterStore.currentQuestTasks.map(t => ({ ...t, _done: false }))
  dataLoading.value = false
})
const currentIdx = ref(0)
const combo = ref(0)
const maxCombo = ref(0)
const flashRed = ref(false)

const xpFloats = ref<{ id: number; amount: number; x: number }[]>([])
let floatId = 0

const currentTask = computed(() => {
  if (allDone.value) return null
  return tasks.value[currentIdx.value] ?? null
})

const doneCount = computed(() => tasks.value.filter(t => t._done).length)
const allDone = computed(() => tasks.value.every(t => t._done))
const charPos = computed(() => {
  const total = tasks.value.length
  if (total <= 1) return '0%'
  return (currentIdx.value / (total - 1)) * 100 + '%'
})

const taskComponent = computed(() => resolveTaskComponent(currentTask.value))

function jumpTo(idx: number) {
  if (idx <= currentIdx.value || tasks.value[idx]._done) {
    currentIdx.value = idx
  }
}

function onAnswer(correct: boolean) {
  const task = tasks.value[currentIdx.value]
  const wordCode = task.links?.contentItemCodes?.[0] ?? task.code

  if (correct) {
    combo.value++
    if (combo.value > maxCombo.value) maxCombo.value = combo.value

    combo.value >= 5 ? sound.combo() : sound.correct()

    const reward = grantReward(true, combo.value, 'quest')

    floatId++
    xpFloats.value.push({ id: floatId, amount: reward.xpEarned, x: 200 + Math.random() * 200 })
    setTimeout(() => {
      xpFloats.value = xpFloats.value.filter(f => f.id !== floatId)
    }, 1200)

    // SRS: 答对质量为 4
    srsStore.reviewCard(wordCode, 4)
    // 每日目标
    dailyStore.addXp(reward.xpEarned)
    dailyStore.addTaskCompleted()
    // 成就：连击 & 任务数
    achievementStore.updateProgress('perfect_combo', maxCombo.value)
    achievementStore.updateProgress('tasks_completed', doneCount.value + 1)

    // Mark done & advance
    task._done = true
    setTimeout(() => {
      if (!allDone.value) {
        const next = tasks.value.findIndex((t, i) => i > currentIdx.value && !t._done)
        if (next >= 0) currentIdx.value = next
      }
    }, 600)
  } else {
    combo.value = 0
    sound.wrong()
    flashRed.value = true
    setTimeout(() => { flashRed.value = false }, 300)

    // SRS: 答错质量为 1
    srsStore.reviewCard(wordCode, 1)
    // 错题收集
    mistakeStore.addMistake({
      taskCode: task.code,
      taskType: task.type,
      wordCode,
      promptEn: task.promptEn,
      promptZhHint: task.promptZhHint,
      userAnswer: '',
      correctAnswer: String(task.answer?.correctOptionKey ?? ''),
    })
  }
}

watch(allDone, v => {
  if (v) {
    sound.victory()
    chapterStore.completeQuestDay(chapterStore.currentQuestDayIndex)
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.quest-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 20px 60px;
  position: relative;
}

// Corridor
.corridor {
  margin-bottom: 20px;
}

.corridor-track {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: 0 16px;
  height: 48px;
  align-items: center;
}

.corridor-line {
  position: absolute;
  top: 50%;
  left: 16px;
  right: 16px;
  height: 3px;
  background: $border-dim;
  transform: translateY(-50%);
  z-index: 0;
}

.corridor-node {
  position: relative;
  z-index: 1;
  cursor: pointer;
}

.node-pip {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  background: $bg-dark;
  border: 2px solid $border-dim;
  color: $text-muted;
  transition: all $transition-normal;
}

.corridor-node--done .node-pip {
  background: $life-green;
  border-color: $life-green;
  color: #fff;
}

.corridor-node--active .node-pip {
  background: $gold;
  border-color: $gold;
  color: $bg-abyss;
  box-shadow: $shadow-glow-gold;
  animation: pulse-glow 1.5s infinite;
}

.corridor-node--locked { opacity: 0.4; pointer-events: none; }

.corridor-char {
  position: absolute;
  top: -18px;
  font-size: 22px;
  transform: translateX(-50%);
  transition: left 0.5s $ease-bounce;
  filter: drop-shadow(0 0 6px rgba(245, 158, 11, 0.5));
  z-index: 2;
}

.corridor-info {
  text-align: center;
  font-size: 13px;
  color: $text-muted;
  margin-top: 8px;
}

// Combo bar
.combo-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  margin-bottom: 16px;
  position: relative;
  overflow: hidden;
}

.combo-label { font-size: 13px; color: $text-secondary; }
.combo-count {
  font-size: 18px;
  font-weight: 900;
  color: $fire-orange;
  &.combo-hot { color: $hp-red; animation: combo-pop 0.3s ease-out; }
}

.combo-fill {
  position: absolute;
  left: 0; bottom: 0;
  height: 3px;
  background: linear-gradient(90deg, $fire-orange, $hp-red);
  transition: width 0.3s;
}

// XP float
.xp-float {
  position: fixed;
  top: 50%;
  font-size: 18px;
  font-weight: 800;
  color: $gold;
  pointer-events: none;
  animation: xp-float 1.2s ease-out forwards;
  z-index: 100;
}

.xp-anim-enter-active { animation: xp-float 1.2s ease-out forwards; }

// Screen flash
.screen-flash-red {
  position: fixed;
  inset: 0;
  background: rgba(220, 38, 38, 0.15);
  pointer-events: none;
  z-index: 200;
  animation: screen-flash-red 0.3s ease-out forwards;
}

// Task area
.task-area {
  min-height: 300px;
}

// Victory
.victory-area {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.victory-card {
  text-align: center;
  padding: 40px;
  animation: scale-in 0.5s $ease-bounce;
}

.victory-icon {
  font-size: 56px;
  margin-bottom: 12px;
  animation: float 2s ease-in-out infinite;
}

.victory-card h2 {
  font-size: 28px;
  margin-bottom: 8px;
}

.victory-card p {
  color: $text-secondary;
  margin-bottom: 20px;
}

.victory-stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 24px;
}
</style>
