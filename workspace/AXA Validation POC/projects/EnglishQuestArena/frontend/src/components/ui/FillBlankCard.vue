<template>
  <div class="fill-blank-card dark-panel">
    <div class="card-header">
      <span class="badge badge-fill">填空选择</span>
      <span class="cefr-tag" v-if="task.cefrLevel">{{ task.cefrLevel }}</span>
    </div>

    <!-- 句子展示，空位高亮 -->
    <div class="sentence">
      <template v-for="(part, idx) in sentenceParts" :key="idx">
        <span v-if="part !== '___'" class="word-part">{{ part }}</span>
        <span v-else class="blank-slot" :class="{
          'blank-filled': selectedAnswer,
          'blank-correct': answered && correct,
          'blank-wrong': answered && !correct
        }">
          {{ selectedAnswer || '______' }}
        </span>
      </template>
    </div>

    <p v-if="task.promptZhHint" class="zh-hint">{{ task.promptZhHint }}</p>

    <!-- 选项 -->
    <div v-if="!answered" class="options">
      <button
        v-for="(opt, idx) in task.options"
        :key="idx"
        class="option-chip"
        :class="{ 'chip-selected': selectedIdx === idx }"
        @click="pickOption(idx, opt)"
      >
        {{ opt.textEn }}
      </button>
    </div>

    <!-- 提交按钮 -->
    <button
      v-if="selectedAnswer && !answered"
      class="btn-gold submit-btn"
      @click="submit"
    >
      确认
    </button>

    <!-- 结果 -->
    <div v-if="answered" class="result-area">
      <div class="result" :class="correct ? 'result-ok' : 'result-fail'">
        {{ correct ? '✅ 正确！' : '❌ 正确答案：' + correctAnswer }}
      </div>
      <button class="play-btn-sm" @click="playTts">🔈 听整句</button>
      <p v-if="task.explanationZh" class="explain">💡 {{ task.explanationZh }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Task, TaskOption } from '@/types'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()

const correctKey = computed(() => (props.task.answer as any)?.correctOptionKey ?? '')
const correctAnswer = computed(() => {
  const opt = props.task.options?.find(o => o.key === correctKey.value)
  return opt?.textEn ?? ''
})

// 将 promptEn 里的 ___ 拆分成 parts
const sentenceParts = computed(() => {
  const prompt = props.task.promptEn || ''
  // 匹配连续的下划线（3个以上）或 {blank}
  return prompt.split(/(___+|\{blank\})/).map(p =>
    /^(___+|\{blank\})$/.test(p) ? '___' : p
  )
})

const selectedIdx = ref(-1)
const selectedAnswer = ref('')
const answered = ref(false)
const correct = ref(false)

function pickOption(idx: number, opt: TaskOption) {
  selectedIdx.value = idx
  selectedAnswer.value = opt.textEn
}

function submit() {
  const opt = props.task.options?.[selectedIdx.value]
  if (!opt) return
  correct.value = opt.key === correctKey.value
  answered.value = true
  emit('answer', correct.value)
}

function playTts() {
  const text = props.task.tts?.ttsTextEn || props.task.promptEn.replace(/___+|\{blank\}/g, correctAnswer.value)
  const utter = new SpeechSynthesisUtterance(text)
  utter.lang = 'en-US'
  utter.rate = 0.85
  speechSynthesis.cancel()
  speechSynthesis.speak(utter)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.fill-blank-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.badge-fill {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(249, 115, 22, 0.15);
  color: $fire-orange;
  border: 1px solid rgba(249, 115, 22, 0.2);
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

.sentence {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.8;
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px;
}

.word-part {
  white-space: pre;
}

.blank-slot {
  display: inline-block;
  min-width: 80px;
  padding: 4px 12px;
  border-bottom: 3px solid $gold;
  color: $gold-light;
  text-align: center;
  font-weight: 800;
  transition: all 0.2s;

  &.blank-filled {
    background: rgba($gold, 0.08);
    border-radius: $radius-sm $radius-sm 0 0;
  }

  &.blank-correct {
    border-color: $life-green;
    color: $life-green;
    background: rgba($life-green, 0.1);
  }

  &.blank-wrong {
    border-color: $hp-red;
    color: $hp-red;
    background: rgba($hp-red, 0.08);
    animation: shake 0.4s;
  }
}

.zh-hint {
  font-size: 14px;
  color: $text-muted;
  margin-bottom: 20px;
}

.options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.option-chip {
  padding: 10px 20px;
  border: 2px solid $border-dim;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.03);
  color: $text-primary;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba($fire-orange, 0.4);
    background: rgba($fire-orange, 0.06);
  }

  &.chip-selected {
    border-color: $gold;
    background: rgba($gold, 0.12);
    color: $gold-light;
    box-shadow: 0 0 12px rgba($gold, 0.15);
  }
}

.submit-btn {
  width: 100%;
  padding: 12px;
  animation: slide-up 0.3s ease-out;
}

.result-area {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result {
  text-align: center;
  padding: 12px;
  border-radius: $radius-md;
  font-size: 15px;
  font-weight: 600;
  animation: scale-in 0.3s $ease-bounce;

  &-ok {
    background: rgba($life-green, 0.1);
    color: $life-green;
  }

  &-fail {
    background: rgba($hp-red, 0.1);
    color: $hp-red;
  }
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

  &:hover { background: rgba($mana-blue, 0.18); }
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
