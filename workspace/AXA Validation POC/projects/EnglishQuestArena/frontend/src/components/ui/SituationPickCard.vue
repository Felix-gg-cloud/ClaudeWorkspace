<template>
  <div class="situation-pick-card dark-panel">
    <div class="card-header">
      <span class="badge badge-situation">情景选择</span>
      <span class="cefr-tag" v-if="task.cefrLevel">{{ task.cefrLevel }}</span>
    </div>

    <!-- 情景描述 -->
    <div class="scenario-box">
      <span class="scenario-icon">🎭</span>
      <p class="scenario-text">{{ task.promptZhHint }}</p>
    </div>

    <h3 class="question">{{ task.promptEn }}</h3>

    <!-- TTS 播放 -->
    <button v-if="task.tts?.enabled" class="play-btn" @click="playTts" :class="{ playing }">
      <span>{{ playing ? '🔊' : '🔈' }}</span>
      <span>播放发音</span>
    </button>

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

    <p v-if="answered && task.explanationZh" class="explain">
      💡 {{ task.explanationZh }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Task } from '@/types'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()

const correctKey = computed(() => (props.task.answer as any)?.correctOptionKey ?? '')
const answered = ref(false)
const selected = ref(-1)
const playing = ref(false)

function select(idx: number, isCorrect: boolean) {
  if (answered.value) return
  selected.value = idx
  answered.value = true
  emit('answer', isCorrect)
}

function playTts() {
  if (playing.value) return
  const text = props.task.tts?.ttsTextEn || props.task.promptEn
  const utter = new SpeechSynthesisUtterance(text)
  utter.lang = 'en-US'
  utter.rate = 0.85
  playing.value = true
  utter.onend = () => { playing.value = false }
  utter.onerror = () => { playing.value = false }
  speechSynthesis.cancel()
  speechSynthesis.speak(utter)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.situation-pick-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.badge-situation {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(251, 146, 60, 0.15);
  color: #fb923c;
  border: 1px solid rgba(251, 146, 60, 0.25);
}

.cefr-tag {
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 700;
  background: rgba($gold, 0.1);
  color: $gold;
  border: 1px solid rgba($gold, 0.2);
}

.scenario-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  background: rgba(251, 146, 60, 0.06);
  border: 1px solid rgba(251, 146, 60, 0.15);
  border-radius: $radius-lg;
  margin-bottom: 16px;
}

.scenario-icon { font-size: 22px; flex-shrink: 0; }

.scenario-text {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.5;
}

.question {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 16px;
  line-height: 1.5;
}

.play-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba($mana-blue, 0.1);
  border: 1px solid rgba($mana-blue, 0.25);
  color: $mana-blue;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.2s;

  &:hover { background: rgba($mana-blue, 0.18); }
  &.playing { animation: pulse-glow 0.6s infinite; }
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
  border-radius: $radius-lg;
  background: rgba(255, 255, 255, 0.02);
  color: $text-primary;
  font-size: 15px;
  cursor: pointer;
  transition: all $transition-normal;
  text-align: left;
  width: 100%;

  &:hover:not(:disabled) {
    border-color: rgba($gold, 0.3);
    background: rgba($gold, 0.04);
  }

  &.option-correct {
    border-color: $life-green;
    background: rgba($life-green, 0.08);
  }

  &.option-wrong {
    border-color: $hp-red;
    background: rgba($hp-red, 0.08);
  }
}

.opt-letter {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: $text-muted;
  flex-shrink: 0;
}

.opt-text { flex: 1; }

.opt-zh {
  font-size: 12px;
  color: $text-muted;
}

.opt-icon { font-size: 16px; flex-shrink: 0; }

.explain {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 16px;
  padding: 12px;
  background: rgba($gold, 0.04);
  border-radius: $radius-md;
}
</style>
