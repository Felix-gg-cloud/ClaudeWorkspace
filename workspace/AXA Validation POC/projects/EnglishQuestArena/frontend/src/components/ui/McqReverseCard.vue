<template>
  <div class="mcq-reverse-card dark-panel">
    <div class="card-header">
      <span class="badge badge-reverse">看义选词</span>
      <span class="cefr-tag" v-if="task.cefrLevel">{{ task.cefrLevel }}</span>
    </div>

    <h3 class="question">{{ task.promptZhHint }}</h3>
    <p class="sub-hint">选择对应的英文单词</p>

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

    <!-- 答后播放正确发音 -->
    <div v-if="answered" class="after-answer">
      <button class="play-btn-sm" @click="playTts">🔈 听发音</button>
      <p v-if="task.explanationZh" class="explain">💡 {{ task.explanationZh }}</p>
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

function select(idx: number, isCorrect: boolean) {
  if (answered.value) return
  selected.value = idx
  answered.value = true
  emit('answer', isCorrect)
}

function playTts() {
  const text = props.task.tts?.ttsTextEn || props.task.promptEn
  const utter = new SpeechSynthesisUtterance(text)
  utter.lang = 'en-US'
  utter.rate = 0.85
  speechSynthesis.cancel()
  speechSynthesis.speak(utter)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.mcq-reverse-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.badge-reverse {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(124, 58, 237, 0.15);
  color: $magic-purple;
  border: 1px solid rgba(124, 58, 237, 0.2);
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

.question {
  font-size: 22px;
  font-weight: 800;
  color: $gold-light;
  margin-bottom: 8px;
  line-height: 1.5;
}

.sub-hint {
  font-size: 13px;
  color: $text-muted;
  margin-bottom: 20px;
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
    border-color: rgba($magic-purple, 0.4);
    background: rgba($magic-purple, 0.06);
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
.opt-icon { font-size: 18px; }

.option-correct {
  border-color: $life-green !important;
  background: rgba($life-green, 0.1) !important;
}

.option-wrong {
  border-color: $hp-red !important;
  background: rgba($hp-red, 0.08) !important;
}

.after-answer {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.play-btn-sm {
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
  align-self: flex-start;
  transition: all 0.2s;

  &:hover {
    background: rgba($mana-blue, 0.18);
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
