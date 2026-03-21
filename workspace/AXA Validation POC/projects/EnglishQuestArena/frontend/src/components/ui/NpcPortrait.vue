<template>
  <canvas ref="canvas" :width="W" :height="H" class="npc-portrait-canvas"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const props = defineProps<{
  type: 'sage' | 'guide' | 'knight' | 'merchant'
  animate?: boolean
}>()

const W = 280
const H = 440
const canvas = ref<HTMLCanvasElement>()

function draw() {
  const el = canvas.value
  if (!el) return
  const ctx = el.getContext('2d')
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)

  switch (props.type) {
    case 'sage': drawSage(ctx); break
    case 'guide': drawGuide(ctx); break
    case 'knight': drawKnight(ctx); break
    case 'merchant': drawMerchant(ctx); break
  }
}

/* ═══════════════════════════════════════
   共用：大眼动漫脸
   ═══════════════════════════════════════ */
function drawAnimeFace(
  ctx: CanvasRenderingContext2D,
  cx: number, cy: number,       // 脸部中心
  skinColor: string,
  eyeColor: string,
  mouth: 'smile' | 'grin' | 'serious' = 'smile',
) {
  const skin = skinColor
  const skinLight = lighten(skinColor, 30)
  const skinShadow = darken(skinColor, 25)

  // 脸型 — 倒蛋型
  ctx.fillStyle = skin
  ctx.beginPath()
  ctx.ellipse(cx, cy - 4, 38, 44, 0, 0, Math.PI * 2)
  ctx.fill()
  // 下巴 V 收
  ctx.beginPath()
  ctx.moveTo(cx - 28, cy + 10)
  ctx.quadraticCurveTo(cx, cy + 50, cx + 28, cy + 10)
  ctx.fill()

  // 脸颊高光
  ctx.fillStyle = skinLight
  ctx.beginPath()
  ctx.ellipse(cx - 12, cy - 14, 18, 22, -0.1, 0, Math.PI * 2)
  ctx.fill()

  // 腮红
  ctx.fillStyle = 'rgba(255, 140, 140, 0.18)'
  ctx.beginPath()
  ctx.ellipse(cx - 26, cy + 12, 10, 6, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(cx + 26, cy + 12, 10, 6, 0, 0, Math.PI * 2)
  ctx.fill()

  // 大眼睛
  drawAnimeEye(ctx, cx - 16, cy - 2, eyeColor, false)
  drawAnimeEye(ctx, cx + 16, cy - 2, eyeColor, true)

  // 鼻子 — 简笔
  ctx.strokeStyle = skinShadow
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(cx, cy + 10)
  ctx.lineTo(cx - 2, cy + 16)
  ctx.stroke()

  // 嘴巴
  if (mouth === 'smile') {
    ctx.strokeStyle = darken(skinColor, 40)
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.arc(cx, cy + 24, 8, 0.15, Math.PI - 0.15)
    ctx.stroke()
  } else if (mouth === 'grin') {
    ctx.fillStyle = darken(skinColor, 50)
    ctx.beginPath()
    ctx.arc(cx, cy + 24, 9, 0, Math.PI)
    ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(cx, cy + 24, 9, 0.1, Math.PI - 0.1)
    ctx.fill()
    ctx.fillStyle = darken(skinColor, 50)
    ctx.beginPath()
    ctx.arc(cx, cy + 24, 9, 0, Math.PI)
    ctx.fill()
    // 上齿
    ctx.fillStyle = '#fff'
    ctx.fillRect(cx - 7, cy + 24, 14, 4)
  } else {
    ctx.strokeStyle = darken(skinColor, 40)
    ctx.lineWidth = 2.5
    ctx.beginPath()
    ctx.moveTo(cx - 7, cy + 26)
    ctx.lineTo(cx + 7, cy + 26)
    ctx.stroke()
  }
}

function drawAnimeEye(ctx: CanvasRenderingContext2D, cx: number, cy: number, color: string, isRight: boolean) {
  const dir = isRight ? 1 : -1
  // 眼白
  ctx.fillStyle = '#fff'
  ctx.beginPath()
  ctx.ellipse(cx, cy, 11, 13, 0, 0, Math.PI * 2)
  ctx.fill()

  // 虹膜
  const irisGrad = ctx.createRadialGradient(cx + dir, cy - 1, 2, cx + dir, cy, 9)
  irisGrad.addColorStop(0, lighten(color, 40))
  irisGrad.addColorStop(0.4, color)
  irisGrad.addColorStop(1, darken(color, 30))
  ctx.fillStyle = irisGrad
  ctx.beginPath()
  ctx.ellipse(cx + dir, cy, 8, 10, 0, 0, Math.PI * 2)
  ctx.fill()

  // 瞳孔
  ctx.fillStyle = '#111'
  ctx.beginPath()
  ctx.ellipse(cx + dir, cy + 1, 4, 5, 0, 0, Math.PI * 2)
  ctx.fill()

  // 大高光
  ctx.fillStyle = 'rgba(255,255,255,0.9)'
  ctx.beginPath()
  ctx.ellipse(cx - 2 * dir, cy - 4, 3.5, 4, -0.3, 0, Math.PI * 2)
  ctx.fill()
  // 小高光
  ctx.fillStyle = 'rgba(255,255,255,0.7)'
  ctx.beginPath()
  ctx.arc(cx + 3 * dir, cy + 3, 2, 0, Math.PI * 2)
  ctx.fill()

  // 上眼线
  ctx.strokeStyle = '#222'
  ctx.lineWidth = 2.5
  ctx.beginPath()
  ctx.ellipse(cx, cy, 11, 13, 0, Math.PI + 0.3, -0.3)
  ctx.stroke()

  // 睫毛
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(cx - 10 * (isRight ? -1 : 1), cy - 6)
  ctx.lineTo(cx - 13 * (isRight ? -1 : 1), cy - 10)
  ctx.stroke()

  // 眉毛
  ctx.strokeStyle = '#333'
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.moveTo(cx - 10, cy - 18)
  ctx.quadraticCurveTo(cx, cy - 22, cx + 10, cy - 18)
  ctx.stroke()
}

/* ═══════════════════════════════════════
   贤者 — 紫袍魔法师
   ═══════════════════════════════════════ */
function drawSage(ctx: CanvasRenderingContext2D) {
  const CX = 140, FACE_Y = 145

  // 魔法光圈背景
  drawAura(ctx, CX, 260, 140, 'rgba(130, 90, 240, 0.12)', 'rgba(100, 60, 220, 0.04)')

  // 阴影
  drawShadow(ctx, CX, 425, 60)

  // ── 长袍 ──
  const robeG = ctx.createLinearGradient(CX, 180, CX, 430)
  robeG.addColorStop(0, '#8866dd')
  robeG.addColorStop(0.4, '#6644bb')
  robeG.addColorStop(1, '#3a2277')
  ctx.fillStyle = robeG
  ctx.beginPath()
  ctx.moveTo(CX - 50, 200)
  ctx.quadraticCurveTo(CX - 70, 320, CX - 75, 430)
  ctx.lineTo(CX + 75, 430)
  ctx.quadraticCurveTo(CX + 70, 320, CX + 50, 200)
  ctx.closePath()
  ctx.fill()

  // 袍子高光
  ctx.fillStyle = 'rgba(200, 170, 255, 0.08)'
  ctx.beginPath()
  ctx.moveTo(CX - 30, 200)
  ctx.quadraticCurveTo(CX - 45, 310, CX - 50, 430)
  ctx.lineTo(CX - 25, 430)
  ctx.quadraticCurveTo(CX - 30, 310, CX - 18, 200)
  ctx.closePath()
  ctx.fill()

  // 袍子褶皱暗线
  ctx.strokeStyle = 'rgba(30, 10, 60, 0.15)'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(CX + 10, 220)
  ctx.quadraticCurveTo(CX + 20, 320, CX + 25, 430)
  ctx.stroke()
  ctx.beginPath()
  ctx.moveTo(CX + 30, 210)
  ctx.quadraticCurveTo(CX + 45, 330, CX + 55, 430)
  ctx.stroke()

  // ── 领口V 字 ──
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.moveTo(CX - 20, 190)
  ctx.lineTo(CX, 225)
  ctx.lineTo(CX + 20, 190)
  ctx.closePath()
  ctx.fill()

  // ── 金腰带 ──
  ctx.fillStyle = '#daa520'
  ctx.beginPath()
  ctx.moveTo(CX - 55, 265)
  ctx.quadraticCurveTo(CX, 278, CX + 55, 265)
  ctx.quadraticCurveTo(CX, 282, CX - 55, 270)
  ctx.closePath()
  ctx.fill()
  // 腰带扣
  drawGem(ctx, CX, 272, 8, '#ffd700', '#b8860b')

  // ── 胡须 ──
  ctx.fillStyle = '#ddd'
  ctx.beginPath()
  ctx.moveTo(CX - 16, 175)
  ctx.quadraticCurveTo(CX - 5, 255, CX, 260)
  ctx.quadraticCurveTo(CX + 5, 255, CX + 16, 175)
  ctx.closePath()
  ctx.fill()
  // 胡须高光
  ctx.fillStyle = 'rgba(255,255,255,0.25)'
  ctx.beginPath()
  ctx.moveTo(CX - 6, 180)
  ctx.quadraticCurveTo(CX, 240, CX, 248)
  ctx.quadraticCurveTo(CX + 2, 235, CX + 4, 178)
  ctx.closePath()
  ctx.fill()

  // ── 脸 ──
  drawAnimeFace(ctx, CX, FACE_Y, '#ffcc88', '#6633cc', 'smile')

  // ── 帽子 ──
  const hatG = ctx.createLinearGradient(CX, 30, CX, 120)
  hatG.addColorStop(0, '#7755cc')
  hatG.addColorStop(1, '#4422aa')
  ctx.fillStyle = hatG
  ctx.beginPath()
  ctx.moveTo(CX + 5, 20)
  ctx.quadraticCurveTo(CX - 55, 70, CX - 55, 120)
  ctx.quadraticCurveTo(CX, 110, CX + 58, 120)
  ctx.quadraticCurveTo(CX + 55, 70, CX + 5, 20)
  ctx.fill()
  // 帽檐
  ctx.fillStyle = '#553399'
  ctx.beginPath()
  ctx.ellipse(CX, 118, 62, 14, 0, 0, Math.PI * 2)
  ctx.fill()
  // 帽子星星装饰
  drawStarShape(ctx, CX + 10, 60, 8, '#ffd700', 0.8)
  drawStarShape(ctx, CX - 8, 80, 5, '#ffc855', 0.6)
  drawStarShape(ctx, CX + 20, 85, 4, '#ffe088', 0.4)

  // ── 头发（从帽子下露出）──
  ctx.fillStyle = '#888'
  ctx.beginPath()
  ctx.moveTo(CX - 42, 125)
  ctx.quadraticCurveTo(CX - 48, 155, CX - 45, 170)
  ctx.lineTo(CX - 38, 170)
  ctx.quadraticCurveTo(CX - 42, 150, CX - 38, 128)
  ctx.closePath()
  ctx.fill()
  ctx.beginPath()
  ctx.moveTo(CX + 42, 125)
  ctx.quadraticCurveTo(CX + 48, 155, CX + 45, 170)
  ctx.lineTo(CX + 38, 170)
  ctx.quadraticCurveTo(CX + 42, 150, CX + 38, 128)
  ctx.closePath()
  ctx.fill()

  // ── 法杖（右手）──
  ctx.strokeStyle = '#6B3A10'
  ctx.lineWidth = 7
  ctx.beginPath()
  ctx.moveTo(CX + 75, 190)
  ctx.quadraticCurveTo(CX + 80, 310, CX + 85, 430)
  ctx.stroke()
  // 木纹
  ctx.strokeStyle = 'rgba(255,255,255,0.1)'
  ctx.lineWidth = 1
  for (let t = 0; t < 5; t++) {
    const y = 210 + t * 45
    ctx.beginPath()
    ctx.moveTo(CX + 73, y)
    ctx.quadraticCurveTo(CX + 78, y + 5, CX + 73, y + 10)
    ctx.stroke()
  }

  // 法杖顶端宝珠
  const orbG = ctx.createRadialGradient(CX + 74, 178, 3, CX + 74, 180, 20)
  orbG.addColorStop(0, '#fff')
  orbG.addColorStop(0.2, '#cc99ff')
  orbG.addColorStop(0.5, '#8855dd')
  orbG.addColorStop(1, '#4422aa')
  ctx.fillStyle = orbG
  ctx.beginPath()
  ctx.arc(CX + 74, 180, 18, 0, Math.PI * 2)
  ctx.fill()
  // 宝珠外光晕
  ctx.fillStyle = 'rgba(170, 130, 255, 0.25)'
  ctx.beginPath()
  ctx.arc(CX + 74, 180, 28, 0, Math.PI * 2)
  ctx.fill()
  // 宝珠十字高光
  ctx.strokeStyle = 'rgba(255,255,255,0.5)'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(CX + 74, 165)
  ctx.lineTo(CX + 74, 195)
  ctx.moveTo(CX + 59, 180)
  ctx.lineTo(CX + 89, 180)
  ctx.stroke()

  // 魔法粒子
  drawFloatingParticles(ctx, CX + 74, 180, 35, 'rgba(200,160,255,0.6)', 6)
}

/* ═══════════════════════════════════════
   向导 — 绿衣弓手
   ═══════════════════════════════════════ */
function drawGuide(ctx: CanvasRenderingContext2D) {
  const CX = 140, FACE_Y = 148

  drawAura(ctx, CX, 260, 130, 'rgba(60, 200, 90, 0.1)', 'rgba(40, 160, 60, 0.03)')
  drawShadow(ctx, CX, 425, 55)

  // ── 斗篷 ──
  const cloakG = ctx.createLinearGradient(CX, 160, CX, 430)
  cloakG.addColorStop(0, '#357a48')
  cloakG.addColorStop(0.5, '#2a6038')
  cloakG.addColorStop(1, '#1a4028')
  ctx.fillStyle = cloakG
  ctx.beginPath()
  ctx.moveTo(CX - 45, 190)
  ctx.quadraticCurveTo(CX - 75, 310, CX - 65, 420)
  ctx.lineTo(CX + 65, 420)
  ctx.quadraticCurveTo(CX + 75, 310, CX + 45, 190)
  ctx.closePath()
  ctx.fill()

  // 斗篷高光
  ctx.fillStyle = 'rgba(100, 220, 120, 0.06)'
  ctx.beginPath()
  ctx.moveTo(CX - 25, 200)
  ctx.quadraticCurveTo(CX - 50, 310, CX - 45, 420)
  ctx.lineTo(CX - 20, 420)
  ctx.quadraticCurveTo(CX - 25, 310, CX - 10, 200)
  ctx.closePath()
  ctx.fill()

  // ── 皮甲内衬 ──
  ctx.fillStyle = '#9b7520'
  ctx.beginPath()
  ctx.moveTo(CX - 30, 200)
  ctx.lineTo(CX - 35, 340)
  ctx.lineTo(CX + 35, 340)
  ctx.lineTo(CX + 30, 200)
  ctx.closePath()
  ctx.fill()
  // 皮甲纹路
  ctx.strokeStyle = 'rgba(0,0,0,0.12)'
  ctx.lineWidth = 1
  for (let y = 220; y < 330; y += 24) {
    ctx.beginPath()
    ctx.moveTo(CX - 30, y)
    ctx.lineTo(CX + 30, y)
    ctx.stroke()
  }

  // 领口
  ctx.fillStyle = '#f5c78a'
  ctx.beginPath()
  ctx.moveTo(CX - 22, 188)
  ctx.lineTo(CX, 215)
  ctx.lineTo(CX + 22, 188)
  ctx.closePath()
  ctx.fill()

  // ── 腰带 ──
  ctx.fillStyle = '#5a3a1a'
  ctx.fillRect(CX - 40, 275, 80, 10)
  drawGem(ctx, CX, 280, 6, '#daa520', '#8b6914')

  // ── 脸 ──
  drawAnimeFace(ctx, CX, FACE_Y, '#f5c78a', '#2a7530', 'grin')

  // ── 头发 ──
  ctx.fillStyle = '#774422'
  ctx.beginPath()
  ctx.ellipse(CX, 118, 42, 28, 0, Math.PI, Math.PI * 2)
  ctx.fill()
  // 刘海
  ctx.beginPath()
  ctx.moveTo(CX - 38, 130)
  ctx.quadraticCurveTo(CX - 28, 108, CX - 10, 112)
  ctx.lineTo(CX - 25, 140)
  ctx.closePath()
  ctx.fill()
  // 后发长
  ctx.beginPath()
  ctx.moveTo(CX + 36, 128)
  ctx.quadraticCurveTo(CX + 48, 160, CX + 42, 190)
  ctx.lineTo(CX + 35, 185)
  ctx.quadraticCurveTo(CX + 40, 155, CX + 32, 130)
  ctx.closePath()
  ctx.fill()

  // ── 帽子 ──
  ctx.fillStyle = '#2d7a40'
  ctx.beginPath()
  ctx.moveTo(CX, 78)
  ctx.quadraticCurveTo(CX - 50, 100, CX - 48, 125)
  ctx.quadraticCurveTo(CX, 115, CX + 48, 125)
  ctx.quadraticCurveTo(CX + 50, 100, CX, 78)
  ctx.fill()
  // 帽子暗面
  ctx.fillStyle = 'rgba(0,0,0,0.1)'
  ctx.beginPath()
  ctx.moveTo(CX + 10, 82)
  ctx.quadraticCurveTo(CX + 50, 100, CX + 48, 125)
  ctx.quadraticCurveTo(CX + 30, 118, CX + 15, 90)
  ctx.closePath()
  ctx.fill()

  // 大红羽毛
  ctx.save()
  ctx.translate(CX + 40, 105)
  ctx.rotate(-0.3)
  const featherG = ctx.createLinearGradient(0, 0, 0, -70)
  featherG.addColorStop(0, '#cc3300')
  featherG.addColorStop(1, '#ff6633')
  ctx.fillStyle = featherG
  ctx.beginPath()
  ctx.moveTo(0, 0)
  ctx.quadraticCurveTo(-12, -35, -5, -65)
  ctx.quadraticCurveTo(3, -40, 5, -65)
  ctx.quadraticCurveTo(12, -35, 0, 0)
  ctx.fill()
  // 羽轴
  ctx.strokeStyle = 'rgba(0,0,0,0.2)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(0, 0)
  ctx.lineTo(0, -60)
  ctx.stroke()
  ctx.restore()

  // ── 弓（左肩）──
  ctx.strokeStyle = '#7a4510'
  ctx.lineWidth = 5
  ctx.beginPath()
  ctx.moveTo(CX - 65, 175)
  ctx.quadraticCurveTo(CX - 85, 280, CX - 65, 380)
  ctx.stroke()
  // 弓弦
  ctx.strokeStyle = 'rgba(200,200,200,0.6)'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(CX - 65, 175)
  ctx.lineTo(CX - 65, 380)
  ctx.stroke()

  // ── 右手指引 ──
  ctx.fillStyle = '#f5c78a'
  ctx.beginPath()
  ctx.ellipse(CX + 68, 260, 10, 12, 0.2, 0, Math.PI * 2)
  ctx.fill()
  // 食指
  ctx.save()
  ctx.translate(CX + 80, 252)
  ctx.rotate(-0.6)
  ctx.fillRect(-4, -18, 8, 18)
  ctx.beginPath()
  ctx.arc(0, -18, 4, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()
}

/* ═══════════════════════════════════════
   骑士 — 铁甲红披
   ═══════════════════════════════════════ */
function drawKnight(ctx: CanvasRenderingContext2D) {
  const CX = 140, FACE_Y = 145

  drawAura(ctx, CX, 260, 140, 'rgba(220, 80, 40, 0.1)', 'rgba(180, 40, 20, 0.03)')
  drawShadow(ctx, CX, 425, 60)

  // ── 红披风 ──
  ctx.fillStyle = '#8b2222'
  ctx.beginPath()
  ctx.moveTo(CX - 50, 195)
  ctx.quadraticCurveTo(CX - 85, 310, CX - 78, 430)
  ctx.lineTo(CX + 78, 430)
  ctx.quadraticCurveTo(CX + 85, 310, CX + 50, 195)
  ctx.closePath()
  ctx.fill()
  // 披风高光
  ctx.fillStyle = 'rgba(200, 60, 40, 0.12)'
  ctx.beginPath()
  ctx.moveTo(CX - 30, 200)
  ctx.quadraticCurveTo(CX - 60, 310, CX - 55, 430)
  ctx.lineTo(CX - 30, 430)
  ctx.quadraticCurveTo(CX - 35, 310, CX - 15, 200)
  ctx.closePath()
  ctx.fill()
  // 褶皱暗线
  ctx.strokeStyle = 'rgba(60, 0, 0, 0.15)'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(CX + 20, 210)
  ctx.quadraticCurveTo(CX + 40, 340, CX + 50, 430)
  ctx.stroke()

  // ── 铁甲 ──
  const armorG = ctx.createLinearGradient(CX - 50, 195, CX + 50, 195)
  armorG.addColorStop(0, '#888')
  armorG.addColorStop(0.25, '#bbb')
  armorG.addColorStop(0.45, '#ddd')
  armorG.addColorStop(0.55, '#ccc')
  armorG.addColorStop(0.75, '#aaa')
  armorG.addColorStop(1, '#777')
  ctx.fillStyle = armorG
  ctx.beginPath()
  ctx.moveTo(CX - 40, 195)
  ctx.lineTo(CX - 45, 340)
  ctx.lineTo(CX + 45, 340)
  ctx.lineTo(CX + 40, 195)
  ctx.closePath()
  ctx.fill()

  // 盔甲纹饰
  ctx.strokeStyle = 'rgba(0,0,0,0.12)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(CX, 200)
  ctx.lineTo(CX, 340)
  ctx.stroke()
  // 胸甲十字纹
  ctx.strokeStyle = 'rgba(218,165,32,0.3)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(CX - 15, 230)
  ctx.lineTo(CX + 15, 230)
  ctx.moveTo(CX, 215)
  ctx.lineTo(CX, 250)
  ctx.stroke()

  // 肩甲
  const shoulderG = ctx.createLinearGradient(0, 0, 30, 0)
  shoulderG.addColorStop(0, '#999')
  shoulderG.addColorStop(0.5, '#ccc')
  shoulderG.addColorStop(1, '#888')
  ctx.fillStyle = shoulderG
  ctx.beginPath()
  ctx.ellipse(CX - 48, 198, 22, 14, 0.3, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(CX + 48, 198, 22, 14, -0.3, 0, Math.PI * 2)
  ctx.fill()
  // 肩甲铆钉
  ctx.fillStyle = '#daa520'
  ctx.beginPath()
  ctx.arc(CX - 48, 198, 3, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(CX + 48, 198, 3, 0, Math.PI * 2)
  ctx.fill()

  // 领口
  ctx.fillStyle = '#eebb88'
  ctx.beginPath()
  ctx.moveTo(CX - 22, 188)
  ctx.lineTo(CX, 208)
  ctx.lineTo(CX + 22, 188)
  ctx.closePath()
  ctx.fill()

  // ── 腰带 ──
  ctx.fillStyle = '#8b6914'
  ctx.fillRect(CX - 48, 295, 96, 12)
  drawGem(ctx, CX, 301, 8, '#ffd700', '#b8860b')

  // ── 脸 ──
  drawAnimeFace(ctx, CX, FACE_Y, '#eebb88', '#884422', 'serious')

  // ── 头发 ──
  ctx.fillStyle = '#2a2a2a'
  ctx.beginPath()
  ctx.ellipse(CX, 112, 42, 30, 0, Math.PI, Math.PI * 2)
  ctx.fill()
  // 鬓角
  ctx.beginPath()
  ctx.moveTo(CX - 38, 118)
  ctx.quadraticCurveTo(CX - 45, 145, CX - 40, 165)
  ctx.lineTo(CX - 34, 160)
  ctx.quadraticCurveTo(CX - 38, 140, CX - 34, 120)
  ctx.closePath()
  ctx.fill()
  ctx.beginPath()
  ctx.moveTo(CX + 38, 118)
  ctx.quadraticCurveTo(CX + 45, 145, CX + 40, 165)
  ctx.lineTo(CX + 34, 160)
  ctx.quadraticCurveTo(CX + 38, 140, CX + 34, 120)
  ctx.closePath()
  ctx.fill()

  // ── 疤痕 ──
  ctx.strokeStyle = 'rgba(200, 80, 60, 0.55)'
  ctx.lineWidth = 2.5
  ctx.beginPath()
  ctx.moveTo(CX + 22, 130)
  ctx.lineTo(CX + 28, 158)
  ctx.stroke()

  // ── 大剑（右手）──
  // 剑刃
  ctx.fillStyle = '#bbb'
  ctx.beginPath()
  ctx.moveTo(CX + 78, 90)
  ctx.lineTo(CX + 82, 90)
  ctx.lineTo(CX + 88, 400)
  ctx.lineTo(CX + 72, 400)
  ctx.closePath()
  ctx.fill()
  // 剑刃高光
  ctx.fillStyle = 'rgba(255,255,255,0.3)'
  ctx.beginPath()
  ctx.moveTo(CX + 78, 95)
  ctx.lineTo(CX + 80, 95)
  ctx.lineTo(CX + 82, 390)
  ctx.lineTo(CX + 76, 390)
  ctx.closePath()
  ctx.fill()
  // 剑柄
  ctx.fillStyle = '#5a3a1a'
  ctx.fillRect(CX + 68, 82, 24, 14)
  // 护手
  ctx.fillStyle = '#daa520'
  ctx.fillRect(CX + 64, 93, 32, 6)
  // 柄头宝石
  drawGem(ctx, CX + 80, 78, 6, '#cc2222', '#881111')
}

/* ═══════════════════════════════════════
   商人/学者 — 棕衣眼镜
   ═══════════════════════════════════════ */
function drawMerchant(ctx: CanvasRenderingContext2D) {
  const CX = 140, FACE_Y = 148

  drawAura(ctx, CX, 260, 130, 'rgba(210, 170, 80, 0.1)', 'rgba(180, 140, 50, 0.03)')
  drawShadow(ctx, CX, 425, 55)

  // ── 大衣 ──
  const coatG = ctx.createLinearGradient(CX, 190, CX, 430)
  coatG.addColorStop(0, '#9b7520')
  coatG.addColorStop(0.5, '#7a5c18')
  coatG.addColorStop(1, '#544010')
  ctx.fillStyle = coatG
  ctx.beginPath()
  ctx.moveTo(CX - 50, 200)
  ctx.quadraticCurveTo(CX - 68, 310, CX - 62, 430)
  ctx.lineTo(CX + 62, 430)
  ctx.quadraticCurveTo(CX + 68, 310, CX + 50, 200)
  ctx.closePath()
  ctx.fill()

  // 大衣分割线
  ctx.strokeStyle = 'rgba(0,0,0,0.12)'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(CX - 5, 200)
  ctx.lineTo(CX - 8, 430)
  ctx.stroke()
  ctx.beginPath()
  ctx.moveTo(CX + 5, 200)
  ctx.lineTo(CX + 8, 430)
  ctx.stroke()

  // ── 内衬衬衫 ──
  ctx.fillStyle = '#f0e8c8'
  ctx.beginPath()
  ctx.moveTo(CX - 25, 200)
  ctx.lineTo(CX - 28, 340)
  ctx.lineTo(CX + 28, 340)
  ctx.lineTo(CX + 25, 200)
  ctx.closePath()
  ctx.fill()

  // 领口
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.moveTo(CX - 24, 190)
  ctx.lineTo(CX, 218)
  ctx.lineTo(CX + 24, 190)
  ctx.closePath()
  ctx.fill()

  // ── 纽扣 ──
  ctx.fillStyle = '#daa520'
  for (let y = 232; y <= 325; y += 28) {
    ctx.beginPath()
    ctx.arc(CX, y, 4, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = 'rgba(0,0,0,0.15)'
    ctx.beginPath()
    ctx.arc(CX, y, 2, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#daa520'
  }

  // ── 脸 ──
  drawAnimeFace(ctx, CX, FACE_Y, '#ffcc88', '#664422', 'smile')

  // ── 整齐头发 ──
  ctx.fillStyle = '#554430'
  ctx.beginPath()
  ctx.ellipse(CX, 116, 42, 28, 0, Math.PI, Math.PI * 2)
  ctx.fill()
  // 整齐分头
  ctx.fillStyle = '#443320'
  ctx.beginPath()
  ctx.moveTo(CX - 5, 90)
  ctx.quadraticCurveTo(CX - 40, 105, CX - 42, 130)
  ctx.lineTo(CX - 38, 140)
  ctx.quadraticCurveTo(CX - 35, 115, CX, 95)
  ctx.closePath()
  ctx.fill()

  // ── 圆框眼镜 ──
  ctx.strokeStyle = '#b8860b'
  ctx.lineWidth = 2.5
  ctx.beginPath()
  ctx.arc(CX - 16, FACE_Y - 2, 14, 0, Math.PI * 2)
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(CX + 16, FACE_Y - 2, 14, 0, Math.PI * 2)
  ctx.stroke()
  // 镜桥
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(CX + 2, FACE_Y - 2)
  ctx.lineTo(CX - 2, FACE_Y - 2)
  ctx.stroke()
  // 镜臂
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(CX - 30, FACE_Y - 2)
  ctx.lineTo(CX - 38, FACE_Y - 8)
  ctx.stroke()
  ctx.beginPath()
  ctx.moveTo(CX + 30, FACE_Y - 2)
  ctx.lineTo(CX + 38, FACE_Y - 8)
  ctx.stroke()
  // 镜片反光
  ctx.fillStyle = 'rgba(255,255,255,0.12)'
  ctx.beginPath()
  ctx.ellipse(CX - 20, FACE_Y - 6, 5, 8, -0.3, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(CX + 12, FACE_Y - 6, 5, 8, -0.3, 0, Math.PI * 2)
  ctx.fill()

  // ── 大本书（左手）──
  ctx.save()
  ctx.translate(CX - 72, 260)
  ctx.rotate(-0.12)
  // 书脊
  ctx.fillStyle = '#7a1515'
  ctx.fillRect(-2, 0, 6, 55)
  // 封面
  ctx.fillStyle = '#9b2020'
  ctx.fillRect(4, 0, 38, 55)
  // 封面装饰
  ctx.strokeStyle = '#daa520'
  ctx.lineWidth = 1
  ctx.strokeRect(8, 4, 30, 47)
  ctx.strokeRect(12, 8, 22, 39)
  // 书页
  ctx.fillStyle = '#fffae0'
  ctx.fillRect(4, 2, 36, 51)
  // 文字线
  ctx.strokeStyle = 'rgba(0,0,0,0.08)'
  ctx.lineWidth = 0.8
  for (let y = 10; y < 48; y += 6) {
    ctx.beginPath()
    ctx.moveTo(8, y)
    ctx.lineTo(36, y)
    ctx.stroke()
  }
  ctx.restore()

  // ── 羽毛笔（右手）──
  ctx.save()
  ctx.translate(CX + 68, 240)
  ctx.rotate(-0.2)
  // 笔杆
  ctx.strokeStyle = '#b8860b'
  ctx.lineWidth = 2.5
  ctx.beginPath()
  ctx.moveTo(0, 0)
  ctx.lineTo(-5, -60)
  ctx.stroke()
  // 羽毛
  const featherG = ctx.createLinearGradient(-5, -60, -5, -110)
  featherG.addColorStop(0, '#f8f0d8')
  featherG.addColorStop(1, '#fff')
  ctx.fillStyle = featherG
  ctx.beginPath()
  ctx.moveTo(-5, -60)
  ctx.quadraticCurveTo(-22, -75, -15, -100)
  ctx.quadraticCurveTo(-5, -85, 5, -100)
  ctx.quadraticCurveTo(12, -75, -5, -60)
  ctx.fill()
  // 羽轴
  ctx.strokeStyle = 'rgba(0,0,0,0.15)'
  ctx.lineWidth = 0.8
  ctx.beginPath()
  ctx.moveTo(-5, -60)
  ctx.lineTo(-5, -95)
  ctx.stroke()
  ctx.restore()

  // 墨水滴
  ctx.fillStyle = 'rgba(20, 20, 80, 0.4)'
  ctx.beginPath()
  ctx.arc(CX + 70, 245, 2, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(CX + 74, 250, 1.2, 0, Math.PI * 2)
  ctx.fill()
}

/* ═══════════════════════════════════════
   辅助函数
   ═══════════════════════════════════════ */
function drawAura(ctx: CanvasRenderingContext2D, cx: number, cy: number, r: number, inner: string, outer: string) {
  const g = ctx.createRadialGradient(cx, cy, r * 0.15, cx, cy, r)
  g.addColorStop(0, inner)
  g.addColorStop(0.7, outer)
  g.addColorStop(1, 'transparent')
  ctx.fillStyle = g
  ctx.fillRect(0, 40, W, H - 40)
}

function drawShadow(ctx: CanvasRenderingContext2D, cx: number, y: number, rx: number) {
  ctx.fillStyle = 'rgba(0,0,0,0.3)'
  ctx.beginPath()
  ctx.ellipse(cx, y, rx, 12, 0, 0, Math.PI * 2)
  ctx.fill()
}

function drawGem(ctx: CanvasRenderingContext2D, cx: number, cy: number, r: number, color1: string, color2: string) {
  const g = ctx.createRadialGradient(cx - r * 0.3, cy - r * 0.3, r * 0.1, cx, cy, r)
  g.addColorStop(0, lighten(color1, 40))
  g.addColorStop(0.5, color1)
  g.addColorStop(1, color2)
  ctx.fillStyle = g
  ctx.beginPath()
  ctx.arc(cx, cy, r, 0, Math.PI * 2)
  ctx.fill()
  // 高光
  ctx.fillStyle = 'rgba(255,255,255,0.5)'
  ctx.beginPath()
  ctx.arc(cx - r * 0.25, cy - r * 0.25, r * 0.35, 0, Math.PI * 2)
  ctx.fill()
}

function drawStarShape(ctx: CanvasRenderingContext2D, x: number, y: number, r: number, color: string, alpha: number) {
  ctx.save()
  ctx.globalAlpha = alpha
  ctx.fillStyle = color
  ctx.beginPath()
  for (let i = 0; i < 5; i++) {
    const a = (i * 4 * Math.PI) / 5 - Math.PI / 2
    const m = i === 0 ? 'moveTo' : 'lineTo'
    ctx[m](x + r * Math.cos(a), y + r * Math.sin(a))
  }
  ctx.closePath()
  ctx.fill()
  ctx.restore()
}

function drawFloatingParticles(ctx: CanvasRenderingContext2D, cx: number, cy: number, spread: number, color: string, count: number) {
  ctx.fillStyle = color
  // 使用固定种子位置（不随机，防止每次重绘位置变化）
  const positions = [
    { dx: -0.7, dy: -0.8 }, { dx: 0.6, dy: -0.6 },
    { dx: -0.5, dy: 0.5 }, { dx: 0.8, dy: 0.3 },
    { dx: -0.3, dy: -0.4 }, { dx: 0.2, dy: 0.7 },
  ]
  for (let i = 0; i < Math.min(count, positions.length); i++) {
    const p = positions[i]
    const px = cx + p.dx * spread
    const py = cy + p.dy * spread
    const r = 1.5 + (i % 3)
    ctx.beginPath()
    ctx.arc(px, py, r, 0, Math.PI * 2)
    ctx.fill()
  }
}

function lighten(hex: string, amount: number): string {
  const c = hexToRgb(hex)
  return `rgb(${Math.min(255, c.r + amount)},${Math.min(255, c.g + amount)},${Math.min(255, c.b + amount)})`
}

function darken(hex: string, amount: number): string {
  const c = hexToRgb(hex)
  return `rgb(${Math.max(0, c.r - amount)},${Math.max(0, c.g - amount)},${Math.max(0, c.b - amount)})`
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const h = hex.replace('#', '')
  return {
    r: parseInt(h.substring(0, 2), 16),
    g: parseInt(h.substring(2, 4), 16),
    b: parseInt(h.substring(4, 6), 16),
  }
}

onMounted(draw)
watch(() => props.type, draw)
</script>

<style scoped>
.npc-portrait-canvas {
  display: block;
  image-rendering: auto;
}
</style>
