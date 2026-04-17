<template>
  <div class="mcq-card dark-panel">
    <div class="card-header">
      <span class="badge badge-mcq">选择题</span>
      <span class="q-num">{{ task.type }}</span>
    </div>

    <h3 class="question">{{ task.promptEn }}</h3>
    <p v-if="task.promptZhHint" class="zh-hint">{{ task.promptZhHint }}</p>

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

function select(idx: number, isCorrect: boolean) {
  if (answered.value) return
  selected.value = idx
  answered.value = true
  emit('answer', isCorrect)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.mcq-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
}

.badge-mcq {
  background: rgba(59, 130, 246, 0.15);
  color: $mana-blue;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.q-num {
  font-size: 11px;
  color: $text-muted;
}

.question {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 20px;
  line-height: 1.5;
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
}

.opt-letter {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: $bg-dark;
  border: 1px solid $border-dim;
  flex-shrink: 0;
}

.opt-text { flex: 1; }
.opt-icon { font-size: 18px; }

.option-correct {
  border-color: $life-green !important;
  background: rgba($life-green, 0.08) !important;
  animation: scale-in 0.3s $ease-bounce;

  .opt-letter {
    background: $life-green;
    border-color: $life-green;
    color: #fff;
  }
}

.option-wrong {
  border-color: $hp-red !important;
  background: rgba($hp-red, 0.08) !important;
  animation: shake 0.4s;

  .opt-letter {
    background: $hp-red;
    border-color: $hp-red;
    color: #fff;
  }
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
