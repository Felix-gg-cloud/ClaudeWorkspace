<template>
  <div class="word-order-card dark-panel">
    <div class="card-header">
      <span class="badge badge-order">排序题</span>
    </div>

    <h3 class="question">{{ task.promptEn }}</h3>
    <p v-if="task.promptZhHint" class="zh-hint">{{ task.promptZhHint }}</p>

    <!-- Answer slots -->
    <div class="answer-slots">
      <div
        v-for="(word, idx) in placed"
        :key="'placed-' + idx"
        class="slot"
        :class="{
          'slot-filled': word !== null,
          'slot-correct': answered && correct,
          'slot-wrong': answered && !correct
        }"
        @click="removePlaced(idx)"
      >
        {{ word ?? '___' }}
      </div>
    </div>

    <!-- Word bank -->
    <div v-if="!answered" class="word-bank">
      <button
        v-for="(word, idx) in shuffled"
        :key="'bank-' + idx"
        class="word-chip"
        :class="{ 'chip-used': usedIndices.has(idx) }"
        :disabled="usedIndices.has(idx)"
        @click="placeWord(word, idx)"
      >
        {{ word }}
      </button>
    </div>

    <button
      v-if="!answered && allPlaced"
      class="btn-gold submit-btn"
      @click="submit"
    >
      确认答案
    </button>

    <div v-if="answered" class="result" :class="correct ? 'result-ok' : 'result-fail'">
      <span v-if="correct">✅ 顺序正确！</span>
      <span v-else>❌ 正确顺序：{{ task.correctAnswer }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Task } from '@/types'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()

const correctWords = ((props.task.answer as any)?.correctTokens ?? props.task.tokens ?? []) as string[]
const correctAnswer = correctWords.join(' ')
const shuffled = ref([...(props.task.tokens ?? correctWords)].sort(() => Math.random() - 0.5))
const placed = ref<(string | null)[]>(new Array(correctWords.length).fill(null))
const usedIndices = ref(new Set<number>())
const answered = ref(false)
const correct = ref(false)

const allPlaced = computed(() => placed.value.every(w => w !== null))

function placeWord(word: string, bankIdx: number) {
  const slot = placed.value.findIndex(w => w === null)
  if (slot >= 0) {
    placed.value[slot] = word
    usedIndices.value.add(bankIdx)
  }
}

function removePlaced(slotIdx: number) {
  if (answered.value) return
  const word = placed.value[slotIdx]
  if (word === null) return
  placed.value[slotIdx] = null
  // Find corresponding bank index
  const bankIdx = shuffled.value.findIndex((w, i) => w === word && usedIndices.value.has(i))
  if (bankIdx >= 0) usedIndices.value.delete(bankIdx)
}

function submit() {
  const result = placed.value.join(' ')
  correct.value = result === correctAnswer
  answered.value = true
  emit('answer', correct.value)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.word-order-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header { margin-bottom: 16px; }

.badge-order {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(245, 158, 11, 0.15);
  color: $gold;
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.question {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 24px;
}

.answer-slots {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 20px;
  min-height: 48px;
}

.slot {
  padding: 10px 18px;
  border: 2px dashed $border-dim;
  border-radius: $radius-md;
  font-size: 15px;
  font-weight: 600;
  color: $text-muted;
  min-width: 60px;
  text-align: center;
  cursor: pointer;
  transition: all $transition-fast;

  &-filled {
    border-style: solid;
    border-color: $gold;
    color: $text-primary;
    background: rgba($gold, 0.06);
  }

  &-correct {
    border-color: $life-green !important;
    color: $life-green !important;
    background: rgba($life-green, 0.08) !important;
    animation: scale-in 0.3s $ease-bounce;
  }

  &-wrong {
    border-color: $hp-red !important;
    color: $hp-red !important;
    background: rgba($hp-red, 0.08) !important;
    animation: shake 0.4s;
  }
}

.word-bank {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 20px;
}

.word-chip {
  padding: 10px 18px;
  border: 1px solid rgba($mana-blue, 0.25);
  border-radius: $radius-md;
  background: rgba($mana-blue, 0.06);
  color: $text-primary;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover:not(:disabled) {
    background: rgba($mana-blue, 0.12);
    transform: translateY(-2px);
  }

  &.chip-used {
    opacity: 0.2;
    pointer-events: none;
  }
}

.submit-btn {
  width: 100%;
  padding: 12px;
  animation: slide-up 0.3s ease-out;
}

.result {
  text-align: center;
  padding: 14px;
  border-radius: $radius-md;
  font-size: 15px;
  font-weight: 600;
  animation: scale-in 0.3s $ease-bounce;

  &-ok { background: rgba($life-green, 0.1); color: $life-green; }
  &-fail { background: rgba($hp-red, 0.1); color: $hp-red; }
}
</style>
