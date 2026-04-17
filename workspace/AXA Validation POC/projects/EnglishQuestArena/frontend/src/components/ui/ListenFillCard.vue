<template>
  <div class="listen-fill-card dark-panel" @click="focusInput">
    <div class="card-header">
      <span class="badge badge-listen-fill">听音填写</span>
      <span class="cefr-tag" v-if="task.cefrLevel">{{ task.cefrLevel }}</span>
    </div>

    <!-- 播放区 -->
    <div class="listen-hero">
      <button class="play-circle" :class="{ playing }" @click.stop="playTts">
        <span class="play-icon-lg">{{ playing ? '🔊' : '🔈' }}</span>
      </button>
      <p class="listen-tip">仔细听发音，然后拼写你听到的单词</p>
      <button v-if="canReplay" class="replay-btn" @click.stop="playTts">🔁 再听一次</button>
    </div>

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

    <input
      v-if="!answered"
      ref="inputRef"
      class="hidden-input"
      @keydown="onKeydown"
      autofocus
      aria-label="听音填写输入"
    />
    <p v-if="!answered" class="input-tip">直接用键盘输入字母，Backspace 删除</p>

    <button
      v-if="!answered && inputFull"
      class="btn-gold submit-btn"
      @click="submit"
    >
      确认提交
    </button>

    <div v-if="answered" class="result-area">
      <div class="result" :class="correct ? 'result-ok' : 'result-fail'">
        {{ correct ? '✅ 拼写正确！' : '❌ 正确答案：' + answer }}
      </div>
      <p v-if="task.explanationZh" class="explain">💡 {{ task.explanationZh }}</p>
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
const canReplay = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)

const cursorPos = computed(() => {
  const idx = userInput.value.findIndex(c => !c)
  return idx >= 0 ? idx : answerChars.length
})

const inputFull = computed(() => userInput.value.every(c => c !== ''))

const ttsText = (props.task.tts as any)?.ttsTextEn || answer
function playTts() {
  if (playing.value) return
  const utter = new SpeechSynthesisUtterance(ttsText)
  utter.lang = 'en-US'
  utter.rate = 0.75
  playing.value = true
  utter.onend = () => { playing.value = false; canReplay.value = true }
  utter.onerror = () => { playing.value = false }
  speechSynthesis.cancel()
  speechSynthesis.speak(utter)
}

function onKeydown(e: KeyboardEvent) {
  if (answered.value) return
  const key = e.key.toUpperCase()
  if (/^[A-Z]$/.test(key)) {
    const idx = userInput.value.findIndex(c => !c)
    if (idx >= 0) userInput.value[idx] = key
  } else if (e.key === 'Backspace') {
    for (let i = userInput.value.length - 1; i >= 0; i--) {
      if (userInput.value[i]) { userInput.value[i] = ''; break }
    }
  } else if (e.key === 'Enter' && inputFull.value) {
    submit()
  }
  e.preventDefault()
}

function submit() {
  const entered = userInput.value.join('').toUpperCase()
  correct.value = entered === answer
  answered.value = true
  emit('answer', correct.value)
}

onMounted(() => {
  nextTick(() => inputRef.value?.focus())
  // 自动播放一次
  setTimeout(playTts, 500)
})

function focusInput() {
  if (!answered.value) inputRef.value?.focus()
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.listen-fill-card {
  padding: 28px;
  animation: scale-in 0.3s ease-out;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.badge-listen-fill {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(139, 92, 246, 0.15);
  color: #a78bfa;
  border: 1px solid rgba(139, 92, 246, 0.25);
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

.listen-hero {
  text-align: center;
  margin-bottom: 24px;
}

.play-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba($mana-blue, 0.12);
  border: 2px solid rgba($mana-blue, 0.3);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 12px;

  &:hover { background: rgba($mana-blue, 0.2); transform: scale(1.05); }
  &.playing { animation: pulse-glow 1s infinite; border-color: $mana-blue; }
}

.play-icon-lg { font-size: 32px; }

.listen-tip {
  font-size: 13px;
  color: $text-muted;
  margin: 8px 0;
}

.replay-btn {
  padding: 6px 16px;
  border-radius: 16px;
  background: rgba($mana-blue, 0.08);
  border: 1px solid rgba($mana-blue, 0.2);
  color: $mana-blue;
  font-size: 12px;
  cursor: pointer;
  &:hover { background: rgba($mana-blue, 0.15); }
}

.letter-boxes {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin: 20px 0;
  flex-wrap: wrap;
}

.letter-box {
  width: 40px;
  height: 48px;
  border: 2px solid $border-dim;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 800;
  color: $text-primary;
  background: rgba(255, 255, 255, 0.02);
  transition: all 0.2s;

  &.box-filled { border-color: rgba($gold, 0.4); }
  &.box-cursor { border-color: $gold; box-shadow: $shadow-glow-gold; animation: pulse-glow 1.5s infinite; }
  &.box-correct { border-color: $life-green; background: rgba($life-green, 0.08); color: $life-green; }
  &.box-wrong { border-color: $hp-red; background: rgba($hp-red, 0.08); color: $hp-red; }
}

.hidden-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.input-tip {
  text-align: center;
  font-size: 12px;
  color: $text-muted;
  margin-bottom: 16px;
}

.submit-btn {
  display: block;
  margin: 0 auto;
  padding: 10px 36px;
}

.result-area { margin-top: 16px; text-align: center; }

.result {
  padding: 12px 20px;
  border-radius: $radius-lg;
  font-size: 15px;
  font-weight: 700;
  &.result-ok { background: rgba($life-green, 0.1); color: $life-green; }
  &.result-fail { background: rgba($hp-red, 0.1); color: $hp-red; }
}

.explain {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 12px;
}
</style>
