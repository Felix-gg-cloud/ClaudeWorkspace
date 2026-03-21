<template>
  <div class="spelling-card dark-panel" @click="focusInput">
    <div class="card-header">
      <span class="badge badge-spell">听音拼写</span>
      <button class="play-btn" @click="playTts" :class="{ playing }">
        <span class="play-icon">{{ playing ? '🔊' : '🔈' }}</span>
        <span class="play-label">播放发音</span>
      </button>
    </div>

    <h3 class="question">{{ task.promptEn }}</h3>
    <p v-if="task.promptZhHint" class="zh-hint">{{ task.promptZhHint }}</p>

    <!-- Letter boxes -->
    <div class="letter-boxes">
      <div
        v-for="(ch, idx) in answerChars"
        :key="idx"
        class="letter-box"
        :class="{
          'box-filled': userInput[idx],
          'box-cursor': idx === cursorPos && !answered,
          'box-correct': answered && correct,
          'box-wrong': answered && !correct
        }"
      >
        {{ userInput[idx] || '' }}
      </div>
    </div>

    <!-- Keyboard input area (hidden but focusable) -->
    <input
      v-if="!answered"
      ref="inputRef"
      class="hidden-input"
      @keydown="onKeydown"
      autofocus
      aria-label="拼写输入"
    />
    <p v-if="!answered" class="input-tip">直接用键盘输入字母，Backspace 删除</p>

    <!-- Submit -->
    <button
      v-if="!answered && inputFull"
      class="btn-gold submit-btn"
      @click="submit"
    >
      确认提交
    </button>

    <div v-if="answered" class="result" :class="correct ? 'result-ok' : 'result-fail'">
      <span>{{ correct ? '✅ 拼写正确！' : '❌ 正确答案：' + answer }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import type { Task } from '@/types'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()

const answer = ((props.task.answer as any)?.acceptedTexts?.[0] ?? '').toUpperCase()
const answerChars = answer.split('')

const userInput = ref<string[]>(new Array(answerChars.length).fill(''))
const answered = ref(false)
const correct = ref(false)
const playing = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)

const cursorPos = computed(() => {
  const idx = userInput.value.findIndex(c => !c)
  return idx >= 0 ? idx : answerChars.length
})

const inputFull = computed(() => userInput.value.every(c => c !== ''))

// TTS playback using browser SpeechSynthesis
const ttsText = (props.task.tts as any)?.ttsTextEn || answer
function playTts() {
  if (playing.value) return
  const utter = new SpeechSynthesisUtterance(ttsText)
  utter.lang = 'en-US'
  utter.rate = 0.85
  playing.value = true
  utter.onend = () => { playing.value = false }
  utter.onerror = () => { playing.value = false }
  speechSynthesis.cancel()
  speechSynthesis.speak(utter)
}

function onKeydown(e: KeyboardEvent) {
  if (answered.value) return
  const key = e.key.toUpperCase()
  if (/^[A-Z]$/.test(key)) {
    typeLetter(key)
  } else if (e.key === 'Backspace') {
    backspace()
  } else if (e.key === 'Enter' && inputFull.value) {
    submit()
  }
  e.preventDefault()
}

function typeLetter(letter: string) {
  const idx = userInput.value.findIndex(c => !c)
  if (idx >= 0) {
    userInput.value[idx] = letter
  }
}

function backspace() {
  for (let i = userInput.value.length - 1; i >= 0; i--) {
    if (userInput.value[i]) {
      userInput.value[i] = ''
      break
    }
  }
}

function submit() {
  const entered = userInput.value.join('').toUpperCase()
  const expected = answer.toUpperCase()
  correct.value = entered === expected
  answered.value = true
  emit('answer', correct.value)
}

onMounted(() => {
  nextTick(() => inputRef.value?.focus())
})

function focusInput() {
  if (!answered.value) inputRef.value?.focus()
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.spelling-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.badge-spell {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(16, 185, 129, 0.15);
  color: $life-green;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.play-btn {
  display: flex;
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
  transition: all 0.2s;

  &:hover {
    background: rgba($mana-blue, 0.18);
    border-color: rgba($mana-blue, 0.4);
  }

  &.playing {
    animation: pulse-glow 0.8s infinite;
  }
}

.play-icon { font-size: 16px; }

.question {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 24px;
}

.zh-hint {
  color: $text-muted;
  font-size: 14px;
  margin-top: -16px;
  margin-bottom: 20px;
}

.letter-boxes {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.letter-box {
  width: 44px;
  height: 52px;
  border: 2px solid $border-dim;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
  background: $bg-dark;
  transition: all $transition-fast;

  &.box-filled {
    border-color: $gold;
    background: rgba($gold, 0.06);
  }

  &.box-cursor {
    border-color: $gold;
    animation: pulse-glow 1s infinite;
  }

  &.box-correct {
    border-color: $life-green;
    background: rgba($life-green, 0.1);
    color: $life-green;
    animation: scale-in 0.3s $ease-bounce;
  }

  &.box-wrong {
    border-color: $hp-red;
    background: rgba($hp-red, 0.1);
    color: $hp-red;
    animation: shake 0.4s;
  }
}

.hidden-input {
  position: fixed;
  left: -9999px;
  opacity: 0;
  width: 1px;
  height: 1px;
}

.input-tip {
  text-align: center;
  color: $text-muted;
  font-size: 12px;
  margin-bottom: 16px;
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

  &-ok {
    background: rgba($life-green, 0.1);
    color: $life-green;
  }

  &-fail {
    background: rgba($hp-red, 0.1);
    color: $hp-red;
  }
}
</style>
