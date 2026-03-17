<template>
  <Teleport to="body">
    <div class="encounter-overlay" @click.self="$emit('close')">
      <div class="encounter-dialog" :class="dialogClass">

        <!-- ========== 阶段 1: 学习 ========== -->
        <template v-if="phase === 'learn'">
          <div class="dialog-ribbon">
            <span class="ribbon-icon">📖</span>
            <span class="ribbon-text">新词学习</span>
            <span class="ribbon-icon">📖</span>
          </div>

          <!-- 怪物形象 + 单词大字 -->
          <div class="learn-hero">
            <div class="monster-mini">
              <div class="monster-glow"></div>
              <div class="monster-body">{{ monsterEmoji }}</div>
            </div>
            <div class="learn-arrow">→</div>
            <div class="learn-word-block">
              <h2 class="learn-word-en">{{ encounter.wordEn }}</h2>
              <p class="learn-word-zh">{{ encounter.wordZh }}</p>
            </div>
          </div>

          <!-- 音标 + TTS -->
          <div class="learn-phonetic-row">
            <span class="phonetic-text">{{ encounter.phonetic }}</span>
            <button class="btn-tts" @click="playTts">🔊 听发音</button>
          </div>

          <!-- 例句 -->
          <div class="learn-sentence-box">
            <p class="sentence-en">"{{ encounter.sentence }}"</p>
            <p class="sentence-zh">{{ encounter.sentenceZh }}</p>
          </div>

          <!-- 记忆提示 (如果有) -->
          <div class="learn-tip" v-if="mnemonicTip">
            <span class="tip-icon">💡</span>
            <span class="tip-text">{{ mnemonicTip }}</span>
          </div>

          <!-- 下一步 -->
          <button ref="learnedBtn" class="btn-learned" @click="goToPractice">✨ 我记住了！开始挑战 →</button>
        </template>

        <!-- ========== 阶段 2: 练习 ========== -->
        <template v-if="phase === 'practice'">
          <div class="dialog-ribbon">
            <span class="ribbon-icon">⚔️</span>
            <span class="ribbon-text">{{ practiceTypeLabel }}</span>
            <span class="ribbon-icon">⚔️</span>
          </div>

          <!-- 怪物 HP -->
          <div class="monster-section">
            <div class="monster-stage">
              <div class="monster-glow"></div>
              <div class="monster-sprite">
                <div class="monster-body">{{ monsterEmoji }}</div>
              </div>
            </div>
            <div class="monster-hp-bar">
              <div class="hp-label">HP</div>
              <div class="hp-track">
                <div class="hp-fill" :style="{ width: '100%' }"></div>
              </div>
            </div>
          </div>

          <!-- 练习类型: 选择题 -->
          <template v-if="practiceType === 'mcq'">
            <p class="practice-prompt">选择「<strong>{{ encounter.wordEn }}</strong>」的正确释义</p>
            <div class="options-grid">
              <button
                v-for="(opt, idx) in encounter.options"
                :key="opt.key"
                class="option-btn"
                :class="{ 'option-selected': selectedKey === opt.key }"
                @click="selectOption(opt.key)"
                @keydown.enter.prevent="selectOption(opt.key)"
              >
                <span class="option-key">{{ ['A','B','C','D'][idx] }}</span>
                <span class="option-text">{{ opt.textZh }}</span>
                <span class="option-check" v-if="selectedKey === opt.key">✓</span>
              </button>
            </div>
            <button ref="confirmBtn" class="btn-confirm" :disabled="!selectedKey" @click="confirmMcq">
              ⚔️ 确认攻击
            </button>
          </template>

          <!-- 练习类型: 听音选词 -->
          <template v-if="practiceType === 'listen'">
            <div class="listen-prompt">
              <button class="btn-listen-big" @click="playTts">
                <span class="listen-icon">🔊</span>
                <span>点击听发音</span>
              </button>
              <p class="listen-hint">听音后选择正确的单词</p>
            </div>
            <div class="options-grid">
              <button
                v-for="(opt, idx) in listenOptions"
                :key="idx"
                class="option-btn"
                :class="{ 'option-selected': selectedListenIdx === idx }"
                @click="selectListenOption(idx)"
                @keydown.enter.prevent="selectListenOption(idx)"
              >
                <span class="option-key">{{ ['A','B','C'][idx] }}</span>
                <span class="option-text">{{ opt.en }}</span>
              </button>
            </div>
            <button ref="confirmBtn" class="btn-confirm" :disabled="selectedListenIdx === null" @click="confirmListen">
              ⚔️ 确认攻击
            </button>
          </template>

          <!-- 练习类型: 拼写 -->
          <template v-if="practiceType === 'spell'">
            <p class="practice-prompt">拼写出这个单词: <strong>{{ encounter.wordZh }}</strong></p>
            <div class="spell-input-wrapper">
              <input
                ref="spellInput"
                v-model="spellText"
                class="spell-input"
                placeholder="输入英文..."
                @keyup.enter="confirmSpell"
                autocomplete="off"
                autocapitalize="off"
              />
              <button class="btn-tts-mini" @click="playTts" title="再听一次">🔊</button>
            </div>
            <button ref="confirmBtn" class="btn-confirm" :disabled="!spellText.trim()" @click="confirmSpell">
              ⚔️ 确认攻击
            </button>
          </template>
        </template>

        <!-- ========== 阶段 3: 结果 ========== -->
        <template v-if="phase === 'result'">
          <div class="dialog-ribbon">
            <span class="ribbon-icon">{{ correct ? '⭐' : '💥' }}</span>
            <span class="ribbon-text">{{ correct ? '胜利!' : '失败...' }}</span>
            <span class="ribbon-icon">{{ correct ? '⭐' : '💥' }}</span>
          </div>

          <div class="monster-section">
            <div class="monster-stage">
              <div class="monster-sprite" :class="{ 'monster-defeated': correct, 'monster-shake': !correct }">
                <div class="monster-body">{{ monsterEmoji }}</div>
              </div>
            </div>
          </div>

          <!-- 正确 -->
          <div v-if="correct" class="result-correct">
            <div class="result-stars">⭐ ⭐ ⭐</div>
            <h3>击败成功！</h3>
            <p class="result-word">「{{ encounter.wordEn }}」= {{ encounter.wordZh }}</p>
            <div class="reward-line" v-if="encounter.reward">
              <div class="reward-badge reward-xp">✨ +{{ encounter.reward.xp }} XP</div>
              <div class="reward-badge reward-coin">🪙 +{{ encounter.reward.coins }} 金币</div>
            </div>
            <div class="combo-bonus" v-if="comboDisplay > 1">
              <span class="combo-fire">🔥</span>
              <span class="combo-label">COMBO ×{{ comboDisplay }}</span>
              <span class="combo-detail">+{{ comboBonus }}% 经验加成</span>
            </div>

            <!-- 强化信息 -->
            <div class="result-extra">
              <div class="extra-sentence">
                <span class="extra-label">📝 例句</span>
                <p>"{{ encounter.sentence }}"</p>
                <p class="extra-zh">{{ encounter.sentenceZh }}</p>
              </div>
              <div class="extra-tip" v-if="usageTip">
                <span class="extra-label">💡 用法</span>
                <p>{{ usageTip }}</p>
              </div>
            </div>
            <p class="word-collected">📖 已收入图鉴</p>
          </div>

          <!-- 错误 -->
          <div v-else class="result-wrong">
            <div class="result-crack">💥</div>
            <h3>未能击败...</h3>
            <p>正确答案: <strong>{{ encounter.wordZh }}</strong></p>

            <!-- 复习区: 再看一次 -->
            <div class="result-review">
              <div class="review-word">
                <span class="review-en">{{ encounter.wordEn }}</span>
                <span class="review-phonetic">{{ encounter.phonetic }}</span>
                <button class="btn-tts-mini" @click="playTts">🔊</button>
              </div>
              <p class="review-sentence">"{{ encounter.sentence }}"</p>
              <p class="review-sentence-zh">{{ encounter.sentenceZh }}</p>
            </div>
            <p class="hint-text">下次再遇到它，你一定能记住！</p>
          </div>

          <button ref="continueBtn" class="btn-continue" @click="emitResolve">
            🏕️ 继续探索
          </button>
        </template>

      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import type { MapEncounter } from '@/game/config/mapData'

const props = defineProps<{
  encounter: MapEncounter
  combo?: number
}>()

const emit = defineEmits<{
  resolve: [result: { id: string; success: boolean }]
  close: []
}>()

// ---- State ----
type Phase = 'learn' | 'practice' | 'result'
type PracticeType = 'mcq' | 'listen' | 'spell'

const phase = ref<Phase>('learn')
const correct = ref(false)

// 练习类型随机选择
const practiceType = ref<PracticeType>('mcq')
const selectedKey = ref<string | null>(null)
const selectedListenIdx = ref<number | null>(null)
const spellText = ref('')
const spellInput = ref<HTMLInputElement>()
const learnedBtn = ref<HTMLButtonElement>()
const confirmBtn = ref<HTMLButtonElement>()
const continueBtn = ref<HTMLButtonElement>()

// ---- Computed ----
const monsterEmoji = computed(() => {
  const d = props.encounter.difficulty
  return d === 'easy' ? '🐸' : d === 'hard' ? '🐲' : '🐺'
})

const dialogClass = computed(() => ({
  'encounter-success': phase.value === 'result' && correct.value,
  'encounter-fail': phase.value === 'result' && !correct.value,
}))

const practiceTypeLabel = computed(() => {
  const labels: Record<PracticeType, string> = {
    mcq: '选择释义',
    listen: '听音选词',
    spell: '拼写挑战',
  }
  return labels[practiceType.value]
})

// 听音选项: 正确词 + 2 个干扰项
const listenOptions = computed(() => {
  const correct = { en: props.encounter.wordEn, zh: props.encounter.wordZh }
  const distractors = (props.encounter.options ?? [])
    .filter(o => o.key !== props.encounter.correctKey)
    .slice(0, 2)
    .map(o => ({ en: o.textEn || o.textZh, zh: o.textZh }))
  const all = [correct, ...distractors]
  // 简单打乱
  const seed = props.encounter.wordEn.length
  return all
    .map((item, i) => ({ item, sort: ((seed + 1) * (i + 1) * 37) % 97 }))
    .sort((a, b) => a.sort - b.sort)
    .map(x => x.item)
})

// 记忆助记词 (简单规则生成)
const mnemonicTip = computed(() => {
  const w = props.encounter.wordEn.toLowerCase()
  const tips: Record<string, string> = {
    hello: '发音像"哈喽"，打招呼就喊它！',
    goodbye: 'good(好) + bye(再见) = 美好的道别',
    'thank you': 'thank 感谢 + you 你 → 感谢你！',
    sorry: '发音像"扫瑞"，做错事就说它',
    please: '发音像"普利斯"，礼貌用语的万能钥匙',
    name: '发音像"内姆"，你的名牌上写的就是它',
    happy: '哈皮！开心就是这个音',
    sad: '就一个音节，伤心时叹気: sad~',
    cat: '喵！猫咪的英文很短: cat',
    dog: '汪！小狗只有三个字母: dog',
    mom: '妈妈 mama 的英文缩写就是 mom',
    dad: '爸爸 dada 的英文缩写就是 dad',
    love: '❤️ Love is all you need!',
    like: 'I like = 我喜欢，后面加上任何你爱的东西',
    one: '一根手指就是 one',
    two: '两只眼睛 two eyes',
    three: '三角形 triangle 的 three',
    red: '红色 red，想想红灯 red light',
    blue: '蓝色 blue，想想蓝天 blue sky',
    green: '绿色 green，想想绿草 green grass',
    big: 'BIG 三个字母，大声说！',
    small: '小小的 small，轻轻说~',
  }
  return tips[w] ?? null
})

// combo 显示 (当前 combo + 本次 = combo + 1)
const comboDisplay = computed(() => (props.combo ?? 0) + 1)
const comboBonus = computed(() => Math.min((comboDisplay.value - 1) * 20, 100))

// 用法提示
const usageTip = computed(() => {
  const w = props.encounter.wordEn.toLowerCase()
  const tips: Record<string, string> = {
    hello: '正式场合用 Hello，朋友间可以用 Hi 或 Hey',
    goodbye: '也可以说 Bye 或 See you，更加口语化',
    'thank you': '更随意可以说 Thanks，更正式说 Thank you very much',
    sorry: "道歉说 I'm sorry，表示遗憾也可以说 Sorry to hear that",
    please: '放在句尾或句首都行: Please sit down / Sit down, please',
    yes: '口语中也常说 Yeah, Yep, Sure',
    no: '委婉拒绝可以说 No, thank you',
  }
  return tips[w] ?? null
})

// ---- Methods ----
function playTts() {
  if ('speechSynthesis' in window && props.encounter.wordEn) {
    window.speechSynthesis.cancel()
    const utter = new SpeechSynthesisUtterance(props.encounter.wordEn)
    utter.lang = 'en-US'
    utter.rate = 0.75
    window.speechSynthesis.speak(utter)
  }
}

function goToPractice() {
  // 自动播放一次发音
  playTts()

  // 选择练习类型 — 简单词用拼写，有选项的用 MCQ 或听音
  const hasOptions = (props.encounter.options?.length ?? 0) >= 2
  const wordLen = props.encounter.wordEn.length

  if (!hasOptions) {
    // 没有选项只能拼写
    practiceType.value = 'spell'
  } else if (wordLen <= 5) {
    // 短词: 随机 MCQ 或 拼写
    const r = Math.random()
    practiceType.value = r < 0.4 ? 'spell' : r < 0.7 ? 'listen' : 'mcq'
  } else {
    // 长词: MCQ 或 听音
    practiceType.value = Math.random() < 0.5 ? 'mcq' : 'listen'
  }

  phase.value = 'practice'

  if (practiceType.value === 'spell') {
    nextTick(() => spellInput.value?.focus())
  }
}

function selectOption(key: string) {
  selectedKey.value = key
  nextTick(() => confirmBtn.value?.focus())
}

function selectListenOption(idx: number) {
  selectedListenIdx.value = idx
  nextTick(() => confirmBtn.value?.focus())
}

function confirmMcq() {
  if (!selectedKey.value) return
  correct.value = selectedKey.value === props.encounter.correctKey
  phase.value = 'result'
}

function confirmListen() {
  if (selectedListenIdx.value === null) return
  const selected = listenOptions.value[selectedListenIdx.value]
  correct.value = selected?.en === props.encounter.wordEn
  phase.value = 'result'
}

function confirmSpell() {
  const input = spellText.value.trim().toLowerCase()
  const answer = props.encounter.wordEn.toLowerCase()
  correct.value = input === answer
  phase.value = 'result'
}

function emitResolve() {
  emit('resolve', {
    id: props.encounter.id,
    success: correct.value,
  })
}

// 自动聚焦主操作按钮
onMounted(() => {
  nextTick(() => learnedBtn.value?.focus())
})

watch(phase, (p) => {
  nextTick(() => {
    if (p === 'result') continueBtn.value?.focus()
  })
})
</script>

<style scoped lang="scss">
.encounter-overlay {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(0,0,0,0.6) 0%, rgba(0,0,0,0.85) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.25s ease;
  backdrop-filter: blur(4px);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.encounter-dialog {
  width: 92vw;
  max-width: 440px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 0 24px 24px;
  border-radius: 20px;
  border: 2px solid rgba(180, 140, 80, 0.5);
  background: linear-gradient(170deg, #1a1410 0%, #0f0c08 50%, #12100e 100%);
  box-shadow:
    0 0 40px rgba(180, 140, 80, 0.15),
    0 20px 60px rgba(0,0,0,0.5),
    inset 0 1px 0 rgba(255,255,255,0.06);
  animation: dialogIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 3px;
    border-radius: 17px;
    border: 1px solid rgba(180, 140, 80, 0.12);
    pointer-events: none;
  }

  &.encounter-success {
    border-color: rgba(80, 180, 80, 0.6);
    box-shadow: 0 0 50px rgba(80, 180, 80, 0.15), 0 20px 60px rgba(0,0,0,0.5);
  }
  &.encounter-fail {
    border-color: rgba(180, 60, 60, 0.5);
    box-shadow: 0 0 50px rgba(180, 60, 60, 0.15), 0 20px 60px rgba(0,0,0,0.5);
  }
}

@keyframes dialogIn {
  from { transform: translateY(40px) scale(0.92); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}

/* ---- Ribbon ---- */
.dialog-ribbon {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 0 10px;

  .ribbon-icon { font-size: 14px; opacity: 0.7; }
  .ribbon-text {
    font-size: 13px;
    color: rgba(180, 140, 80, 0.8);
    letter-spacing: 3px;
    font-weight: 600;
  }
}

/* ============ Phase 1: Learn ============ */
.learn-hero {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.monster-mini {
  position: relative;
  .monster-glow {
    position: absolute;
    inset: -8px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(255, 80, 60, 0.12) 0%, transparent 70%);
    animation: glowPulse 2s ease-in-out infinite;
  }
  .monster-body { font-size: 44px; filter: drop-shadow(0 3px 6px rgba(0,0,0,0.4)); }
}

.learn-arrow {
  font-size: 24px;
  color: rgba(255, 200, 80, 0.5);
  animation: arrowPulse 1.5s ease-in-out infinite;
}
@keyframes arrowPulse {
  0%, 100% { transform: translateX(0); opacity: 0.5; }
  50% { transform: translateX(4px); opacity: 1; }
}

.learn-word-block {
  text-align: center;
  .learn-word-en {
    font-size: 32px;
    color: #a8d8ff;
    margin: 0;
    font-weight: 700;
    text-shadow: 0 0 20px rgba(100, 180, 255, 0.2);
  }
  .learn-word-zh {
    font-size: 18px;
    color: #eee;
    margin: 4px 0 0;
    font-weight: 500;
  }
}

.learn-phonetic-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 14px;

  .phonetic-text {
    color: #888;
    font-size: 15px;
    font-style: italic;
  }
  .btn-tts {
    padding: 5px 14px;
    border: 1px solid rgba(100, 180, 255, 0.25);
    border-radius: 16px;
    background: rgba(100, 180, 255, 0.08);
    color: #88bbff;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
    &:hover { background: rgba(100, 180, 255, 0.18); transform: scale(1.05); }
    &:active { transform: scale(0.96); }
  }
}

.learn-sentence-box {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 12px;
  text-align: center;
  .sentence-en { color: #ccc; font-size: 15px; margin: 0 0 4px; font-style: italic; }
  .sentence-zh { color: #888; font-size: 13px; margin: 0; }
}

.learn-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: rgba(255, 200, 60, 0.06);
  border: 1px solid rgba(255, 200, 60, 0.15);
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 16px;
  .tip-icon { font-size: 16px; }
  .tip-text { color: #ddc080; font-size: 13px; line-height: 1.5; }
}

.btn-learned {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #4a9b5a, #3a8a4a);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  font-weight: 700;
  letter-spacing: 1px;
  box-shadow: 0 4px 15px rgba(60, 140, 70, 0.3);
  transition: all 0.2s;
  &:hover { background: linear-gradient(135deg, #5aab6a, #4a9a5a); transform: translateY(-1px); }
  &:active { transform: translateY(1px); }
}

/* ============ Phase 2: Practice ============ */
.monster-section {
  text-align: center;
  margin-bottom: 14px;
}
.monster-stage {
  position: relative;
  display: inline-block;
  padding: 10px;
}
.monster-glow {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 80, 60, 0.12) 0%, transparent 70%);
  animation: glowPulse 2s ease-in-out infinite;
}
@keyframes glowPulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.15); opacity: 1; }
}
.monster-sprite {
  position: relative;
  animation: floatMonster 2.5s ease-in-out infinite;
  transition: all 0.5s;
  &.monster-defeated { opacity: 0.2; transform: scale(0.6) rotate(25deg); filter: grayscale(1); }
  &.monster-shake { animation: shake 0.4s ease; }
  .monster-body { font-size: 56px; filter: drop-shadow(0 4px 8px rgba(0,0,0,0.4)); }
}
@keyframes floatMonster {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-5px); }
  80% { transform: translateX(5px); }
}
.monster-hp-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 140px;
  margin: 8px auto 0;
  .hp-label { font-size: 11px; font-weight: 700; color: #cc4444; letter-spacing: 1px; }
  .hp-track {
    flex: 1; height: 8px;
    background: rgba(100, 30, 30, 0.4);
    border-radius: 4px; overflow: hidden;
    border: 1px solid rgba(200, 60, 60, 0.3);
  }
  .hp-fill {
    height: 100%;
    background: linear-gradient(90deg, #cc2222, #ff5544);
    border-radius: 4px;
    transition: width 0.5s ease;
    box-shadow: 0 0 8px rgba(255, 60, 40, 0.4);
  }
}

.practice-prompt {
  text-align: center;
  color: #bbb;
  font-size: 15px;
  margin: 0 0 14px;
  strong { color: #a8d8ff; }
}

.options-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.option-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.025);
  color: #ccc;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;

  &:hover { border-color: rgba(180, 140, 80, 0.35); background: rgba(180, 140, 80, 0.06); transform: translateX(3px); }
  &.option-selected {
    border-color: rgba(255, 200, 60, 0.6);
    background: rgba(255, 200, 60, 0.1);
    color: #ffe080;
    box-shadow: 0 0 15px rgba(255, 200, 60, 0.08);
  }
  .option-key {
    width: 28px; height: 28px;
    border: 1.5px solid currentColor; border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    font-size: 13px; font-weight: 600; flex-shrink: 0;
  }
  .option-text { font-size: 15px; flex: 1; }
  .option-check { font-size: 16px; color: #ffd700; }
}

.btn-confirm {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #cc6600, #aa4400);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  font-weight: 700;
  letter-spacing: 1px;
  box-shadow: 0 4px 15px rgba(204, 102, 0, 0.25);
  transition: all 0.2s;
  &:hover:not(:disabled) { background: linear-gradient(135deg, #dd7700, #bb5500); transform: translateY(-1px); }
  &:active:not(:disabled) { transform: translateY(1px); }
  &:disabled { opacity: 0.35; cursor: not-allowed; background: #444; box-shadow: none; }
}

/* 听音 */
.listen-prompt {
  text-align: center;
  margin-bottom: 16px;
}
.btn-listen-big {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  border: 2px solid rgba(100, 180, 255, 0.3);
  border-radius: 16px;
  background: rgba(100, 180, 255, 0.08);
  color: #88bbff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
  .listen-icon { font-size: 24px; }
  &:hover { background: rgba(100, 180, 255, 0.18); transform: scale(1.03); }
  &:active { transform: scale(0.97); }
}
.listen-hint { color: #777; font-size: 13px; margin: 4px 0 0; }

/* 拼写 */
.spell-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.spell-input {
  flex: 1;
  padding: 12px 16px;
  border: 1.5px solid rgba(180, 140, 80, 0.3);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  color: #eee;
  font-size: 18px;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
  outline: none;
  transition: border-color 0.2s;
  &:focus { border-color: rgba(255, 200, 60, 0.6); }
  &::placeholder { color: #555; letter-spacing: 0; }
}
.btn-tts-mini {
  padding: 8px;
  border: 1px solid rgba(100, 180, 255, 0.2);
  border-radius: 8px;
  background: rgba(100, 180, 255, 0.06);
  font-size: 16px;
  cursor: pointer;
  color: #88bbff;
  transition: all 0.2s;
  &:hover { background: rgba(100, 180, 255, 0.15); }
}

/* ============ Phase 3: Result ============ */
.result-correct, .result-wrong {
  text-align: center;
  margin-bottom: 18px;
}
.result-stars {
  font-size: 24px;
  margin-bottom: 8px;
  animation: starsIn 0.6s ease;
}
@keyframes starsIn {
  from { transform: scale(0.3) rotate(-20deg); opacity: 0; }
  to { transform: scale(1) rotate(0); opacity: 1; }
}
.result-crack { font-size: 44px; margin-bottom: 8px; }

h3 { margin: 0 0 8px; font-size: 22px; font-weight: 700; }
.result-correct {
  h3 { color: #66dd66; text-shadow: 0 0 15px rgba(80,200,80,0.2); }
  .result-word { color: #bbb; margin: 4px 0 12px; font-size: 15px; }
  .reward-line { display: flex; justify-content: center; gap: 12px; margin: 10px 0; }
  .reward-badge {
    padding: 6px 14px; border-radius: 20px; font-size: 14px; font-weight: 600;
    &.reward-xp { background: rgba(255, 200, 60, 0.12); border: 1px solid rgba(255, 200, 60, 0.3); color: #ffd700; }
    &.reward-coin { background: rgba(255, 180, 40, 0.12); border: 1px solid rgba(255, 180, 40, 0.3); color: #ffbb33; }
  }
  .word-collected { font-size: 13px; color: #88aadd; margin-top: 8px; }
}

.combo-bonus {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 14px;
  margin: 8px 0;
  border-radius: 10px;
  background: rgba(255, 120, 0, 0.08);
  border: 1px solid rgba(255, 120, 0, 0.25);
  animation: comboPulse 0.6s ease;
  .combo-fire { font-size: 18px; }
  .combo-label {
    font-size: 16px;
    font-weight: 900;
    color: #ff8800;
    letter-spacing: 2px;
  }
  .combo-detail {
    font-size: 12px;
    color: #ffaa44;
    opacity: 0.8;
  }
}
@keyframes comboPulse {
  from { transform: scale(0.8); opacity: 0; }
  50% { transform: scale(1.08); }
  to { transform: scale(1); opacity: 1; }
}

.result-extra {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 12px 14px;
  margin: 12px 0;
  text-align: left;
  .extra-label { font-size: 12px; color: rgba(180, 140, 80, 0.7); font-weight: 600; margin-bottom: 4px; display: block; }
  .extra-sentence p { margin: 2px 0; color: #bbb; font-size: 13px; }
  .extra-zh { color: #888 !important; }
  .extra-tip { margin-top: 10px; }
  .extra-tip p { margin: 2px 0; color: #ccc; font-size: 13px; }
}

.result-wrong {
  h3 { color: #ee5555; }
  p { color: #bbb; margin: 4px 0; font-size: 14px; }
  strong { color: #ffa; }
  .hint-text { color: #88bbff; font-size: 13px; margin-top: 10px; }
}

.result-review {
  background: rgba(100, 180, 255, 0.04);
  border: 1px solid rgba(100, 180, 255, 0.12);
  border-radius: 10px;
  padding: 12px 14px;
  margin: 12px 0;
  text-align: center;
  .review-word {
    display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 6px;
    .review-en { font-size: 20px; color: #a8d8ff; font-weight: 700; }
    .review-phonetic { font-size: 13px; color: #777; font-style: italic; }
  }
  .review-sentence { color: #bbb; font-size: 13px; margin: 4px 0 2px; font-style: italic; }
  .review-sentence-zh { color: #888; font-size: 12px; margin: 0; }
}

.btn-continue {
  width: 100%;
  padding: 14px;
  border: 1.5px solid rgba(180, 140, 80, 0.35);
  border-radius: 12px;
  background: rgba(180, 140, 80, 0.08);
  color: #d4b880;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: rgba(180, 140, 80, 0.15); transform: translateY(-1px); }
}
</style>
