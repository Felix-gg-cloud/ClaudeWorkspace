<template>
  <div class="listen-pick-card dark-panel" @click="focusCard">
    <div class="card-header">
      <span class="badge badge-listen">听音选词</span>
      <span class="cefr-tag" v-if="task.cefrLevel">{{ task.cefrLevel }}</span>
    </div>

    <!-- 核心交互：大播放按钮 -->
    <div class="listen-hero">
      <button class="play-circle" :class="{ playing }" @click.stop="playTts">
        <span class="play-icon-lg">{{ playing ? '🔊' : '🔈' }}</span>
      </button>
      <p class="listen-tip">点击播放，听单词发音后选择正确答案</p>
      <button v-if="canReplay" class="replay-btn" @click.stop="playTts">🔁 再听一次</button>
    </div>

    <!-- 不显示 promptEn，这是纯听力题 -->

    <!-- 选项 -->
    <div class="options">
      <button
        v-for="(opt, idx) in task.options"
        :key="idx"
        class="option-btn"
        :class="{
          'option-correct': answered && opt.key === correctKey,
          'option-wrong': answered && selected === idx && opt.key !== correctKey,
        }"
        :disabled="answered"
        @click="select(idx, opt.key === correctKey)"
      >
        <span class="opt-letter">{{ opt.key }}</span>
        <span class="opt-text">{{ opt.textEn }}</span>
        <span class="opt-zh">{{ opt.textZh }}</span>
        <span v-if="answered && opt.key === correctKey" class="opt-icon">✅</span>
        <span v-if="answered && selected === idx && opt.key !== correctKey" class="opt-icon">❌</span>
      </button>
    </div>

    <!-- 答后揭示 -->
    <div v-if="answered" class="reveal-area">
      <p class="reveal-word">
        正确单词：<strong class="text-gold">{{ correctWord }}</strong>
      </p>
      <p v-if="task.explanationZh" class="explain">💡 {{ task.explanationZh }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Task } from '@/types'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()

const correctKey = computed(() => (props.task.answer as any)?.correctOptionKey ?? '')
const correctWord = computed(() => {
  const opt = props.task.options?.find(o => o.key === correctKey.value)
  return opt?.textEn ?? ''
})

const answered = ref(false)
const selected = ref(-1)
const playing = ref(false)
const canReplay = ref(false)

const ttsText = computed(() => props.task.tts?.ttsTextEn || props.task.promptEn)

function playTts() {
  if (playing.value) return
  const utter = new SpeechSynthesisUtterance(ttsText.value)
  utter.lang = 'en-US'
  utter.rate = 0.85
  playing.value = true
  utter.onend = () => {
    playing.value = false
    canReplay.value = true
  }
  utter.onerror = () => { playing.value = false }
  speechSynthesis.cancel()
  speechSynthesis.speak(utter)
}

function select(idx: number, isCorrect: boolean) {
  if (answered.value) return
  selected.value = idx
  answered.value = true
  emit('answer', isCorrect)
}

function focusCard() { /* keep card interactive */ }

// 自动播放一次
onMounted(() => {
  setTimeout(playTts, 400)
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.listen-pick-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.badge-listen {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(6, 182, 212, 0.15);
  color: $ice-blue;
  border: 1px solid rgba(6, 182, 212, 0.2);
}

.cefr-tag {
  font-size: 10px;
  font-weight: 700;
  color: $text-muted;
  background: rgba(255, 255, 255, 0.05);
  padding: 2px 8px;
  border-radius: 8px;
  border: 1px solid $border-dim;
}

.listen-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
  gap: 12px;
}

.play-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba($ice-blue, 0.15), rgba($mana-blue, 0.1));
  border: 2px solid rgba($ice-blue, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: $ice-blue;
    box-shadow: 0 0 24px rgba($ice-blue, 0.25);
    transform: scale(1.05);
  }

  &.playing {
    animation: pulse-glow 0.8s infinite;
    border-color: $ice-blue;
    box-shadow: 0 0 24px rgba($ice-blue, 0.3);
  }
}

.play-icon-lg { font-size: 36px; }

.listen-tip {
  font-size: 13px;
  color: $text-muted;
}

.replay-btn {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid $border-dim;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($ice-blue, 0.08);
    border-color: rgba($ice-blue, 0.3);
    color: $ice-blue;
  }
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid $border-dim;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.02);
  color: $text-primary;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 15px;
  text-align: left;

  &:hover:not(:disabled) {
    border-color: rgba($ice-blue, 0.4);
    background: rgba($ice-blue, 0.06);
  }

  &:disabled { cursor: default; }
}

.opt-letter {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.opt-text { flex: 1; font-weight: 600; }
.opt-zh { font-size: 13px; color: $text-muted; }
.opt-icon { font-size: 18px; }

.option-correct {
  border-color: $life-green !important;
  background: rgba($life-green, 0.1) !important;
}

.option-wrong {
  border-color: $hp-red !important;
  background: rgba($hp-red, 0.08) !important;
}

.reveal-area {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reveal-word {
  font-size: 15px;
  color: $text-secondary;

  strong {
    font-size: 18px;
    font-weight: 800;
  }
}

.explain {
  font-size: 13px;
  color: $text-secondary;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-sm;
  border-left: 3px solid $gold;
}
</style>
