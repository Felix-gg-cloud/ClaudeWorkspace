<template>
  <div class="dialogue-card dark-panel">
    <div class="card-header">
      <span class="badge badge-dialogue">对话复习</span>
    </div>

    <h3 v-if="task.promptEn" class="question">{{ task.promptEn }}</h3>

    <!-- Chat bubbles from dialogue.lines -->
    <div class="chat-area">
      <div
        v-for="(line, idx) in dialogueLines"
        :key="idx"
        class="chat-bubble"
        :class="idx % 2 === 0 ? 'bubble-left' : 'bubble-right'"
        :style="{ animationDelay: idx * 0.3 + 's' }"
      >
        <span class="bubble-text">{{ line.en }}</span>
        <span class="bubble-zh">{{ line.zh }}</span>
      </div>
    </div>

    <!-- Follow-up question (MCQ) -->
    <div v-if="followUp && !answered" class="followup-area">
      <p class="followup-prompt">{{ followUp.promptEn }}</p>
      <p v-if="followUp.promptZhHint" class="followup-hint">{{ followUp.promptZhHint }}</p>
      <div class="options">
        <button
          v-for="(opt, idx) in followUp.options"
          :key="idx"
          class="option-btn"
          :class="{
            'option-correct': answered && opt.key === followUpCorrectKey,
            'option-wrong': answered && selected === idx && opt.key !== followUpCorrectKey,
          }"
          :disabled="answered"
          @click="select(idx, opt.key === followUpCorrectKey)"
        >
          {{ opt.textEn }} {{ opt.textZh }}
        </button>
      </div>
    </div>

    <!-- Simple acknowledge if no follow-up questions -->
    <button
      v-if="!followUp && !answered"
      class="btn-gold ack-btn"
      @click="acknowledge"
    >
      ✅ 我理解了
    </button>

    <div v-if="answered" class="result-msg" :class="correct ? 'msg-ok' : 'msg-fail'">
      {{ correct ? '✅ 回答正确！' : '❌ 再想想' }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Task } from '@/types'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()

const answered = ref(false)
const selected = ref(-1)
const correct = ref(false)

const dialogueLines = computed(() => props.task.dialogue?.lines ?? [])
const followUp = computed(() => props.task.followUpQuestions?.[0] ?? null)
const followUpCorrectKey = computed(() => (followUp.value?.answer as any)?.correctOptionKey ?? '')

function select(idx: number, isCorrect: boolean) {
  if (answered.value) return
  selected.value = idx
  correct.value = isCorrect
  answered.value = true
  emit('answer', isCorrect)
}

function acknowledge() {
  correct.value = true
  answered.value = true
  emit('answer', true)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.dialogue-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header { margin-bottom: 16px; }

.badge-dialogue {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(6, 182, 212, 0.15);
  color: $ice-blue;
  border: 1px solid rgba(6, 182, 212, 0.2);
}

.question {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 20px;
}

.chat-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.chat-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  animation: slide-up 0.4s ease-out both;

  &.bubble-left {
    align-self: flex-start;
    background: rgba($mana-blue, 0.1);
    border: 1px solid rgba($mana-blue, 0.15);
    border-bottom-left-radius: 4px;
    color: $text-primary;
  }

  &.bubble-right {
    align-self: flex-end;
    background: rgba($magic-purple, 0.1);
    border: 1px solid rgba($magic-purple, 0.15);
    border-bottom-right-radius: 4px;
    color: $text-primary;
  }
}

.bubble-role {
  font-size: 11px;
  font-weight: 700;
  margin-right: 6px;
  opacity: 0.6;
}

.bubble-text {
  line-height: 1.5;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.option-btn {
  padding: 12px 16px;
  border: 1px solid $border-dim;
  border-radius: $radius-lg;
  background: rgba(255, 255, 255, 0.02);
  color: $text-primary;
  font-size: 14px;
  cursor: pointer;
  text-align: left;
  width: 100%;
  transition: all $transition-normal;

  &:hover:not(:disabled) {
    border-color: rgba($ice-blue, 0.3);
    background: rgba($ice-blue, 0.04);
  }
}

.option-correct {
  border-color: $life-green !important;
  background: rgba($life-green, 0.08) !important;
  animation: scale-in 0.3s $ease-bounce;
}

.option-wrong {
  border-color: $hp-red !important;
  background: rgba($hp-red, 0.08) !important;
  animation: shake 0.4s;
}

.ack-btn {
  width: 100%;
  padding: 14px;
}

.explain {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba($gold, 0.06);
  border: 1px solid rgba($gold, 0.12);
  border-radius: $radius-md;
  font-size: 13px;
  color: $text-secondary;
  line-height: 1.6;
  animation: slide-up 0.3s ease-out;
}
</style>
