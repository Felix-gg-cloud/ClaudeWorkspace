<template>
  <div class="error-fix-card dark-panel">
    <div class="card-header">
      <span class="badge badge-error">纠错改正</span>
      <span class="cefr-tag" v-if="task.cefrLevel">{{ task.cefrLevel }}</span>
    </div>

    <div class="error-prompt">
      <span class="error-label">🔍 找出下面句子中的错误：</span>
    </div>

    <div class="sentence-box">
      <p class="sentence-text">{{ task.promptEn }}</p>
    </div>

    <p v-if="task.promptZhHint" class="zh-hint">{{ task.promptZhHint }}</p>

    <!-- TTS -->
    <button v-if="task.tts?.enabled" class="play-btn" @click="playTts" :class="{ playing }">
      <span>{{ playing ? '🔊' : '🔈' }}</span>
      <span>播放</span>
    </button>

    <!-- 选项：选择正确的句子 -->
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
        <span v-if="answered && opt.key === correctKey" class="opt-icon">✅</span>
        <span v-if="answered && selected === idx && opt.key !== correctKey" class="opt-icon">❌</span>
      </button>
    </div>

    <div v-if="answered" class="explain-area">
      <p v-if="task.explanationZh" class="explain">💡 {{ task.explanationZh }}</p>
      <p v-if="task.explanationEn" class="explain-en">{{ task.explanationEn }}</p>
    </div>
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

.error-fix-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.badge-error {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.25);
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

.error-prompt {
  margin-bottom: 12px;
}

.error-label {
  font-size: 14px;
  color: $text-secondary;
  font-weight: 600;
}

.sentence-box {
  padding: 16px 20px;
  background: rgba(239, 68, 68, 0.05);
  border: 1px dashed rgba(239, 68, 68, 0.3);
  border-radius: $radius-lg;
  margin-bottom: 12px;
}

.sentence-text {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.6;
  text-decoration: underline wavy rgba(239, 68, 68, 0.4);
  text-underline-offset: 4px;
}

.zh-hint {
  font-size: 13px;
  color: $text-muted;
  margin-bottom: 12px;
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

.opt-icon { font-size: 16px; flex-shrink: 0; }

.explain-area {
  margin-top: 16px;
  padding: 12px;
  background: rgba($gold, 0.04);
  border-radius: $radius-md;
}

.explain {
  font-size: 13px;
  color: $text-secondary;
}

.explain-en {
  font-size: 12px;
  color: $text-muted;
  margin-top: 4px;
}
</style>
