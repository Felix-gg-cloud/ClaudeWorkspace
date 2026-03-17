<template>
  <MainLayout>
    <div class="boss-page">
      <!-- Pre-battle -->
      <div v-if="phase === 'pre'" class="pre-battle">
        <div class="boss-intro dark-panel">
          <BossSvg
            :tier="bossTier"
            :name="bossName"
            :currentHp="bossMaxHp"
            :maxHp="bossMaxHp"
            state="idle"
          />
          <div class="intro-info">
            <h2>💀 Boss 出现了！</h2>
            <p class="intro-desc">{{ bossName }} 挡在你面前，准备好迎战了吗？</p>
            <div class="intro-stats">
              <span>❤️ HP: {{ bossMaxHp }}</span>
              <span>⚔️ 伤害: {{ bossDmg }}</span>
              <span>🔥 连击阈值: {{ comboThreshold }}</span>
            </div>
            <button class="btn-danger" @click="startBattle">⚔️ 开始战斗</button>
          </div>
        </div>
      </div>

      <!-- Battle phase -->
      <div v-if="phase === 'battle'" class="battle-area">
        <!-- Dual HP bars -->
        <div class="dual-hp">
          <div class="hp-row player-hp">
            <span class="hp-label">🧙 你</span>
            <div class="hp-bar hp-bar--green">
              <div class="hp-bar__fill" :style="{ width: playerHpPercent + '%' }"></div>
              <span class="hp-bar__text">{{ playerHp }} / {{ playerMaxHp }}</span>
            </div>
          </div>
          <div class="hp-row boss-hp-row">
            <span class="hp-label">💀 {{ bossName }}</span>
            <div class="hp-bar hp-bar--red">
              <div class="hp-bar__fill" :style="{ width: bossHpPercent + '%' }"></div>
              <span class="hp-bar__text">{{ bossHp }} / {{ bossMaxHp }}</span>
            </div>
          </div>
        </div>

        <!-- Boss display -->
        <div class="battle-boss">
          <BossSvg
            :tier="bossTier"
            :name="bossName"
            :currentHp="bossHp"
            :maxHp="bossMaxHp"
            :state="bossState"
          />

          <!-- Damage numbers -->
          <transition-group name="dmg-anim" tag="div" class="dmg-numbers">
            <div
              v-for="d in damageFloats"
              :key="d.id"
              class="dmg-num"
              :class="d.type"
            >
              {{ d.text }}
            </div>
          </transition-group>
        </div>

        <!-- Combo display -->
        <div v-if="combo > 0" class="battle-combo" :class="{ 'combo-hot': combo >= comboThreshold }">
          <span class="combo-x">x{{ combo }}</span>
          <span class="combo-word">COMBO</span>
        </div>

        <!-- Question card (reuse same components as Quest) -->
        <transition name="page-fade" mode="out-in">
          <component
            :is="taskComponent"
            :key="currentQuestion.code"
            :task="currentQuestion"
            @answer="onCardAnswer"
          />
        </transition>
      </div>

      <!-- Victory -->
      <div v-if="phase === 'victory'" class="victory-area">
        <div class="victory-card dark-panel">
          <div class="vic-icon">🏆</div>
          <h2 class="text-gold">Boss 被击败了！</h2>
          <p>{{ bossName }} 倒下了</p>
          <div class="vic-rewards">
            <span class="text-gold">💰 +50 金币</span>
            <span class="text-fire">⭐ +100 XP</span>
          </div>
          <button class="btn-gold" @click="$router.push('/dashboard')">返回大厅</button>
        </div>
      </div>

      <!-- Defeat -->
      <div v-if="phase === 'defeat'" class="defeat-area">
        <div class="defeat-card dark-panel">
          <div class="def-icon">💀</div>
          <h2 style="color: #dc2626;">战败...</h2>
          <p>{{ bossName }} 获胜了，下次再来吧！</p>
          <button class="btn-ghost" @click="resetBattle">🔄 重新挑战</button>
          <button class="btn-gold" style="margin-top: 8px;" @click="$router.push('/dashboard')">返回大厅</button>
        </div>
      </div>

      <!-- Screen flash effects -->
      <div v-if="flashType" class="screen-flash" :class="'flash-' + flashType"></div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import BossSvg from '@/components/boss/BossSvg.vue'
import { useUserStore } from '@/stores/user'
import { useChapterStore } from '@/stores/chapter'
import { useSound } from '@/composables/useSound'
import { resolveTaskComponent } from '@/composables/useTaskCard'
import type { Task } from '@/types'

const userStore = useUserStore()
const chapterStore = useChapterStore()
const sound = useSound()

const dataLoading = ref(true)

onMounted(async () => {
  await chapterStore.loadBossData(chapterStore.currentChapterCode)
  dataLoading.value = false
  bossHp.value = bossConfig.value.bossHp
})

const bossConfig = computed(() => chapterStore.currentBossConfig)
const bossTier = computed(() => bossConfig.value.tier)
const bossName = computed(() => bossConfig.value.bossName)
const bossMaxHp = computed(() => bossConfig.value.bossHp)
const bossDmg = computed(() => bossConfig.value.bossDamage)
const comboThreshold = computed(() => bossConfig.value.comboThreshold)

const bossHp = ref(bossConfig.value.bossHp)
const playerMaxHp = ref(10)
const playerHp = ref(10)
const combo = ref(0)
const phase = ref<'pre' | 'battle' | 'victory' | 'defeat'>('pre')
const bossState = ref<'idle' | 'attack' | 'hurt' | 'defeat'>('idle')
const roundDone = ref(false)
const questionIdx = ref(0)
const flashType = ref<'' | 'red' | 'white'>('')

const damageFloats = ref<{ id: number; text: string; type: string }[]>([])
let dmgId = 0

const bossHpPercent = computed(() => Math.max(0, (bossHp.value / bossMaxHp.value) * 100))
const playerHpPercent = computed(() => Math.max(0, (playerHp.value / playerMaxHp.value) * 100))



const allQuestions = ref<Task[]>([...chapterStore.currentBossTasks])
const currentQuestion = computed(() => allQuestions.value[questionIdx.value % allQuestions.value.length])
const taskComponent = computed(() => resolveTaskComponent(currentQuestion.value))

function startBattle() {
  phase.value = 'battle'
  sound.click()
}

function onCardAnswer(isCorrect: boolean) {
  if (roundDone.value) return
  roundDone.value = true

  if (isCorrect) {
    combo.value++
    const dmg = combo.value >= comboThreshold.value ? 2 : 1
    bossHp.value = Math.max(0, bossHp.value - dmg)

    // Boss hurt
    bossState.value = 'hurt'
    sound.bossHit()
    addDmgFloat(`-${dmg}`, 'dmg-boss')

    setTimeout(() => { bossState.value = 'idle' }, 400)

    if (bossHp.value <= 0) {
      setTimeout(() => {
        bossState.value = 'defeat'
        sound.victory()
        userStore.addXp(100)
        userStore.addCoins(50)
        chapterStore.defeatBoss()
        flashType.value = 'white'
        setTimeout(() => { flashType.value = '' }, 400)
        setTimeout(() => { phase.value = 'victory' }, 1000)
      }, 500)
      return
    }
  } else {
    combo.value = 0
    // Boss attacks player
    bossState.value = 'attack'
    sound.playerHit()
    playerHp.value = Math.max(0, playerHp.value - bossDmg.value)
    addDmgFloat(`-${bossDmg.value}`, 'dmg-player')

    flashType.value = 'red'
    setTimeout(() => { flashType.value = '' }, 300)
    setTimeout(() => { bossState.value = 'idle' }, 400)

    if (playerHp.value <= 0) {
      setTimeout(() => {
        sound.defeat()
        phase.value = 'defeat'
      }, 600)
      return
    }
  }

  // Next question
  setTimeout(() => {
    questionIdx.value++
    roundDone.value = false
  }, 700)
}

function addDmgFloat(text: string, type: string) {
  const id = ++dmgId
  damageFloats.value.push({ id, text, type })
  setTimeout(() => {
    damageFloats.value = damageFloats.value.filter(d => d.id !== id)
  }, 1000)
}

function resetBattle() {
  bossHp.value = bossConfig.value.bossHp
  playerHp.value = playerMaxHp.value
  combo.value = 0
  questionIdx.value = 0
  roundDone.value = false
  bossState.value = 'idle'
  phase.value = 'pre'
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.boss-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px 20px 60px;
  position: relative;
}

// Pre-battle
.pre-battle {
  display: flex;
  justify-content: center;
}

.boss-intro {
  text-align: center;
  padding: 32px;
  animation: scale-in 0.5s $ease-bounce;
}

.intro-info {
  margin-top: 16px;

  h2 { font-size: 22px; margin-bottom: 6px; }
}

.intro-desc {
  color: $text-secondary;
  font-size: 14px;
  margin-bottom: 16px;
}

.intro-stats {
  display: flex;
  justify-content: center;
  gap: 16px;
  font-size: 13px;
  color: $text-muted;
  margin-bottom: 20px;
}

// Battle
.dual-hp {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.hp-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hp-label {
  font-size: 13px;
  font-weight: 600;
  width: 100px;
  color: $text-secondary;
}

.battle-boss {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
  position: relative;
}

.dmg-numbers {
  position: absolute;
  top: 0;
  left: 50%;
}

.dmg-num {
  position: absolute;
  font-size: 24px;
  font-weight: 900;
  animation: damage-number 1s ease-out forwards;
  white-space: nowrap;

  &.dmg-boss { color: $gold; left: 30px; }
  &.dmg-player { color: $hp-red; left: -60px; }
}

.dmg-anim-enter-active {
  animation: damage-number 1s ease-out forwards;
}

// Combo
.battle-combo {
  text-align: center;
  margin-bottom: 12px;
  animation: combo-pop 0.3s $ease-bounce;
}

.combo-x {
  font-size: 36px;
  font-weight: 900;
  color: $fire-orange;
}

.combo-word {
  font-size: 14px;
  font-weight: 700;
  color: $text-muted;
  margin-left: 4px;
}

.combo-hot .combo-x {
  color: $hp-red;
  text-shadow: 0 0 20px rgba(220, 38, 38, 0.5);
}

// Victory / Defeat
.victory-area, .defeat-area {
  display: flex;
  justify-content: center;
  padding-top: 32px;
}

.victory-card, .defeat-card {
  text-align: center;
  padding: 40px;
  min-width: 320px;
  animation: scale-in 0.5s $ease-bounce;

  h2 { font-size: 28px; margin: 12px 0 8px; }
  p { color: $text-secondary; margin-bottom: 16px; }
}

.vic-icon, .def-icon {
  font-size: 56px;
  animation: float 2s ease-in-out infinite;
}

.vic-rewards {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 24px;
}

// Screen flash
.screen-flash {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 200;

  &.flash-red {
    background: rgba(220, 38, 38, 0.15);
    animation: screen-flash-red 0.3s ease-out forwards;
  }

  &.flash-white {
    background: rgba(255, 255, 255, 0.2);
    animation: screen-flash-white 0.4s ease-out forwards;
  }
}
</style>
