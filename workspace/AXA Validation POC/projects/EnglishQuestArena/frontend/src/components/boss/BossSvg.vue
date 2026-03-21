<template>
  <div class="boss-svg" :class="[`boss-tier-${tier}`, stateClass]">
    <svg viewBox="0 0 200 260" class="boss-figure">
      <!-- Aura glow -->
      <defs>
        <radialGradient :id="'aura-' + tier" cx="50%" cy="60%" r="50%">
          <stop offset="0%" :stop-color="auraColor" stop-opacity="0.3" />
          <stop offset="100%" :stop-color="auraColor" stop-opacity="0" />
        </radialGradient>
        <filter :id="'glow-' + tier">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <!-- Aura circle -->
      <circle cx="100" cy="150" r="90" :fill="`url(#aura-${tier})`" class="aura-pulse" />

      <!-- Body -->
      <g class="boss-body">
        <!-- Cloak / body shape -->
        <path :d="bodyPath" :fill="bodyColor" :stroke="accentColor" stroke-width="1.5" />

        <!-- Head -->
        <circle cx="100" cy="80" r="28" :fill="headColor" :stroke="accentColor" stroke-width="1.5" />

        <!-- Eyes -->
        <g class="boss-eyes">
          <circle cx="90" cy="76" r="4" :fill="eyeColor" :filter="`url(#glow-${tier})`" />
          <circle cx="110" cy="76" r="4" :fill="eyeColor" :filter="`url(#glow-${tier})`" />
          <!-- Pupils -->
          <circle cx="91" cy="76" r="1.5" fill="#000" />
          <circle cx="111" cy="76" r="1.5" fill="#000" />
        </g>

        <!-- Mouth -->
        <path d="M92 90 Q100 97 108 90" fill="none" :stroke="accentColor" stroke-width="1.5" stroke-linecap="round" />

        <!-- Horns (tier >= 3) -->
        <g v-if="tier >= 3" class="boss-horns">
          <path d="M75 62 L65 35 L82 55" :fill="hornColor" :stroke="accentColor" stroke-width="1" />
          <path d="M125 62 L135 35 L118 55" :fill="hornColor" :stroke="accentColor" stroke-width="1" />
        </g>

        <!-- Crown (tier >= 2) -->
        <g v-if="tier >= 2 && tier < 5" class="boss-crown">
          <polygon points="82,55 88,42 94,55 100,38 106,55 112,42 118,55" :fill="crownColor" :stroke="accentColor" stroke-width="0.8" />
        </g>

        <!-- Abyss Lord halo (tier 5) -->
        <g v-if="tier >= 5" class="boss-halo">
          <ellipse cx="100" cy="50" rx="45" ry="12" fill="none" :stroke="accentColor" stroke-width="1.5" opacity="0.6" />
          <ellipse cx="100" cy="50" rx="45" ry="12" fill="none" :stroke="eyeColor" stroke-width="0.5" stroke-dasharray="4 4" class="halo-spin" />
        </g>

        <!-- Weapon (tier >= 2) -->
        <g v-if="tier >= 2" class="boss-weapon">
          <!-- Sword/staff -->
          <line x1="145" y1="100" x2="170" y2="180" :stroke="weaponColor" stroke-width="3" stroke-linecap="round" />
          <circle cx="170" cy="182" r="5" :fill="eyeColor" opacity="0.7" :filter="`url(#glow-${tier})`" />
        </g>

        <!-- Shoulder armor (tier >= 4) -->
        <g v-if="tier >= 4">
          <ellipse cx="68" cy="108" rx="15" ry="10" :fill="bodyColor" :stroke="accentColor" stroke-width="1" />
          <ellipse cx="132" cy="108" rx="15" ry="10" :fill="bodyColor" :stroke="accentColor" stroke-width="1" />
        </g>
      </g>

      <!-- HP bar -->
      <g class="boss-hp-group">
        <rect x="30" y="230" width="140" height="10" rx="5" fill="#1a1f35" stroke="#333" stroke-width="0.5" />
        <rect x="30" y="230" :width="140 * hpPercent" height="10" rx="5" :fill="hpFillColor" class="hp-animate" />
        <text x="100" y="238" text-anchor="middle" fill="#fff" font-size="7" font-weight="700">
          {{ currentHp }} / {{ maxHp }}
        </text>
      </g>
    </svg>

    <!-- Boss name plate -->
    <div class="boss-nameplate">
      <span class="boss-stars">{{ '★'.repeat(tier) }}</span>
      <span class="boss-name">{{ name }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  tier: number
  name: string
  currentHp: number
  maxHp: number
  state?: 'idle' | 'attack' | 'hurt' | 'defeat'
}>(), { state: 'idle' })

const hpPercent = computed(() => Math.max(0, props.currentHp / props.maxHp))
const stateClass = computed(() => `boss-${props.state}`)

// Tier-based color palettes
const tierColors: Record<number, { body: string; head: string; accent: string; eye: string; aura: string; horn: string; crown: string; weapon: string }> = {
  1: { body: '#2d3748', head: '#4a5568', accent: '#718096', eye: '#f59e0b', aura: '#f59e0b', horn: '#4a5568', crown: '#f59e0b', weapon: '#718096' },
  2: { body: '#1a202c', head: '#2d3748', accent: '#4a5568', eye: '#dc2626', aura: '#dc2626', horn: '#2d3748', crown: '#dc2626', weapon: '#4a5568' },
  3: { body: '#451a03', head: '#7c2d12', accent: '#f97316', eye: '#f97316', aura: '#f97316', horn: '#9a3412', crown: '#f59e0b', weapon: '#f97316' },
  4: { body: '#0c4a6e', head: '#164e63', accent: '#06b6d4', eye: '#06b6d4', aura: '#06b6d4', horn: '#155e75', crown: '#22d3ee', weapon: '#06b6d4' },
  5: { body: '#1e1b4b', head: '#312e81', accent: '#7c3aed', eye: '#a78bfa', aura: '#7c3aed', horn: '#4c1d95', crown: '#a78bfa', weapon: '#7c3aed' },
}

const colors = computed(() => tierColors[props.tier] || tierColors[1])

const bodyColor = computed(() => colors.value.body)
const headColor = computed(() => colors.value.head)
const accentColor = computed(() => colors.value.accent)
const eyeColor = computed(() => colors.value.eye)
const auraColor = computed(() => colors.value.aura)
const hornColor = computed(() => colors.value.horn)
const crownColor = computed(() => colors.value.crown)
const weaponColor = computed(() => colors.value.weapon)

const bodyPath = computed(() => 'M70 100 Q60 130 65 200 L80 210 L100 205 L120 210 L135 200 Q140 130 130 100 Q115 95 100 98 Q85 95 70 100')

const hpFillColor = computed(() => {
  if (hpPercent.value > 0.5) return '#10b981'
  if (hpPercent.value > 0.25) return '#f59e0b'
  return '#dc2626'
})
</script>

<style scoped lang="scss">
.boss-svg {
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;
}

.boss-figure {
  width: 200px;
  height: 260px;
}

// Animations
.aura-pulse {
  animation: boss-aura 3s ease-in-out infinite;
}

@keyframes boss-aura {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.boss-eyes circle:nth-child(1),
.boss-eyes circle:nth-child(2) {
  animation: eye-glow 2s ease-in-out infinite alternate;
}

@keyframes eye-glow {
  0% { r: 3.5; }
  100% { r: 4.5; }
}

.boss-body {
  transition: transform 0.3s;
}

.boss-idle .boss-body {
  animation: boss-idle-float 3s ease-in-out infinite;
}

@keyframes boss-idle-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.boss-attack .boss-body {
  animation: boss-attack 0.4s ease-out;
}

@keyframes boss-attack {
  0% { transform: translateX(0); }
  30% { transform: translateX(-15px); }
  70% { transform: translateX(20px); }
  100% { transform: translateX(0); }
}

.boss-hurt .boss-body {
  animation: boss-hurt 0.3s;
}

@keyframes boss-hurt {
  0%, 100% { opacity: 1; }
  25% { opacity: 0.3; }
  50% { opacity: 1; }
  75% { opacity: 0.3; }
}

.boss-defeat .boss-body {
  animation: boss-defeat 1s ease-in forwards;
}

@keyframes boss-defeat {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
  100% { transform: scale(0.3) translateY(40px); opacity: 0; }
}

.halo-spin {
  animation: spin-slow 8s linear infinite;
  transform-origin: 100px 50px;
}

.hp-animate {
  transition: width 0.5s ease-out;
}

// Name plate
.boss-nameplate {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  margin-top: 8px;
}

.boss-stars {
  font-size: 14px;
  letter-spacing: 2px;
}

.boss-tier-1 .boss-stars { color: #f59e0b; }
.boss-tier-2 .boss-stars { color: #dc2626; }
.boss-tier-3 .boss-stars { color: #f97316; }
.boss-tier-4 .boss-stars { color: #06b6d4; }
.boss-tier-5 .boss-stars { color: #a78bfa; }

.boss-name {
  font-size: 14px;
  font-weight: 700;
  color: #e5e7eb;
  letter-spacing: 1px;
}

@keyframes spin-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
