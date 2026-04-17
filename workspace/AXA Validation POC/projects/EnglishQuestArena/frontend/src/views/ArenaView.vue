<template>
  <MainLayout>
    <div class="arena-page">
      <h2 class="page-title">🏟️ SRS 副本</h2>
      <p class="page-desc">间隔复习 · 强化记忆 · 征服遗忘</p>

      <!-- Session state -->
      <div v-if="!started && !finished" class="start-area">
        <div class="start-card dark-panel">
          <div class="start-icon">⚔️</div>
          <h3>今日副本</h3>
          <p class="start-info">{{ pendingCount }} 个复习项目待挑战</p>
          <div class="start-stats">
            <span class="stat"><span class="text-gold">📖</span> 单词 {{ wordCount }}</span>
            <span class="stat"><span class="text-fire">📝</span> 句型 {{ patternCount }}</span>
          </div>
          <button class="btn-gold" @click="startSession">⚔️ 进入副本</button>
        </div>
      </div>

      <!-- In-session -->
      <div v-if="started && !finished" class="session-area">
        <!-- Progress bar -->
        <div class="session-progress dark-panel">
          <div class="progress-info">
            <span>第 {{ currentIdx + 1 }} / {{ sessionTasks.length }} 题</span>
            <span class="text-gold">🔥 {{ sessionCombo }}x连击</span>
          </div>
          <div class="hp-bar hp-bar--blue">
            <div class="hp-bar__fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
        </div>

        <!-- Energy combo bar -->
        <div class="energy-bar">
          <div class="energy-segments">
            <div
              v-for="seg in 10"
              :key="seg"
              class="energy-seg"
              :class="{ 'seg-active': seg <= sessionCombo }"
            ></div>
          </div>
          <span v-if="sessionCombo >= 5" class="energy-label text-fire">⚡ 超载</span>
        </div>

        <!-- Task card (reuse same components as Quest) -->
        <transition name="page-fade" mode="out-in">
          <component
            :is="taskComponent"
            :key="currentItem.code"
            :task="currentItem"
            @answer="onAnswer"
          />
        </transition>
      </div>

      <!-- Finished -->
      <div v-if="finished" class="finish-area">
        <div class="finish-card dark-panel">
          <div class="finish-icon">🏆</div>
          <h2 class="text-gold">副本通关</h2>
          <div class="finish-stats">
            <div class="fstat">
              <span class="fstat-val text-gold">{{ correctCount }}</span>
              <span class="fstat-label">正确</span>
            </div>
            <div class="fstat">
              <span class="fstat-val" style="color: #dc2626">{{ wrongCount }}</span>
              <span class="fstat-label">错误</span>
            </div>
            <div class="fstat">
              <span class="fstat-val text-fire">{{ maxCombo }}</span>
              <span class="fstat-label">最高连击</span>
            </div>
          </div>
          <button class="btn-gold" @click="$router.push('/dashboard')">返回大厅</button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useChapterStore } from '@/stores/chapter'
import { useSound } from '@/composables/useSound'
import { resolveTaskComponent } from '@/composables/useTaskCard'
import { grantReward } from '@/composables/useReward'
import { mockTodayTasks } from '@/mock/data'
import type { Task, WordItem } from '@/types'

const chapterStore = useChapterStore()
const sound = useSound()

// Generate SRS tasks from all learned words across chapters
function generateArenaTasks(): Task[] {
  const words = chapterStore.allLearnedWords
  if (words.length === 0) return mockTodayTasks // fallback

  const tasks: Task[] = []
  const shuffled = [...words].sort(() => Math.random() - 0.5)

  for (const [i, w] of shuffled.entries()) {
    // Alternate between MCQ and SPELLING
    const others = words.filter(o => o.code !== w.code)
    if (i % 2 === 0 && others.length >= 2) {
      // MCQ: given English word, pick Chinese meaning
      const distractors = others.sort(() => Math.random() - 0.5).slice(0, 2)
      const opts = [
        { key: 'A', textEn: w.en, textZh: w.zh },
        { key: 'B', textEn: distractors[0].en, textZh: distractors[0].zh },
        { key: 'C', textEn: distractors[1].en, textZh: distractors[1].zh },
      ].sort(() => Math.random() - 0.5)
      const correctKey = opts.find(o => o.textZh === w.zh)!.key

      tasks.push({
        code: `ARENA_${w.code}_${i}`,
        lessonCode: '',
        orderIndex: i,
        type: 'MCQ',
        promptEn: `Choose the meaning of "${w.en}"`,
        promptZhHint: `选择 ${w.en} 的中文意思`,
        options: opts,
        answer: { correctOptionKey: correctKey },
        explanationEn: `${w.en} = ${w.zh}`,
        explanationZh: `${w.en} 的意思是"${w.zh}"`,
        xpReward: 5,
        goldReward: 1,
        tts: { enabled: true, ttsTextEn: w.en },
        links: { contentItemCodes: [w.code] },
      })
    } else {
      // SPELLING
      tasks.push({
        code: `ARENA_SP_${w.code}_${i}`,
        lessonCode: '',
        orderIndex: i,
        type: 'SPELLING',
        promptEn: 'Type the word you hear.',
        promptZhHint: `听音拼写：${w.zh}`,
        answer: { acceptedTexts: [w.en] },
        explanationEn: `The word is "${w.en}"`,
        explanationZh: `这个单词是 ${w.en}（${w.zh}）`,
        xpReward: 8,
        goldReward: 1,
        tts: { enabled: true, ttsTextEn: w.en },
        links: { contentItemCodes: [w.code] },
      })
    }
  }
  return tasks
}



// SRS uses same mock tasks shuffled
const sessionTasks = ref<Task[]>([])
const started = ref(false)
const finished = ref(false)
const currentIdx = ref(0)
const sessionCombo = ref(0)
const maxCombo = ref(0)
const correctCount = ref(0)
const wrongCount = ref(0)

const pendingCount = computed(() => chapterStore.allLearnedWords.length || mockTodayTasks.length)
const wordCount = computed(() => chapterStore.allLearnedWords.length)
const patternCount = computed(() => 0)

const currentItem = computed(() => sessionTasks.value[currentIdx.value])
const progressPercent = computed(() => ((currentIdx.value + 1) / sessionTasks.value.length) * 100)

const taskComponent = computed(() => resolveTaskComponent(currentItem.value))

function startSession() {
  sessionTasks.value = generateArenaTasks()
  started.value = true
  sound.click()
}

function onAnswer(isCorrect: boolean) {
  if (isCorrect) {
    correctCount.value++
    sessionCombo.value++
    if (sessionCombo.value > maxCombo.value) maxCombo.value = sessionCombo.value
    sessionCombo.value >= 5 ? sound.combo() : sound.correct()
    grantReward(true, sessionCombo.value, 'arena')
  } else {
    wrongCount.value++
    sessionCombo.value = 0
    sound.wrong()
  }

  setTimeout(() => {
    if (currentIdx.value < sessionTasks.value.length - 1) {
      currentIdx.value++
    } else {
      finished.value = true
      sound.victory()
    }
  }, 800)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.arena-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  text-align: center;
  margin-bottom: 4px;
}

.page-desc {
  text-align: center;
  font-size: 13px;
  color: $text-muted;
  margin-bottom: 28px;
}

// Start
.start-area { display: flex; justify-content: center; }

.start-card {
  text-align: center;
  padding: 40px;
  min-width: 320px;
  animation: scale-in 0.4s ease-out;

  h3 { font-size: 20px; margin: 12px 0 8px; }
}

.start-icon {
  font-size: 48px;
  animation: float 2.5s ease-in-out infinite;
}

.start-info {
  color: $text-secondary;
  font-size: 14px;
  margin-bottom: 16px;
}

.start-stats {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 20px;
  font-size: 13px;
  color: $text-secondary;

  .stat { display: flex; gap: 4px; }
}

// Session
.session-progress {
  padding: 12px 16px;
  margin-bottom: 12px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 8px;
}

// Energy bar
.energy-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.energy-segments {
  display: flex;
  gap: 3px;
  flex: 1;
}

.energy-seg {
  height: 6px;
  flex: 1;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.06);
  transition: all 0.3s;

  &.seg-active {
    background: linear-gradient(90deg, $fire-orange, $hp-red);
    box-shadow: 0 0 6px rgba(249, 115, 22, 0.4);
  }
}

.energy-label {
  font-size: 12px;
  font-weight: 700;
  animation: pulse-glow 1s infinite;
}

// Finished
.finish-area { display: flex; justify-content: center; padding-top: 24px; }

.finish-card {
  text-align: center;
  padding: 40px;
  min-width: 360px;
  animation: scale-in 0.5s $ease-bounce;

  h2 { font-size: 28px; margin: 12px 0 20px; }
}

.finish-icon {
  font-size: 56px;
  animation: float 2s ease-in-out infinite;
}

.finish-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 28px;
}

.fstat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.fstat-val { font-size: 28px; font-weight: 900; }
.fstat-label { font-size: 12px; color: $text-muted; }
</style>
