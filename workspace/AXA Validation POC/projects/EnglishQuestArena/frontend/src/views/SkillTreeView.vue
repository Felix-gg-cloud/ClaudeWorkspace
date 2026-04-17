<template>
  <MainLayout>
    <div class="skill-page">
      <h2 class="page-title">🌟 技能星图</h2>
      <p class="page-desc">解锁星座节点，提升冒险能力</p>

      <div class="skill-info dark-panel">
        <span>可用技能点：</span>
        <span class="text-gold skill-pts">✨ {{ userStore.user?.skillPoints ?? 0 }}</span>
      </div>

      <!-- SVG Constellation Map -->
      <div class="constellation-wrapper dark-panel">
        <svg
          viewBox="0 0 800 500"
          class="constellation-svg"
          ref="svgRef"
        >
          <!-- Background stars -->
          <g class="bg-stars">
            <circle
              v-for="star in bgStars"
              :key="star.id"
              :cx="star.x"
              :cy="star.y"
              :r="star.r"
              fill="#fff"
              :opacity="star.opacity"
              class="twinkle"
              :style="{ animationDelay: star.delay + 's' }"
            />
          </g>

          <!-- Connection lines -->
          <g class="connections">
            <line
              v-for="conn in connections"
              :key="conn.from + '-' + conn.to"
              :x1="getNodePos(conn.from).x"
              :y1="getNodePos(conn.from).y"
              :x2="getNodePos(conn.to).x"
              :y2="getNodePos(conn.to).y"
              :stroke="connColor(conn)"
              stroke-width="1.5"
              :stroke-dasharray="connDash(conn)"
              :opacity="connOpacity(conn)"
            />
          </g>

          <!-- Nodes -->
          <g
            v-for="node in skillNodes"
            :key="node.code"
            class="skill-node"
            :class="{
              'node-unlocked': node.unlocked,
              'node-locked': !node.unlocked && !canUnlock(node),
              'node-available': !node.unlocked && canUnlock(node)
            }"
            @click="tryUnlock(node)"
            :style="{ cursor: canUnlock(node) && !node.unlocked ? 'pointer' : 'default' }"
          >
            <!-- Outer glow ring -->
            <circle
              v-if="node.unlocked"
              :cx="node.x"
              :cy="node.y"
              :r="32"
              :fill="branchGlow(node.branch)"
              class="node-glow"
            />

            <!-- Available pulse -->
            <circle
              v-if="!node.unlocked && canUnlock(node)"
              :cx="node.x"
              :cy="node.y"
              :r="28"
              fill="none"
              :stroke="branchColor(node.branch)"
              stroke-width="1"
              class="pulse-ring"
            />

            <!-- Node circle -->
            <circle
              :cx="node.x"
              :cy="node.y"
              :r="22"
              :fill="nodeFill(node)"
              :stroke="nodeStroke(node)"
              stroke-width="2"
            />

            <!-- Icon -->
            <text
              :x="node.x"
              :y="node.y + 1"
              text-anchor="middle"
              dominant-baseline="central"
              font-size="16"
              :opacity="node.unlocked ? 1 : 0.4"
            >
              {{ nodeIcon(node) }}
            </text>

            <!-- Label -->
            <text
              :x="node.x"
              :y="node.y + 38"
              text-anchor="middle"
              :fill="node.unlocked ? '#e5e7eb' : '#6b7280'"
              font-size="11"
              font-weight="600"
            >
              {{ node.nameZh }}
            </text>

            <!-- Cost badge (available nodes) -->
            <g v-if="!node.unlocked && canUnlock(node)">
              <rect
                :x="node.x + 12"
                :y="node.y - 30"
                width="24"
                height="16"
                rx="8"
                fill="#f59e0b"
              />
              <text
                :x="node.x + 24"
                :y="node.y - 20"
                text-anchor="middle"
                fill="#0a0e1a"
                font-size="9"
                font-weight="800"
              >
                {{ node.costSkillPoints }}
              </text>
            </g>
          </g>

          <!-- Branch labels -->
          <text x="200" y="30" text-anchor="middle" fill="#f59e0b" font-size="13" font-weight="700" opacity="0.6">语法 GRAMMAR</text>
          <text x="400" y="30" text-anchor="middle" fill="#7c3aed" font-size="13" font-weight="700" opacity="0.6">词汇 VOCAB</text>
          <text x="600" y="30" text-anchor="middle" fill="#06b6d4" font-size="13" font-weight="700" opacity="0.6">听力 LISTENING</text>
        </svg>
      </div>

      <!-- Node detail popup -->
      <transition name="page-fade">
        <div v-if="selectedNode" class="node-detail dark-panel">
          <div class="detail-header">
            <span class="detail-icon">{{ nodeIcon(selectedNode) }}</span>
            <div>
              <h3>{{ selectedNode.nameZh }}</h3>
              <span class="detail-branch" :style="{ color: branchColor(selectedNode.branch) }">
                {{ selectedNode.branch }}
              </span>
            </div>
          </div>
          <p class="detail-desc">{{ selectedNode.descriptionZh }}</p>
          <div class="detail-meta">
            <span>需要技能点: {{ selectedNode.costSkillPoints }}</span>
            <span v-if="selectedNode.unlocked" class="text-gold">✅ 已解锁</span>
          </div>
          <button class="btn-ghost" @click="selectedNode = null">关闭</button>
        </div>
      </transition>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'
import { useSound } from '@/composables/useSound'
import { mockSkillNodes } from '@/mock/data'
import type { SkillNode } from '@/types'

const userStore = useUserStore()
const sound = useSound()

const skillNodes = reactive([...mockSkillNodes])
const selectedNode = ref<SkillNode | null>(null)

// Define connections between nodes (using actual mock data codes)
const connections = [
  { from: 'SK_GRAMMAR_I_AM', to: 'SK_GRAMMAR_MY_NAME' },
  { from: 'SK_GRAMMAR_I_AM', to: 'SK_GRAMMAR_I_HAVE' },
  { from: 'SK_GRAMMAR_I_HAVE', to: 'SK_GRAMMAR_THIS_THAT' },
  { from: 'SK_VOCAB_GREETINGS', to: 'SK_VOCAB_NUMBERS' },
  { from: 'SK_VOCAB_GREETINGS', to: 'SK_VOCAB_OBJECTS' },
]

// Background random stars
const bgStars = Array.from({ length: 60 }, (_, i) => ({
  id: i,
  x: Math.random() * 800,
  y: Math.random() * 500,
  r: 0.5 + Math.random() * 1.5,
  opacity: 0.1 + Math.random() * 0.4,
  delay: Math.random() * 4,
}))

function getNodePos(nodeId: string) {
  const n = skillNodes.find(s => s.code === nodeId)
  return n ? { x: n.x, y: n.y } : { x: 0, y: 0 }
}

function branchColor(branch: string) {
  const map: Record<string, string> = {
    GRAMMAR: '#f59e0b',
    VOCAB: '#7c3aed',
    LISTENING: '#06b6d4',
  }
  return map[branch] || '#6b7280'
}

function branchGlow(branch: string) {
  const c = branchColor(branch)
  return c.replace('#', 'rgba(') ? `${c}15` : 'rgba(255,255,255,0.05)'
}

function nodeFill(node: SkillNode) {
  if (node.unlocked) return branchColor(node.branch) + '30'
  return '#1a1f35'
}

function nodeStroke(node: SkillNode) {
  if (node.unlocked) return branchColor(node.branch)
  if (canUnlock(node)) return branchColor(node.branch) + '80'
  return '#374151'
}

function nodeIcon(node: SkillNode) {
  const icons: Record<string, string> = {
    GRAMMAR: '📖',
    VOCAB: '💎',
    LISTENING: '🎧',
  }
  return icons[node.branch] || '⭐'
}

function connColor(conn: { from: string; to: string }) {
  const fromNode = skillNodes.find(n => n.code === conn.from)
  return fromNode ? branchColor(fromNode.branch) : '#374151'
}

function connDash(conn: { from: string; to: string }) {
  const from = skillNodes.find(n => n.code === conn.from)
  const to = skillNodes.find(n => n.code === conn.to)
  if (from?.unlocked && to?.unlocked) return 'none'
  return '6 4'
}

function connOpacity(conn: { from: string; to: string }) {
  const from = skillNodes.find(n => n.code === conn.from)
  const to = skillNodes.find(n => n.code === conn.to)
  if (from?.unlocked && to?.unlocked) return 0.8
  if (from?.unlocked || to?.unlocked) return 0.4
  return 0.15
}

function canUnlock(node: SkillNode) {
  if (node.unlocked) return false
  if ((userStore.user?.skillPoints ?? 0) < node.costSkillPoints) return false
  // Check if prereq is unlocked
  const prereqs = connections.filter(c => c.to === node.code)
  if (prereqs.length === 0) return true
  return prereqs.some(p => {
    const parent = skillNodes.find(n => n.code === p.from)
    return parent?.unlocked
  })
}

function tryUnlock(node: SkillNode) {
  selectedNode.value = node
  if (!canUnlock(node)) return

  // Unlock
  if (userStore.user) {
    userStore.user.skillPoints -= node.costSkillPoints
    node.unlocked = true
    sound.levelUp()
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.skill-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  text-align: center;
  margin-bottom: 4px;
}

.page-desc {
  text-align: center;
  font-size: 13px;
  color: $text-muted;
  margin-bottom: 20px;
}

.skill-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 16px;
}

.skill-pts {
  font-size: 18px;
  font-weight: 800;
}

// Constellation
.constellation-wrapper {
  padding: 16px;
  overflow: hidden;
}

.constellation-svg {
  width: 100%;
  height: auto;
}

// Background twinkle
.twinkle {
  animation: twinkle 3s ease-in-out infinite alternate;
}

@keyframes twinkle {
  0% { opacity: 0.1; }
  100% { opacity: 0.6; }
}

// Node glow
.node-glow {
  animation: pulse-glow 3s ease-in-out infinite;
}

.pulse-ring {
  animation: pulse-ring 2s ease-in-out infinite;
}

@keyframes pulse-ring {
  0%, 100% { r: 26; opacity: 0.4; }
  50% { r: 30; opacity: 0.8; }
}

.skill-node {
  transition: transform 0.2s;

  &:hover circle:not(.twinkle) {
    filter: brightness(1.2);
  }
}

// Node detail
.node-detail {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 20px 24px;
  min-width: 300px;
  z-index: 50;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.detail-icon { font-size: 28px; }

.detail-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.detail-branch {
  font-size: 11px;
  font-weight: 600;
}

.detail-desc {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 10px;
  line-height: 1.5;
}

.detail-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: $text-muted;
  margin-bottom: 12px;
}
</style>
