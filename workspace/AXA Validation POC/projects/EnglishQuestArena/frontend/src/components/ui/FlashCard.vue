<template>
  <div class="flash-card dark-panel" @click="flip">
    <div class="card-header">
      <span class="badge badge-flash">单词卡</span>
      <span class="flip-hint">{{ flipped ? '点击翻回' : '点击翻面' }}</span>
    </div>

    <div class="flip-container" :class="{ flipped }">
      <div class="flip-inner">
        <!-- Front -->
        <div class="flip-face flip-front">
          <div class="word-display">{{ task.promptEn }}</div>
          <div class="tap-icon">👆</div>
        </div>
        <!-- Back -->
        <div class="flip-face flip-back">
          <div class="answer-text">{{ task.promptZhHint }}</div>
          <p v-if="task.explanationZh" class="explain-text">{{ task.explanationZh }}</p>
        </div>
      </div>
    </div>

    <div v-if="flipped && !answered" class="rate-buttons">
      <button class="btn-danger" @click.stop="rate(false)">😕 不认识</button>
      <button class="btn-gold" @click.stop="rate(true)">😊 认识</button>
    </div>

    <div v-if="answered" class="result-msg" :class="correct ? 'msg-ok' : 'msg-fail'">
      {{ correct ? '✅ 很好，继续保持！' : '📝 已标记复习' }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Task } from '@/types'
import { useSound } from '@/composables/useSound'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()
const sound = useSound()

const flipped = ref(false)
const answered = ref(false)
const correct = ref(false)

function flip() {
  if (!answered.value) {
    flipped.value = !flipped.value
    sound.flip()
  }
}

function rate(isCorrect: boolean) {
  answered.value = true
  correct.value = isCorrect
  emit('answer', isCorrect)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.flash-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.badge-flash {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(124, 58, 237, 0.15);
  color: $magic-purple;
  border: 1px solid rgba(124, 58, 237, 0.2);
}

.flip-hint {
  font-size: 12px;
  color: $text-muted;
}

.flip-container {
  perspective: 800px;
  cursor: pointer;
  margin-bottom: 20px;
}

.flip-inner {
  position: relative;
  width: 100%;
  min-height: 180px;
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.flipped .flip-inner {
  transform: rotateY(180deg);
}

.flip-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: $radius-lg;
  padding: 24px;
}

.flip-front {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.08), rgba(245, 158, 11, 0.05));
  border: 1px solid rgba(124, 58, 237, 0.15);
}

.flip-back {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(59, 130, 246, 0.05));
  border: 1px solid rgba(16, 185, 129, 0.15);
  transform: rotateY(180deg);
}

.word-display {
  font-size: 32px;
  font-weight: 800;
  color: $text-primary;
  text-align: center;
}

.tap-icon {
  font-size: 24px;
  margin-top: 12px;
  animation: float 2s ease-in-out infinite;
  opacity: 0.5;
}

.answer-text {
  font-size: 24px;
  font-weight: 700;
  color: $life-green;
  text-align: center;
}

.explain-text {
  margin-top: 10px;
  font-size: 14px;
  color: $text-secondary;
  text-align: center;
}

.rate-buttons {
  display: flex;
  gap: 12px;

  button {
    flex: 1;
    padding: 12px;
    font-size: 14px;
  }
}

.result-msg {
  text-align: center;
  padding: 12px;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 600;
  animation: scale-in 0.3s $ease-bounce;

  &.msg-ok {
    background: rgba($life-green, 0.1);
    color: $life-green;
  }

  &.msg-fail {
    background: rgba($fire-orange, 0.1);
    color: $fire-orange;
  }
}
</style>
