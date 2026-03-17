<template>
  <MainLayout>
    <div class="camp-explore">
      <!-- 顶部工具栏 -->
      <div class="camp-toolbar">
        <button class="btn-back" @click="goBack">← 返回大厅</button>
        <div class="camp-stats">
          <span class="stat">🐺 击败: {{ defeatedCount }}/{{ totalMonsters }}</span>
          <span class="stat">💎 宝箱: {{ openedChests }}/{{ totalChests }}</span>
          <span class="stat combo-stat" v-if="comboCount >= 2">🔥 连击: ×{{ comboCount }}</span>
        </div>
        <button class="btn-skip" @click="skipToCombat">跳过探索 →</button>
      </div>

      <!-- Phaser 游戏容器 -->
      <div ref="gameContainer" class="game-container"></div>

      <!-- 单词怪物遭遇弹窗 -->
      <WordEncounterDialog
        v-if="currentEncounter && currentEncounter.type === 'monster'"
        :encounter="currentEncounter"
        :combo="comboCount"
        @resolve="onEncounterResolve"
        @close="onEncounterClose"
      />

      <!-- NPC 全屏 Visual Novel 对话 -->
      <Teleport to="body">
        <div v-if="currentEncounter && currentEncounter.type === 'npc'" class="vn-overlay" @click="advanceNpc">
          <!-- 暗色场景背景 -->
          <div class="vn-backdrop"></div>

          <!-- NPC 大型立绘 -->
          <div class="vn-portrait" :class="'portrait-' + (currentEncounter.npcAvatar ?? 'sage')">
            <NpcPortrait :type="(currentEncounter.npcAvatar as any) ?? 'sage'" />
          </div>

          <!-- 底部文本框 -->
          <div class="vn-textbox" @click.stop>
            <div class="vn-nameplate">
              {{ currentEncounter.npcName ?? 'NPC' }}
            </div>
            <p class="vn-text">{{ vnDisplayedText }}<span class="vn-cursor" v-if="vnTyping">▍</span></p>
            <div class="vn-footer">
              <span class="vn-progress">{{ npcLineIndex + 1 }} / {{ currentEncounter.npcLines?.length ?? 0 }}</span>
              <button class="vn-next-btn" @click="advanceNpc">
                {{ isLastNpcLine ? '✓ 关闭' : '继续 →' }}
              </button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 宝箱弹窗 -->
      <Teleport to="body">
        <div v-if="currentEncounter && currentEncounter.type === 'treasure'" class="treasure-overlay">
          <div class="treasure-dialog">
            <div class="treasure-glow"></div>
            <div class="treasure-icon">💎</div>
            <h3>发现宝箱！</h3>
            <div class="treasure-rewards">
              <div class="reward-badge">✨ +{{ currentEncounter.reward?.xp ?? 0 }} XP</div>
              <div class="reward-badge">💰 +{{ currentEncounter.reward?.coins ?? 0 }} 金币</div>
            </div>
            <button ref="collectBtn" class="btn-collect" @click="collectTreasure">🎁 收取奖励</button>
          </div>
        </div>
      </Teleport>

      <!-- 图鉴按钮 -->
      <button class="btn-codex" @click="showCodex = !showCodex" title="图鉴">
        📖
      </button>

      <!-- 连击特效覆盖 -->
      <Teleport to="body">
        <div v-if="showComboOverlay" class="combo-overlay">
          <div class="combo-text">{{ comboOverlayText }}</div>
        </div>
      </Teleport>

      <!-- 图鉴面板 -->
      <div v-if="showCodex" class="codex-overlay" @click="showCodex = false">
        <div class="codex-panel dark-panel" @click.stop>
          <h3 class="codex-title">📖 单词图鉴</h3>
          <div class="codex-list" v-if="collectedWords.length > 0">
            <div v-for="word in collectedWords" :key="word.id" class="codex-item" :class="'grade-' + getWordGrade(word.id)">
              <div class="codex-grade-badge">{{ getWordGrade(word.id) === 'gold' ? '🥇' : getWordGrade(word.id) === 'silver' ? '🥈' : '🥉' }}</div>
              <div class="codex-word">{{ word.wordEn }}</div>
              <div class="codex-phonetic">{{ word.phonetic }}</div>
              <div class="codex-meaning">{{ word.wordZh }}</div>
              <div class="codex-sentence">{{ word.sentence }}</div>
            </div>
          </div>
          <p v-else class="codex-empty">还没有收集到单词，去击败怪物吧！</p>
          <button class="btn-close-codex" @click="showCodex = false">关闭</button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import WordEncounterDialog from '@/components/ui/WordEncounterDialog.vue'
import NpcPortrait from '@/components/ui/NpcPortrait.vue'
import { createPhaserGame, destroyPhaserGame, getCampScene } from '@/game/PhaserGame'
import type { MapEncounter } from '@/game/config/mapData'
import { useChapterStore } from '@/stores/chapter'
import { useUserStore } from '@/stores/user'
import { useSrsStore } from '@/stores/srs'
import { useAchievementStore } from '@/stores/achievements'
import { useSound } from '@/composables/useSound'
import { grantReward } from '@/composables/useReward'

const CAMP_STATE_KEY = 'eqa_camp_state'

function saveCampState(playerX: number, playerY: number, revealedTiles: string[]) {
  const chapterCode = chapterStore.currentChapterCode
  const userId = userStore.user?.id ?? 0
  const key = `${CAMP_STATE_KEY}_${userId}_${chapterCode}`
  localStorage.setItem(key, JSON.stringify({ playerX, playerY, revealedTiles }))
}

function loadCampState(): { playerX: number; playerY: number; revealedTiles: string[] } | null {
  const chapterCode = chapterStore.currentChapterCode
  const userId = userStore.user?.id ?? 0
  const key = `${CAMP_STATE_KEY}_${userId}_${chapterCode}`
  try {
    const raw = localStorage.getItem(key)
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return null
}

const router = useRouter()
const sound = useSound()
const chapterStore = useChapterStore()
const userStore = useUserStore()
const srsStore = useSrsStore()
const achievementStore = useAchievementStore()

const gameContainer = ref<HTMLElement>()
const currentEncounter = ref<MapEncounter | null>(null)
const showCodex = ref(false)
const collectedWords = ref<MapEncounter[]>([])
const npcLineIndex = ref(0)
const comboCount = ref(0)
const showComboOverlay = ref(false)
const comboOverlayText = ref('')
const wordAttempts = ref<Map<string, number>>(new Map())
const collectBtn = ref<HTMLButtonElement>()

const campMap = computed(() => chapterStore.currentCampMap)
const totalMonsters = computed(() => campMap.value.encounters.filter(e => e.type === 'monster').length)
const totalChests = computed(() => campMap.value.encounters.filter(e => e.type === 'treasure').length)

const defeatedCount = computed(() => chapterStore.currentProgress?.campDefeated?.length ?? 0)
const openedChests = computed(() =>
  chapterStore.currentProgress?.campOpened?.length ?? 0
)

const npcCurrentLine = computed(() => {
  if (!currentEncounter.value?.npcLines) return ''
  return currentEncounter.value.npcLines[npcLineIndex.value] ?? ''
})

const isLastNpcLine = computed(() => {
  if (!currentEncounter.value?.npcLines) return true
  return npcLineIndex.value >= currentEncounter.value.npcLines.length - 1
})

// ---- VN 打字机效果 ----
const vnDisplayedText = ref('')
const vnTyping = ref(false)
let vnTimer: ReturnType<typeof setInterval> | null = null

function startTypewriter(text: string) {
  if (vnTimer) clearInterval(vnTimer)
  vnDisplayedText.value = ''
  vnTyping.value = true
  let i = 0
  vnTimer = setInterval(() => {
    if (i < text.length) {
      vnDisplayedText.value += text[i++]
    } else {
      vnTyping.value = false
      if (vnTimer) { clearInterval(vnTimer); vnTimer = null }
    }
  }, 35)
}

function skipTypewriter() {
  if (vnTimer) { clearInterval(vnTimer); vnTimer = null }
  vnDisplayedText.value = npcCurrentLine.value
  vnTyping.value = false
}

watch(npcCurrentLine, (text) => {
  if (text) startTypewriter(text)
})

onMounted(async () => {
  window.addEventListener('keydown', handleDialogKeydown)

  // 确保章节进度已加载（刷新页面时需要从后端拉取）
  await chapterStore.loadChapters()

  if (gameContainer.value) {
    // 从进度中恢复已击败/已开启状态
    const prog = chapterStore.currentProgress
    if (prog) {
      for (const enc of campMap.value.encounters) {
        if (prog.campDefeated.includes(enc.id) || prog.campOpened.includes(enc.id)) {
          enc.defeated = true
          if (enc.type === 'monster') {
            collectedWords.value.push({ ...enc })
          }
        }
      }
    }

    // 恢复玩家位置和迷雾
    const saved = loadCampState()

    createPhaserGame(gameContainer.value, campMap.value)

    // 等待场景就绪后绑定回调 + 恢复状态
    const checkScene = setInterval(() => {
      const scene = getCampScene()
      if (scene) {
        scene.onEncounter = handleEncounter
        if (saved) {
          scene.restoreState(saved.playerX, saved.playerY, saved.revealedTiles)
        }
        clearInterval(checkScene)
      }
    }, 200)
  }
})

onBeforeUnmount(() => {
  // 保存玩家位置和迷雾状态
  const scene = getCampScene()
  if (scene) {
    const state = scene.getState()
    saveCampState(state.playerX, state.playerY, state.revealedTiles)
  }
  window.removeEventListener('keydown', handleDialogKeydown)
  destroyPhaserGame()
  if (vnTimer) { clearInterval(vnTimer); vnTimer = null }
})

function handleEncounter(encounter: MapEncounter) {
  currentEncounter.value = encounter
  npcLineIndex.value = 0
  // 如果是NPC，启动打字机
  if (encounter.type === 'npc' && encounter.npcLines?.[0]) {
    startTypewriter(encounter.npcLines[0])
  }
  // 宝箱弹窗自动聚焦收取按钮
  if (encounter.type === 'treasure') {
    nextTick(() => collectBtn.value?.focus())
  }
  sound.click()
}

function onEncounterResolve(result: { id: string; success: boolean }) {
  const scene = getCampScene()
  if (scene) {
    scene.resolveEncounter(result.id, result.success)
  }

  // 追踪尝试次数
  const attempts = (wordAttempts.value.get(result.id) ?? 0) + 1
  wordAttempts.value.set(result.id, attempts)

  if (result.success && currentEncounter.value?.type === 'monster') {
    comboCount.value++
    collectedWords.value.push({ ...currentEncounter.value })
    chapterStore.defeatCampMonster(result.id)

    // 奖励: XP + 金币（使用怪物配置的奖励值）
    const encReward = currentEncounter.value.reward
    import('@/utils/debugLog').then(m => m.debugLogs.push(`encounter[${result.id}] reward=${JSON.stringify(encReward)} diff=${currentEncounter.value?.difficulty}`))
    grantReward(true, comboCount.value, 'camp', {
      baseXp: encReward?.xp,
      baseCoins: encReward?.coins,
    })
    // SRS: 将打败的单词加入复习
    const wordCode = 'W_' + (currentEncounter.value.wordEn || '').toUpperCase().replace(/\s+/g, '_')
    if (wordCode !== 'W_') {
      srsStore.reviewCard(wordCode, 4)
    }

    // 成就: 词汇 + 连击
    achievementStore.updateProgress('words_learned', defeatedCount.value + 1)
    achievementStore.updateProgress('perfect_combo', comboCount.value)

    sound.correct()

    // 连击 >= 2 时显示 combo 特效
    if (comboCount.value >= 2) {
      comboOverlayText.value = `🔥 COMBO ×${comboCount.value}`
      showComboOverlay.value = true
      sound.combo()
      setTimeout(() => { showComboOverlay.value = false }, 1200)
    }
  } else {
    comboCount.value = 0
    sound.wrong()
  }
  currentEncounter.value = null
}

function onEncounterClose() {
  const scene = getCampScene()
  if (scene && currentEncounter.value) {
    // 关闭但不算击败 — 让玩家可以再次尝试
    scene.resolveEncounter(currentEncounter.value.id, false)
  }
  currentEncounter.value = null
}

function advanceNpc() {
  if (!currentEncounter.value?.npcLines) return
  // 如果正在打字，先跳过打字动画
  if (vnTyping.value) {
    skipTypewriter()
    return
  }
  if (isLastNpcLine.value) {
    // 完成NPC对话
    const scene = getCampScene()
    if (scene) {
      scene.resolveEncounter(currentEncounter.value.id, true)
    }
    currentEncounter.value = null
  } else {
    npcLineIndex.value++
  }
}

function collectTreasure() {
  const scene = getCampScene()
  if (scene && currentEncounter.value) {
    scene.resolveEncounter(currentEncounter.value.id, true)
    chapterStore.openCampChest(currentEncounter.value.id)
    sound.correct()
  }
  currentEncounter.value = null
}

function goBack() {
  router.push('/dashboard')
}

function skipToCombat() {
  router.push('/quest')
}

function getWordGrade(id: string): 'gold' | 'silver' | 'bronze' {
  const attempts = wordAttempts.value.get(id) ?? 1
  return attempts === 1 ? 'gold' : attempts <= 2 ? 'silver' : 'bronze'
}

function handleDialogKeydown(e: KeyboardEvent) {
  if (e.key !== 'Enter' && e.key !== ' ') return
  if (!currentEncounter.value) return
  // NPC 对话: Enter/Space 推进
  if (currentEncounter.value?.type === 'npc') {
    e.preventDefault()
    advanceNpc()
  }
  // 宝箱: Enter/Space 收取
  if (currentEncounter.value?.type === 'treasure') {
    e.preventDefault()
    collectTreasure()
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.camp-explore {
  position: relative;
  height: calc(100vh - 48px);
  overflow: hidden;
}

.camp-toolbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: linear-gradient(180deg, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0.3) 70%, transparent 100%);
  z-index: 20;
  pointer-events: none;

  > * {
    pointer-events: auto;
  }

  .btn-back, .btn-skip {
    padding: 6px 14px;
    border: 1px solid rgba(180, 140, 80, 0.35);
    border-radius: 8px;
    background: rgba(0, 0, 0, 0.5);
    color: #ddd;
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;
    backdrop-filter: blur(4px);
    &:hover {
      background: rgba(180, 140, 80, 0.2);
      color: #ffd700;
    }
  }

  .camp-stats {
    display: flex;
    gap: 16px;
    padding: 4px 12px;
    border-radius: 8px;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(4px);
    .stat {
      font-size: 13px;
      color: #ccc;
    }
    .combo-stat {
      color: #f59e0b;
      font-weight: bold;
      animation: comboPulse 0.6s ease infinite alternate;
    }
  }
}

@keyframes comboPulse {
  from { transform: scale(1); }
  to { transform: scale(1.08); }
}

.game-container {
  position: absolute;
  inset: 0;
  background: #0a0a0a;
  overflow: hidden;

  :deep(canvas) {
    display: block;
    width: 100% !important;
    height: 100% !important;
  }
}

// 触控摇杆
.touch-controls {
  position: absolute;
  bottom: 30px;
  left: 30px;
  z-index: 20;

  .dpad {
    display: grid;
    grid-template-areas:
      '. up .'
      'left . right'
      '. down .';
    gap: 4px;
  }

  .dpad-btn {
    width: 50px;
    height: 50px;
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    background: rgba(0, 0, 0, 0.6);
    color: #ddd;
    font-size: 18px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;

    &:active {
      background: rgba(255, 215, 0, 0.3);
    }
  }

  .dpad-up { grid-area: up; }
  .dpad-left { grid-area: left; }
  .dpad-right { grid-area: right; }
  .dpad-down { grid-area: down; }
}

// ===== Visual Novel 全屏对话 =====
.vn-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  cursor: pointer;
}

.vn-backdrop {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center bottom, rgba(10, 8, 30, 0.6) 0%, rgba(0, 0, 0, 0.92) 100%);
  backdrop-filter: blur(6px);
  animation: vnFadeIn 0.3s ease;
}

@keyframes vnFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.vn-portrait {
  position: absolute;
  bottom: 180px;
  left: 50%;
  transform: translateX(-50%);
  animation: vnPortraitEnter 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);

  &.portrait-sage {
    filter: drop-shadow(0 0 30px rgba(120, 80, 220, 0.4)) drop-shadow(0 0 60px rgba(120, 80, 220, 0.15));
  }
  &.portrait-guide {
    filter: drop-shadow(0 0 30px rgba(60, 180, 80, 0.4)) drop-shadow(0 0 60px rgba(60, 180, 80, 0.15));
  }
  &.portrait-knight {
    filter: drop-shadow(0 0 30px rgba(200, 80, 40, 0.4)) drop-shadow(0 0 60px rgba(200, 80, 40, 0.15));
  }
  &.portrait-merchant {
    filter: drop-shadow(0 0 30px rgba(200, 160, 80, 0.4)) drop-shadow(0 0 60px rgba(200, 160, 80, 0.15));
  }
}

@keyframes vnPortraitEnter {
  from {
    transform: translateX(-50%) translateY(30px) scale(0.9);
    opacity: 0;
  }
  to {
    transform: translateX(-50%) translateY(0) scale(1);
    opacity: 1;
  }
}

.vn-textbox {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px 24px 32px;
  background: linear-gradient(to top, rgba(10, 8, 30, 0.97) 0%, rgba(10, 8, 30, 0.92) 60%, rgba(10, 8, 30, 0.7) 100%);
  border-top: 2px solid rgba(100, 120, 200, 0.35);
  cursor: default;
  animation: vnTextboxIn 0.4s ease 0.15s both;
}

@keyframes vnTextboxIn {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.vn-nameplate {
  display: inline-block;
  padding: 5px 18px;
  background: linear-gradient(135deg, #5544aa, #3322aa);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 800;
  color: #e8e0ff;
  margin-bottom: 12px;
  letter-spacing: 1px;
  box-shadow: 0 2px 12px rgba(80, 60, 180, 0.3);
}

.vn-text {
  font-size: 17px;
  line-height: 1.9;
  color: #e0e0f0;
  min-height: 52px;
  margin: 0;
  letter-spacing: 0.3px;
}

.vn-cursor {
  color: rgba(140, 160, 255, 0.8);
  animation: vnBlink 0.6s steps(2) infinite;
}

@keyframes vnBlink {
  50% { opacity: 0; }
}

.vn-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.vn-progress {
  font-size: 12px;
  color: #555;
  background: rgba(255, 255, 255, 0.04);
  padding: 3px 12px;
  border-radius: 10px;
}

.vn-next-btn {
  padding: 10px 22px;
  border-radius: 10px;
  border: 1.5px solid rgba(140, 160, 255, 0.35);
  background: rgba(140, 160, 255, 0.08);
  color: #99aaff;
  cursor: pointer;
  white-space: nowrap;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s;
  &:hover {
    background: rgba(140, 160, 255, 0.18);
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(100, 120, 255, 0.15);
  }
}

// 宝箱弹窗
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.treasure-overlay {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(0,0,0,0.5) 0%, rgba(0,0,0,0.8) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
  backdrop-filter: blur(3px);
}

.treasure-dialog {
  text-align: center;
  padding: 36px 44px;
  border: 2px solid rgba(218, 165, 32, 0.5);
  border-radius: 20px;
  background: linear-gradient(170deg, #1a1608 0%, #100e04 100%);
  box-shadow:
    0 0 50px rgba(255, 215, 0, 0.1),
    0 20px 60px rgba(0,0,0,0.5);
  animation: treasureIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;

  .treasure-glow {
    position: absolute;
    top: -40px;
    left: 50%;
    transform: translateX(-50%);
    width: 120px;
    height: 120px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(255,215,0,0.12) 0%, transparent 70%);
    animation: glowPulse 2s ease-in-out infinite;
  }

  .treasure-icon {
    font-size: 56px;
    margin-bottom: 12px;
    position: relative;
    animation: treasureBounce 0.6s ease 0.3s;
  }

  h3 {
    color: #ffd700;
    margin: 0 0 18px;
    font-size: 22px;
    font-weight: 700;
    text-shadow: 0 0 20px rgba(255, 215, 0, 0.2);
  }

  .treasure-rewards {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-bottom: 22px;
  }

  .reward-badge {
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 15px;
    font-weight: 600;
    background: rgba(255, 200, 60, 0.1);
    border: 1px solid rgba(255, 200, 60, 0.3);
    color: #ffd700;
  }

  .btn-collect {
    padding: 12px 32px;
    border-radius: 12px;
    border: none;
    background: linear-gradient(135deg, #b8860b, #8b6914);
    color: #fff;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 4px 15px rgba(184, 134, 11, 0.3);
    &:hover {
      background: linear-gradient(135deg, #d4a017, #b8860b);
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(184, 134, 11, 0.4);
    }
    &:active {
      transform: translateY(1px);
    }
  }
}

@keyframes treasureIn {
  from { transform: scale(0.7); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

@keyframes treasureBounce {
  0% { transform: scale(1); }
  40% { transform: scale(1.25); }
  70% { transform: scale(0.95); }
  100% { transform: scale(1); }
}

@keyframes glowPulse {
  0%, 100% { transform: translateX(-50%) scale(1); opacity: 0.6; }
  50% { transform: translateX(-50%) scale(1.2); opacity: 1; }
}

// 图鉴按钮
.btn-codex {
  position: absolute;
  top: 60px;
  right: 16px;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  border: 1.5px solid rgba(180, 140, 80, 0.35);
  background: rgba(0, 0, 0, 0.65);
  font-size: 22px;
  cursor: pointer;
  z-index: 20;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  &:hover {
    background: rgba(180, 140, 80, 0.15);
    transform: scale(1.08);
    border-color: rgba(180, 140, 80, 0.5);
  }
}

// 图鉴面板
.codex-overlay {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(0,0,0,0.5) 0%, rgba(0,0,0,0.8) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
  backdrop-filter: blur(3px);
}

.codex-panel {
  width: 92vw;
  max-width: 520px;
  max-height: 75vh;
  overflow-y: auto;
  padding: 0 24px 24px;
  border: 2px solid rgba(180, 140, 80, 0.4);
  border-radius: 20px;
  background: linear-gradient(170deg, #14120e 0%, #0c0a06 100%);
  box-shadow:
    0 0 40px rgba(180, 140, 80, 0.1),
    0 20px 60px rgba(0,0,0,0.5);
  animation: dialogIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

  .codex-title {
    color: #d4b880;
    margin: 0;
    padding: 18px 0 14px;
    text-align: center;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 2px;
    border-bottom: 1px solid rgba(180, 140, 80, 0.15);
    position: sticky;
    top: 0;
    background: linear-gradient(170deg, #14120e 0%, #0c0a06 100%);
    z-index: 1;
  }

  .codex-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding-top: 14px;
  }

  .codex-item {
    padding: 14px 16px;
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.02);
    transition: all 0.2s;
    position: relative;

    &:hover {
      background: rgba(180, 140, 80, 0.04);
      border-color: rgba(180, 140, 80, 0.15);
    }

    &.grade-gold { border-left: 3px solid #ffd700; }
    &.grade-silver { border-left: 3px solid #c0c0c0; }
    &.grade-bronze { border-left: 3px solid #cd7f32; }

    .codex-grade-badge {
      position: absolute;
      top: 10px;
      right: 12px;
      font-size: 16px;
    }

    .codex-word {
      font-size: 18px;
      font-weight: 700;
      color: #a8d8ff;
    }
    .codex-phonetic {
      font-size: 13px;
      color: #777;
      margin: 3px 0;
      font-style: italic;
    }
    .codex-meaning {
      font-size: 15px;
      color: #d0d0d0;
    }
    .codex-sentence {
      font-size: 13px;
      color: #888;
      margin-top: 6px;
      font-style: italic;
      padding-left: 10px;
      border-left: 2px solid rgba(180, 140, 80, 0.2);
    }
  }

  .codex-empty {
    text-align: center;
    color: #666;
    padding: 32px 0;
    font-size: 14px;
  }

  .btn-close-codex {
    display: block;
    margin: 18px auto 0;
    padding: 10px 28px;
    border-radius: 10px;
    border: 1.5px solid rgba(180, 140, 80, 0.3);
    background: rgba(180, 140, 80, 0.06);
    color: #d4b880;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    &:hover {
      background: rgba(180, 140, 80, 0.14);
      transform: translateY(-1px);
    }
  }
}

@keyframes dialogIn {
  from { transform: translateY(30px) scale(0.95); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}
</style>

<style lang="scss">
/* combo 特效覆盖层 (非 scoped，因为在 teleport body 上) */
.combo-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  pointer-events: none;
  animation: comboFade 1.2s ease forwards;
}
.combo-text {
  font-size: 48px;
  font-weight: 900;
  color: #ffd700;
  text-shadow:
    0 0 20px rgba(255, 80, 0, 0.6),
    0 0 40px rgba(255, 180, 0, 0.4),
    0 4px 8px rgba(0, 0, 0, 0.5);
  letter-spacing: 4px;
  animation: comboZoom 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes comboFade {
  0%, 70% { opacity: 1; }
  100% { opacity: 0; }
}
@keyframes comboZoom {
  from { transform: scale(0.3) rotate(-8deg); }
  to { transform: scale(1) rotate(0); }
}

</style>
